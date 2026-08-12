#!/usr/bin/env python3
"""氢风 打包工具公共模块

供主编排器 build_package.py 及各平台脚本 import：
    build_package_windows.py / build_package_linux.py / build_package_android.py / build_package_mac.py

职责：
    1. 路径 / 常量（各平台脚本共用）
    2. 构建环境检测（BuildConfig + BuildEnvironment）
    3. Gradle 任务执行（run_gradle）
    4. 版本号读取 / 交互输入 / 确认 / 写入 / 还原（写入仅由主编排器调用）
    5. 单键询问 confirm_platform（Enter=是 / Esc=跳过，逐平台选择用）

版本写入原则：各平台脚本只读版本（环境变量优先，否则 app_version.json），
绝不写项目文件；只有主编排器在确认版本号后统一写入。
"""

import json
import os
import re
import shutil
import subprocess
import sys
import tkinter as tk
from pathlib import Path
from tkinter import filedialog
from typing import Optional

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent.parent
SETUP_DIR = PROJECT_DIR / "lwjgl3" / "setup"
CONFIG_FILE = SCRIPT_DIR / "build_config.env"
CONSTRUO_OUTPUT_DIR = PROJECT_DIR / "lwjgl3" / "build" / "construo"

# Construo 目标平台配置（对应 lwjgl3/build.gradle 中的 targets）
CONSTRUO_TARGETS = {
    "linux": "linuxX64",
    "mac": "macX64",
    "macM1": "macM1",
}

# 版本类型映射（与 Java VersionType 保持一致）
VERSION_TYPE_MAP = {0: "beta", 1: "release"}

APP_VERSION_JSON = PROJECT_DIR / "assets" / "asset" / "app_version.json"


def _get_type_name(type_int: int) -> str:
    """将整型版本类型转为名称字符串"""
    return VERSION_TYPE_MAP.get(type_int, "beta")


def _is_windows() -> bool:
    return sys.platform.startswith("win")


def _exe(name: str) -> str:
    """返回平台对应的可执行文件名（Windows 加 .exe，其他平台不加）"""
    return f"{name}.exe" if _is_windows() else name


def _decode(data: bytes) -> str:
    """解码子进程输出，优先 UTF-8，回退 GBK（中文 Windows）"""
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        try:
            return data.decode("gbk")
        except UnicodeDecodeError:
            return data.decode("utf-8", errors="replace")


def run_gradle(task: str, jdk_path: Optional[str] = None, print_output: bool = True) -> bool:
    """运行 Gradle task（实时输出）。

    jdk_path 为 None 时不设 JAVA_HOME（gradlew 自带清华镜像自动供给 JDK，
    macOS/Linux 交叉编译场景适用）；主编排器传入已检测的 JDK 路径则用之。
    """
    env = os.environ.copy()
    if jdk_path:
        env["JAVA_HOME"] = jdk_path
    if _is_windows():
        cmd = f'"{PROJECT_DIR / "gradlew.bat"}" {task}'
        r = subprocess.run(cmd, cwd=str(PROJECT_DIR), env=env, capture_output=True, shell=True)
    else:
        cmd = [str(PROJECT_DIR / "gradlew"), task]
        r = subprocess.run(cmd, cwd=str(PROJECT_DIR), env=env, capture_output=True)
    if print_output and r.stdout:
        print(_decode(r.stdout))
    if r.returncode != 0:
        if r.stderr:
            lines = _decode(r.stderr).strip().splitlines()
            print(f"  [stderr] {'; '.join(lines[-10:])}")
        print(f"  [Gradle 退出码: {r.returncode}]")
    return r.returncode == 0


class BuildConfig:
    """持久化构建环境配置"""

    def __init__(self):
        self.jdk_path: Optional[str] = None
        self.iscc_path: Optional[str] = None
        self._loaded = False

    def load(self):
        if not CONFIG_FILE.exists():
            return
        print("[配置] 加载环境配置...")
        for line in CONFIG_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            key, _, val = line.partition("=")
            key = key.strip()
            val = val.strip()
            if key == "JDK_PATH":
                self.jdk_path = val
            elif key == "ISCC_PATH":
                self.iscc_path = val
        self._loaded = True
        print("[配置] 已加载")

    def save(self):
        print("[配置] 保存环境配置...")
        lines = [
            "# 氢风打包工具 - 构建环境配置",
            "# 由打包工具自动生成，删除此文件可重新检测",
            f"JDK_PATH={self.jdk_path or ''}",
            f"ISCC_PATH={self.iscc_path or ''}",
        ]
        CONFIG_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"[配置] 已保存: {CONFIG_FILE}")


class BuildEnvironment:
    """检测并验证构建工具链"""

    def __init__(self, config: BuildConfig):
        self.config = config
        self.jdk_path: Optional[str] = None
        self.iscc_path: Optional[str] = None
        self.has_mingw = False
        self.mingw_gcc: Optional[str] = None
        self.mingw_bin: Optional[str] = None
        self.android_sdk: Optional[str] = None

    def run_cmd(self, cmd: list[str], timeout=30) -> tuple[int, str]:
        """运行命令并返回 (返回码, stdout)"""
        try:
            r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            return r.returncode, r.stdout.strip() + r.stderr.strip()
        except (subprocess.TimeoutExpired, FileNotFoundError, OSError) as e:
            return -1, str(e)

    def check_jdk21(self, javac_path: str) -> bool:
        """检查指定 javac 是否为 JDK 21"""
        rc, out = self.run_cmd([javac_path, "-version"])
        return rc == 0 and "21" in out

    def find_jdk(self):
        """按优先级查找 JDK 21"""
        # 1. 配置路径
        if self.config.jdk_path:
            javac = Path(self.config.jdk_path) / "bin" / _exe("javac")
            if javac.exists() and self.check_jdk21(str(javac)):
                self.jdk_path = self.config.jdk_path
                print(f"[通过] JDK 21: {self.jdk_path}")
                return

        print("[检测] 查找 JDK 21...")

        # 2. JAVA_HOME
        jh = os.environ.get("JAVA_HOME")
        if jh:
            javac = Path(jh) / "bin" / _exe("javac")
            self.jdk_path = jh
            print(f"[通过] JDK 21 (来自 JAVA_HOME): {self.jdk_path}")
            self.config.jdk_path = jh
            return

        # 3. PATH 中的 javac
        javac_in_path = shutil.which("javac")
        if javac_in_path:
            jdk_from_path = str(Path(javac_in_path).resolve().parent.parent)
            if self.check_jdk21(javac_in_path):
                self.jdk_path = jdk_from_path
                print(f"[通过] JDK 21 (来自 PATH): {self.jdk_path}")
                self.config.jdk_path = jdk_from_path
                return

        # 4. Program Files 自动查找
        for pf in [os.environ.get("ProgramFiles", "C:\\Program Files"),
                   os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)")]:
            if not pf:
                continue
            java_dir = Path(pf) / "Java"
            if not java_dir.exists():
                continue
            for d in sorted(java_dir.glob("jdk-21*"), reverse=True):
                javac = d / "bin" / _exe("javac")
                if javac.exists() and self.check_jdk21(str(javac)):
                    self.jdk_path = str(d)
                    print(f"[通过] JDK 21 (自动查找): {self.jdk_path}")
                    self.config.jdk_path = str(d)
                    return

        # 5. 手动选择
        print("未能自动找到 JDK 21，请手动选择...")
        root = tk.Tk()
        root.withdraw()
        selected = filedialog.askdirectory(
            title="请选择 JDK 21 安装目录",
            initialdir="C:\\Program Files\\Java"
        )
        root.destroy()
        if selected:
            javac = Path(selected) / "bin" / _exe("javac")
            self.jdk_path = selected
            print(f"[通过] JDK 21: {self.jdk_path}")
            self.config.jdk_path = selected
            return

        print("[失败] 未找到 JDK 21。下载: https://adoptium.net/")
        sys.exit(1)

    def find_iscc(self):
        """查找 Inno Setup 编译器"""
        # 1. 配置路径
        if self.config.iscc_path:
            if Path(self.config.iscc_path).exists():
                self.iscc_path = self.config.iscc_path
                print(f"[通过] Inno Setup: {self.iscc_path}")
                return

        print("[检测] 查找 Inno Setup 编译器...")

        # 2. ISCC 环境变量
        iscc_env = os.environ.get("ISCC")
        if iscc_env and Path(iscc_env).exists():
            self.iscc_path = iscc_env
            print(f"[通过] Inno Setup (来自 ISCC 环境变量): {self.iscc_path}")
            self.config.iscc_path = iscc_env
            return

        # 3. Program Files 自动查找
        candidates = [
            f"{pf}\\Inno Setup 6\\ISCC.exe"
            for pf in [
                os.environ.get("ProgramFiles", "C:\\Program Files"),
                os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"),
            ] if pf
        ] + [
            f"{pf}\\Inno Setup 5\\ISCC.exe"
            for pf in [
                os.environ.get("ProgramFiles", "C:\\Program Files"),
                os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"),
            ] if pf
        ]
        for c in candidates:
            if Path(c).exists():
                self.iscc_path = c
                print(f"[通过] Inno Setup (自动查找): {self.iscc_path}")
                self.config.iscc_path = c
                return

        # 4. 手动选择
        print("未能自动找到 Inno Setup...")
        root = tk.Tk()
        root.withdraw()
        selected = filedialog.askopenfilename(
            title="请选择 ISCC.exe",
            initialdir=os.environ.get("ProgramFiles", "C:\\Program Files"),
            filetypes=[("ISCC.exe", "ISCC.exe")]
        )
        root.destroy()
        if selected and Path(selected).exists():
            self.iscc_path = selected
            print(f"[通过] Inno Setup: {self.iscc_path}")
            self.config.iscc_path = selected
            return

        print("[失败] 未找到 Inno Setup。下载: https://jrsoftware.org/isdl.php")
        sys.exit(1)

    def check_mingw(self):
        """检查或自动安装 MinGW-w64 编译器"""
        def _find_existing() -> str | None:
            # 1. x86_64-w64-mingw32-gcc（cross-compiler 命名）
            found = shutil.which("x86_64-w64-mingw32-gcc")
            if found: return found
            # 2. w64devkit（推荐的轻量 MinGW，约 50MB，完整工具链）
            w64devkit = Path("C:/tools/w64devkit/w64devkit/bin/gcc.exe")
            if w64devkit.exists():
                return str(w64devkit)
            # 3. CLion 捆绑的 MinGW（注意：部分版本工具链不完整）
            for pf in [os.environ.get("ProgramFiles", "C:\\Program Files")]:
                clion_dir = Path(pf) / "JetBrains"
                if clion_dir.exists():
                    for d in sorted(clion_dir.glob("CLion*/bin/mingw/bin"), reverse=True):
                        candidate = d / "gcc.exe"
                        if candidate.exists():
                            return str(candidate)
            # 4. PATH 中的 gcc
            found = shutil.which("gcc")
            if found: return found
            return None

        self.mingw_gcc = _find_existing()
        if self.mingw_gcc:
            self.has_mingw = True
            # w64devkit/bin 目录需在 PATH 中，gcc 才能找到 as/ld
            self.mingw_bin = str(Path(self.mingw_gcc).parent)
            print(f"[通过] MinGW-w64: {self.mingw_gcc}")
            return

        print("[检测] 未找到 MinGW-w64，尝试自动安装...")
        if shutil.which("winget"):
            print("  运行: winget install BrechtSanders.WinLibs.POSIX.MSVCRT")
            r = subprocess.run(
                ["winget", "install", "BrechtSanders.WinLibs.POSIX.MSVCRT",
                 "--accept-package-agreements", "--accept-source-agreements"],
                capture_output=True, text=True)
            if r.returncode == 0:
                # winget 安装后可能需要刷新 PATH
                self.mingw_gcc = _find_existing()
                if self.mingw_gcc:
                    self.has_mingw = True
                    print(f"[通过] MinGW-w64 已自动安装: {self.mingw_gcc}")
                    return
            print(f"  自动安装失败: {r.stderr[-200:] if r.stderr else 'unknown'}")
        else:
            print("  winget 不可用，请手动安装:")
            print("  https://github.com/brechtsanders/winlibs_mingw/releases")

        print("[跳过] MinGW-w64 未安装，将使用已有 launcher.exe")

    def find_android_sdk(self):
        """从 local.properties 读取 Android SDK 路径"""
        lp = PROJECT_DIR / "local.properties"
        if lp.exists():
            for line in lp.read_text(encoding="utf-8").splitlines():
                if line.startswith("sdk.dir"):
                    _, _, val = line.partition("=")
                    self.android_sdk = val.strip()
                    return
        print("[提示] 未找到 Android SDK 路径配置（local.properties）")


def read_app_version() -> dict:
    """只读 app_version.json，返回 dict；不存在或解析失败返回 {}"""
    if APP_VERSION_JSON.exists():
        try:
            return json.loads(APP_VERSION_JSON.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return {}
    return {}


def resolve_version() -> tuple[str, str, int]:
    """解析版本号（各平台脚本用）：环境变量优先，否则只读 app_version.json。

    返回 (version, release_type, app_version_int)。均无法解析时报错退出。
    绝不修改任何文件。
    """
    version = os.environ.get("PACKAGE_VERSION") or ""
    release_type = os.environ.get("RELEASE_TYPE") or ""
    app_int_env = os.environ.get("APP_VERSION_INT") or ""

    if not version or not release_type or not app_int_env:
        data = read_app_version()
        if not version:
            version = data.get("app_version_string", "")
        if not release_type:
            type_int = data.get("app_version_type", -1)
            release_type = _get_type_name(type_int) if type_int in (0, 1) else ""
        if not app_int_env:
            app_int_env = str(data.get("app_version", 0))

    if not version or not release_type:
        print("[错误] 无法解析版本号。请先运行主编排器 build_package.py，")
        print("       或设置环境变量 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT")
        sys.exit(1)
    return version, release_type, int(app_int_env or 0)


def check_version_consistency(version: str):
    """只读校验 gradle.properties 的 projectVersion 与解析版本是否一致，不一致仅警告。"""
    gp = PROJECT_DIR / "gradle.properties"
    if not gp.exists():
        return
    m = re.search(r"^projectVersion=(.*)$", gp.read_text(encoding="utf-8"), re.MULTILINE)
    if m and m.group(1).strip() != version:
        print(f"[警告] gradle.properties projectVersion={m.group(1)} 与版本 {version} 不一致")
        print("       建议先运行 build_package.py 统一版本号，否则产物命名可能与展示版本不符")


def input_version_interactive() -> tuple[str, str, int]:
    """交互输入版本（主编排器用）：读取上次版本作默认值，返回 (version, release_type, app_version_int)。"""
    last = read_app_version()
    last_version_str = last.get("app_version_string", "")
    last_type = last.get("app_version_type", -1)
    last_release_type = _get_type_name(last_type) if last_type in (0, 1) else ""
    last_version_int = last.get("app_version", 0)

    version = os.environ.get("PACKAGE_VERSION") or ""
    release_type = os.environ.get("RELEASE_TYPE") or ""
    app_int_env = os.environ.get("APP_VERSION_INT") or ""

    if not version:
        while True:
            try:
                prompt = "请输入版本号"
                if last_version_str:
                    prompt += f" (回车使用上次: {last_version_str})"
                prompt += ": "
                raw = input(prompt).strip()
            except (EOFError, OSError):
                raw = ""
            version = raw or last_version_str
            if version:
                break
            print("版本号不能为空")

    if not release_type:
        while True:
            try:
                prompt = "请输入发布类型 (beta/release)"
                if last_release_type:
                    prompt += f" (回车使用上次: {last_release_type})"
                prompt += ": "
                raw = input(prompt).strip().lower()
            except (EOFError, OSError):
                raw = ""
            release_type = raw or last_release_type
            if release_type in ("beta", "release"):
                break
            print("发布类型只能是 beta 或 release")

    if not app_int_env:
        while True:
            try:
                default_int = last_version_int if last_version_int else 1
                prompt = "请输入版本整型编码"
                if last_version_int:
                    prompt += f" (回车使用上次: {last_version_int})"
                prompt += ": "
                raw = input(prompt).strip()
            except (EOFError, OSError):
                raw = ""
            if not raw:
                app_version_int = default_int
                break
            try:
                app_version_int = int(raw)
                break
            except ValueError:
                print("版本整型编码必须为整数")
    else:
        app_version_int = int(app_int_env)

    return version, release_type, app_version_int


def confirm_version_change(version: str, release_type: str, app_version_int: int):
    """展示版本变更差异，用户确认（Y 继续，n 取消退出）"""
    data = read_app_version()
    old_type = data.get("app_version_type", -1)
    old_type_name = _get_type_name(old_type) if old_type in (0, 1) else "(无)"
    print(f"  版本整型编码: {data.get('app_version', '(无)')} → {app_version_int}")
    print(f"  版本字符串:   {data.get('app_version_string', '(无)')} → {version}")
    print(f"  发行类型:     {old_type_name} → {release_type}")
    confirm = input("确认以上信息无误？(Y/n): ").strip().lower()
    if confirm == "n":
        print("取消打包")
        sys.exit(1)


def update_version_files(version: str, release_type: str, app_version_int: int):
    """写入版本号到项目文件（仅主编排器在确认后调用）。

    涉及：gradle.properties / app_version.json / android/build.gradle / inno_setup.iss(+WebSite.OFFICIAL 同步)。
    inno_setup.iss 首次修改前备份为 .bak，供 restore_backups 还原。
    """
    # gradle.properties
    gp = PROJECT_DIR / "gradle.properties"
    content = gp.read_text(encoding="utf-8")
    content = re.sub(r'^projectVersion=.*', f'projectVersion={version}',
                     content, flags=re.MULTILINE)
    gp.write_text(content, encoding="utf-8")
    print(f"  [信息] gradle.properties projectVersion={version}")

    # app_version.json（运行时版本显示 + 更新检测用）
    if APP_VERSION_JSON.exists():
        ver_data = json.loads(APP_VERSION_JSON.read_text(encoding="utf-8"))
        ver_data["app_version"] = app_version_int
        ver_data["app_version_string"] = version
        ver_data["app_version_type"] = 0 if release_type == "beta" else 1
        APP_VERSION_JSON.write_text(
            json.dumps(ver_data, ensure_ascii=False, indent=2),
            encoding="utf-8"
        )
        print(f"  [信息] app_version.json 已同步: v{version} ({release_type})")
    else:
        print(f"  [警告] 未找到 app_version.json: {APP_VERSION_JSON}")

    # android/build.gradle（APK 系统版本号）
    android_build = PROJECT_DIR / "android" / "build.gradle"
    if android_build.exists():
        content = android_build.read_text(encoding="utf-8")
        content = re.sub(r'versionCode \d+', f'versionCode {app_version_int}', content)
        content = re.sub(r'versionName ".*"', f'versionName "{version}"', content)
        android_build.write_text(content, encoding="utf-8")
        print(f"  [信息] android/build.gradle 已同步: versionCode={app_version_int}, versionName={version}")

    # inno_setup.iss（安装包版本显示 + WebSite.OFFICIAL 同步）
    iss_path = SETUP_DIR / "inno_setup.iss"
    iss_bak = SETUP_DIR / "inno_setup.iss.bak"
    if iss_path.exists():
        # 首次运行时备份原始文件，用于构建结束后恢复
        if not iss_bak.exists():
            shutil.copy2(iss_path, iss_bak)
        content = iss_path.read_text(encoding="utf-8")
        content = content.replace(
            '#define MyAppVersion "1.0.0"',
            f'#define MyAppVersion "{version}-{release_type}"'
        )
        content = re.sub(r'qingfeng-.*\.jar', f'qingfeng-{version}.jar', content)

        # 同步 MyAppURL 为 WebSite.OFFICIAL
        web_site_path = PROJECT_DIR / "core" / "src" / "main" / "java" / "com" / "hujiugame" / "qingfeng" / "type" / "url" / "WebSite.java"
        if web_site_path.exists():
            ws_content = web_site_path.read_text(encoding="utf-8")
            m = re.search(r'OFFICIAL\s*=\s*"([^"]+)"', ws_content)
            if m:
                official_url = m.group(1)
                old_url_match = re.search(r'#define MyAppURL "([^"]*)"', content)
                if old_url_match and old_url_match.group(1) != official_url:
                    content = content.replace(
                        f'#define MyAppURL "{old_url_match.group(1)}"',
                        f'#define MyAppURL "{official_url}"'
                    )
                    print(f"  [信息] MyAppURL 已同步: {official_url}")
                else:
                    print(f"  [信息] MyAppURL 无需更新: {official_url}")
            else:
                print("  [警告] 未找到 WebSite.OFFICIAL 常量")

        iss_path.write_text(content, encoding="utf-8")
        print(f"  [信息] inno_setup.iss 已同步: v{version} ({release_type})")


def restore_backups():
    """还原被临时修改的配置文件（inno_setup.iss），并删除备份。"""
    iss_bak = SETUP_DIR / "inno_setup.iss.bak"
    iss_src = SETUP_DIR / "inno_setup.iss"
    if iss_bak.exists():
        shutil.copy2(iss_bak, iss_src)
        iss_bak.unlink()


def _read_single_key() -> bytes | None:
    """读取单个按键。非交互环境（stdin 非 tty / 读键失败）返回 None。

    注意：Windows 下 msvcrt.getch() 直接从控制台读键，不受 stdin 重定向影响，
    若不先做 isatty 检查，管道/自动化场景会阻塞等待按键。
    """
    try:
        if not sys.stdin.isatty():
            return None
        if _is_windows():
            import msvcrt
            return msvcrt.getch()
        import termios
        import tty
        fd = sys.stdin.fileno()
        old = termios.tcgetattr(fd)
        try:
            tty.setraw(fd)
            return os.read(fd, 1)
        finally:
            termios.tcsetattr(fd, termios.TCSADRAIN, old)
    except Exception:
        return None


def confirm_platform(label: str) -> bool:
    """询问是否打包某平台。Enter=是 / Esc=跳过。返回是否打包。

    非交互环境（stdin 非 tty 或读键失败）默认打包，保证自动化流程不阻塞。
    """
    print(f"是否打包 {label} 平台安装包？[Enter=是 / Esc=跳过] ", end="", flush=True)
    key = _read_single_key()
    if key is None:
        print("(非交互，默认打包)")
        return True
    if key in (b"\r", b"\n"):
        print("→ 打包")
        return True
    if key in (b"\x1b",):
        print("→ 跳过")
        return False
    print("→ 打包")
    return True
