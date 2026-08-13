#!/usr/bin/env python3
"""氢风 Windows 安装包脚本（独立自包含）

由主编排器 build_package.py 分发调用（通过环境变量传入版本），也可单独运行。
版本来源：环境变量 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT 优先，
否则只读 app_version.json。本脚本只读版本，绝不修改任何项目文件。

流程：编译桌面 JAR → 组装启动器（launcher.exe + jlink JRE + Win7 兼容补丁）
      → Inno Setup 安装包 → 复制成品到 output 目录

用法：
    python build_package_windows.py                  # 使用 app_version.json 的版本
    python build_package_windows.py --config-only    # 仅检测并保存 Windows 工具链配置
"""

import os
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile
import json
import tempfile
from pathlib import Path

from build_common import (
    OUTPUT_DIR, PROJECT_DIR, SETUP_DIR, CONFIG_FILE,
    _exe, _is_windows, run_gradle, resolve_version, check_version_consistency,
    BuildConfig, BuildEnvironment,
)


def assemble_launcher(env: BuildEnvironment, version: str) -> bool:
    """组装 Windows 启动器：编译 launcher.exe + 复制 JAR + jlink 最小 JRE + Win7 兼容补丁 + 瘦身 JAR"""
    print("[构建] 组装 Windows 启动器...")

    launcher_dir = SETUP_DIR / "dist" / "launcher"

    # 4a. 编译 launcher.exe（原生 C，仅依赖 Win7 原生 API）
    launcher_c = SETUP_DIR / "launcher.c"
    launcher_rc = SETUP_DIR / "launcher.rc"
    if env.has_mingw and launcher_c.exists():
        gcc = env.mingw_gcc
        gcc_dir = Path(env.mingw_bin) if env.mingw_bin else None
        # gcc 需要 bin 目录在 PATH 中才能找到 as/ld 等子工具
        gcc_env = os.environ.copy()
        if gcc_dir:
            gcc_env["PATH"] = str(gcc_dir) + os.pathsep + gcc_env["PATH"]
        print(f"[信息] 使用 MinGW-w64 编译 launcher.exe ...")

        # 编译 .rc 资源（嵌入 console.ico）
        if launcher_rc.exists() and gcc_dir:
            windres = str(gcc_dir / "windres.exe")
            res_o = launcher_dir / "launcher_res.o"
            r = subprocess.run([windres, "-O", "coff", str(launcher_rc), str(res_o)],
                               capture_output=True, text=True, env=gcc_env)
            if r.returncode != 0:
                print(f"[错误] 图标资源编译失败: {r.stderr}")
                return False
            rc_o = str(res_o)
            print("  [信息] 图标资源已编译")
        else:
            rc_o = None

        out_exe = str(launcher_dir / "launcher.exe")
        cmd = [gcc, "-O2", "-s", "-static", "-mwindows",
               "-D_WIN32_WINNT=0x0601", "-D_WIN32_IE=0x0601",
               "-o", out_exe, str(launcher_c)]
        if rc_o:
            cmd.append(rc_o)
        cmd.append("-lshlwapi")
        r = subprocess.run(cmd, capture_output=True, text=True, env=gcc_env)
        if r.returncode != 0:
            print(f"[错误] launcher.exe 编译失败: {r.stderr}")
            return False
        print("[通过] launcher.exe 编译成功（已嵌入图标）")
    else:
        if not (launcher_dir / "launcher.exe").exists():
            prebuilt = SETUP_DIR / "launcher.exe"
            if prebuilt.exists():
                shutil.copy2(prebuilt, launcher_dir / "launcher.exe")
                print(f"[信息] 使用预构建启动器: {prebuilt}")
            elif launcher_c.exists():
                print("[信息] 未找到 MinGW-w64 和预构建 launcher.exe")
                print("  安装 w64devkit（推荐，50MB）:")
                print("    https://github.com/skeeto/w64devkit/releases")
                print("  或安装完整 MinGW:")
                print("    winget install BrechtSanders.WinLibs.POSIX.MSVCRT")
                return False
            else:
                print("[错误] launcher.c 不存在，无法编译")
                return False
        else:
            print("[信息] 使用已有启动器: dist/launcher/launcher.exe")

    # 4b. 清理旧构建产物，创建运行时目录
    # 清理 PyInstaller 旧产物（C 启动器不再需要 _internal/）
    old_internal = launcher_dir / "_internal"
    if old_internal.exists():
        shutil.rmtree(old_internal)

    jar_dir = launcher_dir / "lib" / "jar"
    if jar_dir.exists():
        shutil.rmtree(jar_dir)
    jar_dir.mkdir(parents=True)

    # 清理旧 JRE
    for old_jre in launcher_dir.glob("lib/jre-*"):
        shutil.rmtree(old_jre)

    set_json = launcher_dir / "lib" / "set.json"
    set_json.write_text(
        json.dumps({"console": False, "jre": "jre"},
                    ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    # 4c. 复制 JAR（由 lwjgl3:jar 任务产出）
    jar_src = PROJECT_DIR / "lwjgl3" / "build" / "libs" / f"qingfeng-{version}.jar"
    if not jar_src.exists():
        print(f"[错误] JAR 文件不存在: {jar_src}（请先编译桌面 JAR）")
        return False
    shutil.copy2(jar_src, jar_dir / f"qingfeng-{version}.jar")
    print(f"[信息] JAR 已复制: {jar_src.name}")

    # 4d. jlink 生成最小 JRE（固定命名 jre，每次覆盖）
    jre_target = launcher_dir / "lib" / "jre"
    if jre_target.exists():
        shutil.rmtree(jre_target)

    jlink = Path(env.jdk_path) / "bin" / _exe("jlink")
    jmods = Path(env.jdk_path) / "jmods"
    if not jmods.exists():
        print(f"[错误] JDK 缺少 jmods 目录: {jmods}")
        return False

    print(f"[信息] 使用 jlink 生成最小 JRE 到 {jre_target} ...")
    r = subprocess.run([
        str(jlink),
        "--module-path", str(jmods),
        "--add-modules", "java.base,java.desktop,jdk.unsupported",
        "--output", str(jre_target),
        "--strip-debug",
        "--compress", "zip-9",
        "--no-header-files",
        "--no-man-pages",
    ])
    if r.returncode != 0:
        print("[错误] jlink 生成 JRE 失败")
        return False

    # 4e. 下载 api-ms-win-core-path-l1-1-0.dll（Win7 兼容性补丁）
    # 注意：Win7 上 launcher.exe 和 java.exe 都需要此 DLL，
    # 因此必须同时放在启动器根目录和 jre/bin/ 两处。
    # 使用开源 shim：https://github.com/adang1345/api-ms-win-core-path
    dll_name = "api-ms-win-core-path-l1-1-0.dll"
    dll_targets = [
        launcher_dir / dll_name,            # launcher.exe 启动需要
        jre_target / "bin" / dll_name,      # java.exe 启动需要
    ]
    if not all(t.exists() for t in dll_targets):
        dll_cache = OUTPUT_DIR / ".dll_cache"
        dll_cache.mkdir(parents=True, exist_ok=True)
        cache_extracted = dll_cache / dll_name

        # 从缓存或网络获取 DLL
        if cache_extracted.exists():
            dll_src = cache_extracted
            print("[信息] 使用缓存的 Win7 兼容补丁...")
        else:
            # 查找缓存 ZIP（支持新旧两种命名，兼容之前入库的旧文件名）
            cache_zip_names = [
                f"{dll_name}.zip",              # 新命名: api-ms-win-core-path-l1-1-0.dll.zip
                "api-ms-win-core-path.zip",     # 旧命名: GitHub 原始下载名
            ]
            cache_zip = None
            for name in cache_zip_names:
                candidate = dll_cache / name
                if candidate.exists():
                    cache_zip = candidate
                    break

            if not cache_zip or not cache_zip.exists():
                dll_url = "https://github.com/adang1345/api-ms-win-core-path/releases/download/v1.0.0/api-ms-win-core-path.zip"
                print("[信息] 下载 Win7 兼容补丁...")
                try:
                    cache_zip = dll_cache / cache_zip_names[0]
                    urllib.request.urlretrieve(dll_url, cache_zip)
                except Exception as e:
                    print(f"[警告] 下载失败: {e}")
                    print("       在 Windows 7 上运行时可能报错")
                    cache_zip = None
            if cache_zip and cache_zip.exists():
                with zipfile.ZipFile(cache_zip, 'r') as zf:
                    zf.extract(f"x64/{dll_name}", dll_cache)
                (dll_cache / "x64" / dll_name).rename(cache_extracted)
                (dll_cache / "x64").rmdir()
            dll_src = cache_extracted if cache_extracted.exists() else None

        if dll_src:
            for t in dll_targets:
                t.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(dll_src, t)
                os.chmod(t, 0o755)
            print("[通过] Win7 兼容补丁已就绪（launcher + jre/bin）")
    else:
        print("[通过] Win7 兼容补丁已存在")

    # 瘦身 JAR：移除 Linux/macOS 原生库（仅保留 Windows 的 .dll）
    jar_path = jar_dir / f"qingfeng-{version}.jar"
    if jar_path.exists():
        print("[信息] 瘦身 JAR：移除 Linux/macOS 原生库...")
        exclude_patterns = (".so", ".dylib", "linux/", "macos/", "mac/")
        temp_jar = tempfile.NamedTemporaryFile(delete=False, suffix=".jar")
        temp_jar.close()
        removed = 0
        with zipfile.ZipFile(jar_path, "r") as src:
            entries = [e for e in src.infolist() if not any(p in e.filename for p in exclude_patterns)]
            removed = len(src.infolist()) - len(entries)
            with zipfile.ZipFile(temp_jar.name, "w", zipfile.ZIP_DEFLATED, 9) as dst:
                for e in entries:
                    dst.writestr(e, src.read(e.filename))
        shutil.move(temp_jar.name, jar_path)
        print(f"  [信息] 移除了 {removed} 个非 Windows 文件")
    else:
        print(f"  [跳过] JAR 不存在: {jar_path}")

    # 验证
    checks = [
        ("launcher.exe", launcher_dir / "launcher.exe"),
        ("JAR", jar_dir / f"qingfeng-{version}.jar"),
        ("set.json", set_json),
        ("JRE java", jre_target / "bin" / _exe("java")),
    ]
    for name, path in checks:
        if not path.exists():
            print(f"[错误] 缺少 {name}")
            return False
    print("[通过] 启动器结构完整")
    return True


def build_installer(env: BuildEnvironment, version: str) -> bool:
    """编译 Windows 安装包（Inno Setup）"""
    print("[构建] 更新安装包脚本并编译 Windows 安装包...")

    # 更新 ISS 中的 JAR 引用（版本号已在主编排器写入）
    iss_path = SETUP_DIR / "inno_setup.iss"
    content = iss_path.read_text(encoding="utf-8")
    content = re.sub(r'qingfeng-.*\.jar', f'qingfeng-{version}.jar', content)
    iss_path.write_text(content, encoding="utf-8")

    # 复制三语 LICENSE 到安装包目录（随程序安装）
    for lic_name in ("LICENSE", "LICENSE.zh-CN", "LICENSE.zh-TW"):
        lic_src = PROJECT_DIR / lic_name
        lic_dst = SETUP_DIR / lic_name
        if lic_src.exists():
            shutil.copy2(lic_src, lic_dst)
            print(f"  [信息] 已添加许可证: {lic_name}")

    # 生成安装向导展示用的三语合并文件（LicenseFile 引用，UTF-8 BOM）
    def _read_license(name):
        return (PROJECT_DIR / name).read_text(encoding="utf-8").strip()

    sep = "=" * 60
    combined = "\n\n".join([
        sep + "\n简体中文 — 氢风项目许可证（CC BY-NC 4.0 署名—非商业性使用）\n" + sep,
        _read_license("LICENSE.zh-CN"),
        sep + "\n繁體中文 — 氫風專案授權條款（CC BY-NC 4.0 姓名標示—非商業性）\n" + sep,
        _read_license("LICENSE.zh-TW"),
        sep + "\nEnglish — QingFeng Project License (CC BY-NC 4.0 Attribution-NonCommercial)\n" + sep,
        _read_license("LICENSE"),
    ]) + "\n"
    (SETUP_DIR / "LICENSE.combined.txt").write_bytes(
        b"\xef\xbb\xbf" + combined.encode("utf-8"))
    print("  [信息] 已生成三语合并许可证: LICENSE.combined.txt")

    # 运行 ISCC
    print(f"  > ISCC {iss_path}")
    r = subprocess.run([env.iscc_path, str(iss_path)],
                       cwd=str(SETUP_DIR))
    if r.returncode != 0:
        print("[错误] Inno Setup 打包失败")
        return False
    return True


def copy_outputs(version: str, release_type: str):
    """复制 Windows 安装包到 output 目录"""
    print("[复制] 复制成品到 output 目录...")
    output_dir = OUTPUT_DIR
    tag = f"v{version}-{release_type}"

    setup_exe = SETUP_DIR / "dist" / "qingfeng_setup_windows.exe"
    if setup_exe.exists():
        dst = output_dir / f"qingfeng_setup_windows_{tag}.exe"
        shutil.copy2(setup_exe, dst)
        print(f"[成功] EXE: {dst.name}")
    else:
        print(f"[警告] 未找到安装包: {setup_exe}")


def main():
    os.chdir(str(PROJECT_DIR))

    version, release_type, app_version_int = resolve_version()
    check_version_consistency(version)
    tag = f"v{version}-{release_type}"

    print("=" * 44)
    print(f"   氢风 Windows 打包: {tag}")
    print("=" * 44)

    config = BuildConfig()
    config.load()
    env = BuildEnvironment(config)

    env.find_jdk()
    if "--config-only" in sys.argv:
        env.find_iscc()
        env.check_mingw()
        config.save()
        print("配置检测完成，已保存。")
        return

    env.find_iscc()
    env.check_mingw()
    config.save()
    print()

    ok = run_gradle("lwjgl3:jar", jdk_path=env.jdk_path)
    print()
    if ok:
        ok = assemble_launcher(env, version)
    else:
        print("[跳过] 启动器组装因 JAR 失败跳过")
    if ok:
        ok = build_installer(env, version)
    else:
        print("[跳过] 安装包因启动器失败跳过")
    if ok:
        copy_outputs(version, release_type)

    print()
    print("=" * 44)
    print("   打包完成" if ok else "   打包失败")
    print("=" * 44)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"\n[错误] 未捕获的异常: {e}")
    finally:
        try:
            input("按 Enter 键退出...")
        except (EOFError, OSError):
            pass
