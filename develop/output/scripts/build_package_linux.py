#!/usr/bin/env python3
"""氢风 Linux 安装包脚本（独立自包含）

由主编排器 build_package.py 分发调用（通过环境变量传入版本），也可单独运行。
版本来源：环境变量 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT 优先，
否则只读 app_version.json。本脚本只读版本，绝不修改任何项目文件。

流程：construo(linuxX64) 跨平台包 → 瘦身 JAR → .deb 安装包 → 自解压 .sh 一键安装器
产物（写入 develop/output/，beta 带快照码如 v1.0.0-beta-26w32a）：
    qingfeng_setup_linux_v1.0.0-release.deb
    qingfeng_setup_linux_v1.0.0-release.sh   ← 发给 Linux 用户的首选（双击即可图形化安装）

用法：
    python build_package_linux.py                  # 使用 app_version.json 的版本
"""

import gzip
import io
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
import zipfile
from pathlib import Path

from build_common import (
    OUTPUT_DIR, PROJECT_DIR, CONSTRUO_OUTPUT_DIR,
    run_gradle, resolve_version, check_version_consistency,
    BuildConfig, BuildEnvironment, full_version,
)

CONSTRUO_TARGET = "linuxX64"
PLATFORM_LABEL = "linux"


def build_construo(env: BuildEnvironment, version: str) -> bool:
    """通过 construo 插件构建 Linux 跨平台包，并瘦身 JAR（移除 Windows 原生库）"""
    print("[构建] Linux 包（construo）...")
    task = f"lwjgl3:package{CONSTRUO_TARGET}"
    ok = run_gradle(task, jdk_path=env.jdk_path)
    if not ok:
        print("[错误] Linux 包构建失败")
        return False

    pkg_dir = CONSTRUO_OUTPUT_DIR / CONSTRUO_TARGET

    # 瘦身 JAR：移除 Windows 原生库
    roast_jar = pkg_dir / "roast" / f"qingfeng-{version}.jar"
    if roast_jar.exists():
        print("  [信息] 瘦身 JAR：移除 Windows 原生库...")
        exclude_patterns = (".dll", "windows/", "windows32/", "windows64/")
        temp_jar = tempfile.NamedTemporaryFile(delete=False, suffix=".jar")
        temp_jar.close()
        removed = 0
        with zipfile.ZipFile(roast_jar, "r") as src:
            entries = [e for e in src.infolist() if not any(p in e.filename for p in exclude_patterns)]
            removed = len(src.infolist()) - len(entries)
            with zipfile.ZipFile(temp_jar.name, "w", zipfile.ZIP_DEFLATED) as dst:
                for e in entries:
                    dst.writestr(e, src.read(e.filename))
        shutil.move(temp_jar.name, roast_jar)
        print(f"  [信息] 移除了 {removed} 个 Windows 文件，JAR 已瘦身")
    return True


def _dir_size(path: Path) -> int:
    total = 0
    for f in path.rglob("*"):
        if f.is_file():
            total += f.stat().st_size
    return total


def _make_tar(files: dict, mode: int = 0o644) -> bytes:
    """创建 tar.gz，files 为 {路径: 内容}"""
    buf = io.BytesIO()

    # 收集所有需要创建的目录
    dirs: set[str] = set()
    for name in files:
        parent = Path(name).parent
        while parent and parent.name:
            dirs.add(parent.as_posix())
            parent = parent.parent

    with gzip.GzipFile(fileobj=buf, mode="w", mtime=0) as gz:
        with tarfile.open(fileobj=gz, mode="w|", format=tarfile.USTAR_FORMAT) as tar:
            # 先写目录条目
            for dir_name in sorted(dirs):
                info = tarfile.TarInfo(name=dir_name + "/")
                info.type = tarfile.DIRTYPE
                info.mtime = 0
                info.mode = 0o755
                info.uname = "root"
                info.gname = "root"
                tar.addfile(info)
            # 再写文件
            for name, content in files.items():
                info = tarfile.TarInfo(name=name)
                if isinstance(content, str):
                    content = content.encode("utf-8")
                info.size = len(content)
                info.mtime = 0
                info.mode = mode
                info.uname = "root"
                info.gname = "root"
                tar.addfile(info, io.BytesIO(content))
    return buf.getvalue()


def _write_ar(path: Path, control_tar: bytes, data_tar: bytes):
    """将 control.tar.gz + data.tar.gz 打包为 .deb（ar 格式）"""
    # debian-binary
    debian_binary = b"2.0\n"

    # ar 全局头
    buf = io.BytesIO()
    buf.write(b"!<arch>\n")

    def ar_write(file_name: str, content: bytes):
        """写入一个 ar 文件条目"""
        # 补齐到偶数长度
        if len(content) % 2 == 1:
            content += b"\n"
        # ar header: 文件名必须用空格填充
        name = file_name.ljust(16, " ")[:16].encode("ascii")
        size = f"{len(content):10}".encode("ascii")
        hdr = (
            name +
            b"0           " +   # timestamp (12 spaces)
            b"0     " +          # owner (6 spaces)
            b"0     " +          # group (6 spaces)
            b"100644  " +        # mode (8 spaces)
            size +
            b"\x60\x0a"          # ar magic
        )
        buf.write(hdr)
        buf.write(content)

    ar_write("debian-binary", debian_binary)
    ar_write("control.tar.gz", control_tar)
    ar_write("data.tar.gz", data_tar)

    path.write_bytes(buf.getvalue())


def build_deb(version: str, release_type: str, snapshot: str) -> Path | None:
    """将 construo 构建产物打包为 .deb（Linux 安装包），返回 .deb 路径"""
    print("[构建] 打包 linux .deb 安装包...")

    construo_dir = CONSTRUO_OUTPUT_DIR / CONSTRUO_TARGET
    roast_dir = construo_dir / "roast"
    if not roast_dir.exists():
        print(f"[错误] 未找到 construo 产物: {roast_dir}")
        return None

    tag = "v" + full_version(version, release_type, snapshot)
    deb_name = f"qingfeng_setup_{PLATFORM_LABEL}_{tag}"
    deb_path = OUTPUT_DIR / f"{deb_name}.deb"

    # 生成 .desktop 文件
    desktop_content = (
        "[Desktop Entry]\n"
        "Type=Application\n"
        f"Name=氢风\n"
        f"Comment=QingFeng Launcher\n"
        f"Exec=/usr/lib/qingfeng/qingfeng %f\n"
        f"Icon=qingfeng\n"
        "Categories=Game;\n"
        "MimeType=application/x-qingfeng-game;application/x-qingfeng-language;application/x-qingfeng-theme;application/x-qingfeng-game-language;application/x-qingfeng-game-theme;\n"
        "Terminal=false\n"
    )

    # 生成 postinst 脚本
    postinst_content = (
        "#!/bin/bash\n"
        "set -e\n"
        # 更新 MIME 数据库
        "if command -v update-mime-database >/dev/null 2>&1; then\n"
        "    update-mime-database /usr/share/mime || true\n"
        "fi\n"
        # 更新桌面数据库
        "if command -v update-desktop-database >/dev/null 2>&1; then\n"
        "    update-desktop-database /usr/share/applications || true\n"
        "fi\n"
        # 更新图标缓存
        "if command -v gtk-update-icon-cache >/dev/null 2>&1; then\n"
        "    gtk-update-icon-cache -f /usr/share/icons/hicolor || true\n"
        "fi\n"
        "exit 0\n"
    )

    # 生成 postrm 脚本
    postrm_content = (
        "#!/bin/bash\n"
        "set -e\n"
        "if command -v update-desktop-database >/dev/null 2>&1; then\n"
        "    update-desktop-database /usr/share/applications || true\n"
        "fi\n"
        "if command -v update-mime-database >/dev/null 2>&1; then\n"
        "    update-mime-database /usr/share/mime || true\n"
        "fi\n"
        "exit 0\n"
    )

    # 构建 control.tar.gz
    control_files = {
        "control": (
            f"Package: qingfeng\n"
            f"Version: {full_version(version, release_type, snapshot)}\n"
            f"Section: games\n"
            f"Priority: optional\n"
            f"Architecture: amd64\n"
            f"Maintainer: QingFeng Team\n"
            f"Installed-Size: {_dir_size(roast_dir) // 1024}\n"
            f"Description: QingFeng Launcher\n"
            f" A visual novel game launcher built with libGDX.\n"
        ),
        "postinst": postinst_content,
        "postrm": postrm_content,
    }
    control_tar = _make_tar(control_files, mode=0o755)

    # 构建 data.tar.gz 的内容映射
    data_files = {}

    # Launcher 脚本（入口）
    data_files["usr/bin/qingfeng"] = (
        "#!/bin/bash\n"
        'exec /usr/lib/qingfeng/qingfeng "$@"\n'
    )

    # .desktop 文件
    data_files["usr/share/applications/qingfeng.desktop"] = desktop_content

    # 图标
    icon_path = PROJECT_DIR / "lwjgl3" / "icons" / "logo.png"
    if icon_path.exists():
        data_files["usr/share/icons/hicolor/256x256/apps/qingfeng.png"] = icon_path.read_bytes()

    # MIME 类型注册（.qfg 游戏包 / .qfl 语言包 / .qft 主题包 / .qfgl 游戏语言包 / .qfgt 游戏主题包）
    data_files["usr/share/mime/packages/x-qingfeng.xml"] = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<mime-info xmlns="http://www.freedesktop.org/standards/shared-mime-info">\n'
        '    <mime-type type="application/x-qingfeng-game">\n'
        '        <comment>QingFeng Game Package</comment>\n'
        '        <glob pattern="*.qfg"/>\n'
        '        <icon name="qingfeng"/>\n'
        '    </mime-type>\n'
        '    <mime-type type="application/x-qingfeng-language">\n'
        '        <comment>QingFeng Language Pack</comment>\n'
        '        <glob pattern="*.qfl"/>\n'
        '        <icon name="qingfeng"/>\n'
        '    </mime-type>\n'
        '    <mime-type type="application/x-qingfeng-theme">\n'
        '        <comment>QingFeng Theme Pack</comment>\n'
        '        <glob pattern="*.qft"/>\n'
        '        <icon name="qingfeng"/>\n'
        '    </mime-type>\n'
        '    <mime-type type="application/x-qingfeng-game-language">\n'
        '        <comment>QingFeng Game Language Pack</comment>\n'
        '        <glob pattern="*.qfgl"/>\n'
        '        <icon name="qingfeng"/>\n'
        '    </mime-type>\n'
        '    <mime-type type="application/x-qingfeng-game-theme">\n'
        '        <comment>QingFeng Game Theme Pack</comment>\n'
        '        <glob pattern="*.qfgt"/>\n'
        '        <icon name="qingfeng"/>\n'
        '    </mime-type>\n'
        '</mime-info>\n'
    )

    # 应用文件（roast 目录全部内容）
    for f in roast_dir.rglob("*"):
        if f.is_file():
            rel = f.relative_to(roast_dir)
            target = f"usr/lib/qingfeng/{rel.as_posix()}"
            data_files[target] = f.read_bytes()

    data_tar = _make_tar(data_files, mode=0o755)

    # 生成 .deb（ar 归档格式）
    try:
        _write_ar(deb_path, control_tar, data_tar)
        print(f"[成功] .deb: {deb_path.name}")
        return deb_path
    except Exception as e:
        print(f"[错误] .deb 生成失败: {e}")
        return None


def build_install_sh(deb_path: Path) -> bool:
    """生成一键安装包（自解压式，.deb 内嵌在脚本末尾）

    输出单个 .sh 文件，用户双击即可图形化安装。
    Linux 版 Windows .exe 安装包。
    """
    # 命名: qingfeng_setup_linux_v1.0.0-beta.deb → qingfeng_setup_linux_v1.0.0-beta.sh
    platform_tag = deb_path.stem.replace("qingfeng_setup_", "")
    installer_path = deb_path.parent / f"qingfeng_setup_{platform_tag}.sh"

    deb_data = deb_path.read_bytes()

    # 脚本头：校验 → pkexec 提权 → 自解压 → dpkg -i
    header = (
        "#!/bin/bash\n"
        "# 氢风 一键安装包（自解压）\n"
        "\n"
        'SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"\n'
        'INSTALLER="$0"\n'
        "\n"
        '# 如果是被 pkexec 调用，$0 可能是临时文件，从原路径读取\n'
        'if [ ! -f "$INSTALLER" ] || [ "$(head -c 4 "$INSTALLER" 2>/dev/null)" != "#!/b" ]; then\n'
        '    INSTALLER="$SCRIPT_DIR/$(basename "$0")"\n'
        "fi\n"
        "\n"
        "# 非 root → pkexec 提权（图形密码框）\n"
        'if [ "$EUID" -ne 0 ]; then\n'
        '    if command -v pkexec >/dev/null 2>&1; then\n'
        '        pkexec bash "$INSTALLER" --install\n'
        '        EXIT_CODE=$?\n'
        '        if command -v zenity >/dev/null 2>&1; then\n'
        '            if [ $EXIT_CODE -eq 0 ]; then\n'
        '                zenity --info --title="氢风安装" \\\n'
        '                    --text="安装成功！\\n请在应用程序菜单中启动 氢风。" \\\n'
        '                    --width=300\n'
        '            else\n'
        '                zenity --error --title="氢风安装" \\\n'
        '                    --text="安装失败，请尝试在终端中运行:\\nchmod +x $INSTALLER\\nsudo bash $INSTALLER" \\\n'
        '                    --width=300\n'
        '            fi\n'
        '        fi\n'
        '        exit $EXIT_CODE\n'
        '    else\n'
        '        echo "此安装需要 root 权限。请尝试: sudo bash $INSTALLER"\n'
        '        read -rp "按 Enter 键退出..."\n'
        '        exit 1\n'
        '    fi\n'
        "fi\n"
        "\n"
        "# === 以下以 root 执行 ===\n"
        'if [ "$1" != "--install" ]; then\n'
        '    exec bash "$INSTALLER" --install\n'
        "fi\n"
        "\n"
        'echo "正在安装 氢风..."\n'
        "\n"
        "# 自解压：提取脚本末尾的内嵌 .deb\n"
        'ARCHIVE_START=$(grep -an "^#__DEB_ARCHIVE__$" "$INSTALLER" | cut -d: -f1)\n'
        'if [ -z "$ARCHIVE_START" ]; then\n'
        '    echo "错误: 安装包数据损坏" >&2\n'
        '    exit 1\n'
        "fi\n"
        "\n"
        'DEB_TMP=$(mktemp --tmpdir qingfeng-install.XXXXXX.deb)\n'
        'trap "rm -f $DEB_TMP" EXIT\n'
        "\n"
        'tail -n +$((ARCHIVE_START + 1)) "$INSTALLER" > "$DEB_TMP"\n'
        "\n"
        'if [ "$(head -c 7 "$DEB_TMP")" != "!<arch>" ]; then\n'
        '    echo "错误: 提取的安装包数据无效" >&2\n'
        '    exit 1\n'
        "fi\n"
        "\n"
        'dpkg -i "$DEB_TMP"\n'
        'EXIT_CODE=$?\n'
        "\n"
        'if [ $EXIT_CODE -ne 0 ] && command -v apt-get >/dev/null 2>&1; then\n'
        '    echo "正在修复依赖关系..."\n'
        '    apt-get install -f -y -qq\n'
        '    EXIT_CODE=$?\n'
        "fi\n"
        "\n"
        'exit $EXIT_CODE\n'
        "#__DEB_ARCHIVE__\n"
    ).encode("utf-8")

    installer_path.write_bytes(header + deb_data)
    installer_path.chmod(0o755)
    deb_kb = len(deb_data) // 1024
    print(f"[成功] 一键安装包: {installer_path.name} ({deb_kb} KB 内嵌 .deb)")
    print(f"[成功] DEB: {deb_path.name}（保留，供蓝奏云等不支持 .sh 的平台分发）")
    return True


def main():
    os.chdir(str(PROJECT_DIR))

    version, release_type, app_version_int, snapshot = resolve_version()
    check_version_consistency(version)
    tag = "v" + full_version(version, release_type, snapshot)

    print("=" * 44)
    print(f"   氢风 Linux 打包: {tag}")
    print("=" * 44)

    config = BuildConfig()
    config.load()
    env = BuildEnvironment(config)
    env.find_jdk()
    config.save()
    print()

    ok = build_construo(env, version)
    if ok:
        deb_path = build_deb(version, release_type, snapshot)
        ok = deb_path is not None
        if ok:
            build_install_sh(deb_path)
    else:
        print("[警告] Linux 包构建失败，无产物")

    print()
    print("=" * 44)
    print("   打包完成" if ok else "   打包失败")
    print("=" * 44)
    if not ok:
        sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"\n[错误] 未捕获的异常: {e}")
    finally:
        # 主编排器分发时（PACKAGE_DISPATCHED=1）跳过等待，由主编排器全部完成后统一提示
        if not os.environ.get("PACKAGE_DISPATCHED"):
            try:
                input("按 Enter 键退出...")
            except (EOFError, OSError):
                pass
