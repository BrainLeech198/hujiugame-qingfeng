#!/usr/bin/env python3
"""氢风 macOS 专用打包脚本（独立自包含，并入平台拆分结构）

由主编排器 build_package.py 分发调用（通过环境变量传入版本），也可单独运行。
版本来源：环境变量 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT 优先，
否则只读 app_version.json。本脚本只读版本，绝不修改任何项目文件。

用法：
    python build_package_mac.py                     # 双架构（macX64 + macM1）
    python build_package_mac.py --arch macM1         # 仅 Apple Silicon
    python build_package_mac.py --arch macX64        # 仅 Intel
    python build_package_mac.py --dmg                # 额外生成 .dmg（仅 macOS 原生执行时有效）

产物（每个架构产出 .command + 内嵌安装器的 zip）：
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.command   ← AirDrop 首选：双击即自动安装并启动
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.zip       ← 蓝奏云/网盘传这个：解压后双击「安装氢风」
    qingfeng_setup_mac_intel_v1.0.0-release.command            ← Intel 芯片（AirDrop）
    qingfeng_setup_mac_intel_v1.0.0-release.zip                ← Intel 芯片（蓝奏云/网盘）

平台行为：
    macOS 原生  → 构建 + ad-hoc 签名 + ditto zip + 自解压安装器（tar 保符号链接）+ 可选 dmg
    其他平台    → 交叉编译兜底（construo 产出 .app，跳过签名/DMG，zip 用 zipfile，安装器用 tarfile）

传给朋友：
    AirDrop 传输可免 Gatekeeper 确认；微信/网盘传输需双击后点一次「打开」。
"""

import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

from build_common import (
    OUTPUT_DIR, PROJECT_DIR, CONSTRUO_OUTPUT_DIR,
    _decode, run_gradle, resolve_version, check_version_consistency,
    full_version,
)

# construo target -> 产物文件名里的架构标签（直白面向用户）
CONSTRUO_TARGETS = {
    "macX64": "mac_intel",
    "macM1": "mac_apple_silicon",
}

# construo target -> 安装器打包临时文件名里的架构标签（仅内部临时文件用，无空格）
ARCH_LABEL = {
    "macX64": "Intel",
    "macM1": "AppleSilicon",
}


def is_mac() -> bool:
    return sys.platform == "darwin"


def build_target(target_name: str, platform_label: str):
    """通过 construo 构建单个 mac target，返回 .app 路径；失败返回 None。"""
    print(f"[构建] {target_name} ({platform_label})...")
    target_cap = target_name[0].upper() + target_name[1:]
    task = f"lwjgl3:package{target_cap}"
    if not run_gradle(task):
        print(f"[错误] {task} 构建失败")
        return None

    # 预检：runOnFirstThread（macOS 上 LWJGL/GLFW 必须，否则启动崩溃）
    app_json = CONSTRUO_OUTPUT_DIR / target_name / "roast" / "app" / "qingfeng.json"
    if app_json.exists():
        try:
            cfg = json.loads(app_json.read_text(encoding="utf-8"))
            if not cfg.get("runOnFirstThread"):
                print("[警告] 启动配置未启用 runOnFirstThread，macOS 上可能无法启动")
        except json.JSONDecodeError:
            print("[警告] 无法解析启动配置 app.json，请人工确认 runOnFirstThread")
    else:
        print("[警告] 未找到启动配置 app.json，请人工确认 runOnFirstThread")

    # 定位 .app（标准路径 <target>/qingfeng.app，找不到则递归兜底）
    app_dir = CONSTRUO_OUTPUT_DIR / target_name / "qingfeng.app"
    if not app_dir.exists():
        candidates = [p for p in CONSTRUO_OUTPUT_DIR.glob(f"{target_name}/**/*.app")]
        if candidates:
            app_dir = candidates[0]
        else:
            print(f"[错误] 未找到 {target_name} 的 .app 产物，请检查构建输出目录: {CONSTRUO_OUTPUT_DIR / target_name}")
            return None
    print(f"[信息] .app: {app_dir}")
    return app_dir


def verify_app(app_dir) -> bool:
    """只读校验 .app 结构完整性：Contents/MacOS 有启动器、Info.plist 有 bundle 标识。"""
    macos_dir = app_dir / "Contents" / "MacOS"
    plist = app_dir / "Contents" / "Info.plist"
    ok = True

    if not macos_dir.is_dir():
        print(f"[错误] 缺少 Contents/MacOS: {app_dir}")
        ok = False
    elif not [p for p in macos_dir.iterdir() if p.is_file()]:
        print("[错误] Contents/MacOS 为空，.app 启动器缺失")
        ok = False

    if not plist.exists():
        print("[错误] 缺少 Contents/Info.plist")
        ok = False
    else:
        try:
            import plistlib
            with open(plist, "rb") as f:
                bundle_id = plistlib.load(f).get("CFBundleIdentifier", "")
            if not bundle_id:
                print("[警告] Info.plist 缺少 CFBundleIdentifier")
            elif "com.hujiugame" not in bundle_id:
                print(f"[警告] CFBundleIdentifier 异常: {bundle_id}")
        except Exception as e:
            print(f"[警告] Info.plist 解析失败: {e}")
    return ok


def postprocess_app(app_dir) -> bool:
    """macOS 原生：ad-hoc 签名（降低 Gatekeeper 拦截概率）。其他平台跳过。"""
    if not is_mac():
        return True
    print(f"[签名] ad-hoc 签名 {app_dir.name} ...")
    r = subprocess.run(
        ["codesign", "--force", "--deep", "-s", "-", str(app_dir)],
        capture_output=True, text=True,
    )
    if r.returncode != 0:
        print(f"[错误] 签名失败: {r.stderr or r.stdout}")
        return False
    print("[通过] 签名完成")
    return True


def build_dmg(app_dir, dest_dmg, version, release_type, snapshot) -> bool:
    """macOS 原生：hdiutil 生成 .dmg（双击打开，拖入 Applications）。"""
    print(f"[DMG] 生成 {dest_dmg.name} ...")
    r = subprocess.run([
        "hdiutil", "create",
        "-volname", f"QingFeng-{full_version(version, release_type, snapshot)}",
        "-srcfolder", str(app_dir),
        "-ov", "-format", "UDZO",
        str(dest_dmg),
    ], capture_output=True, text=True)
    if r.returncode != 0:
        print(f"[错误] DMG 生成失败: {r.stderr or r.stdout}")
        return False
    print(f"[成功] dmg: {dest_dmg.name}")
    return True


def build_installer_command(app_dir, dest_command, version, release_type, arch_label) -> bool:
    """生成单个 .command 自解压安装器（交付给朋友的首选产物）

    内嵌 .app 的 tar.gz 于脚本末尾；双击后自动安装到「应用程序」并启动。
    macOS 用系统 tar 保留符号链接；交叉编译兜底用 Python tarfile。
    配合 AirDrop 传输（不加 quarantine）可做到零确认一键安装运行。
    """
    print(f"[安装器] 生成 {dest_command.name} ...")

    # 1. 打包 .app 为 tar.gz
    tmp_tgz = dest_command.parent / f".tmp_{arch_label}.tar.gz"
    if is_mac():
        r = subprocess.run(
            ["tar", "-czf", str(tmp_tgz), "-C", str(app_dir.parent), app_dir.name],
            capture_output=True, text=True,
        )
        if r.returncode != 0:
            print(f"[错误] tar 打包失败: {r.stderr or r.stdout}")
            return False
    else:
        import tarfile
        with tarfile.open(tmp_tgz, "w:gz") as tar:
            for f in app_dir.rglob("*"):
                if f.is_file() or f.is_symlink():
                    tar.add(f, arcname=f.relative_to(app_dir.parent), recursive=False)
    tgz_data = tmp_tgz.read_bytes()
    tmp_tgz.unlink()

    # 2. 安装器头脚本（自解压：定位末尾标记 → 解压到 /Applications → 去隔离 → 启动）
    header = (
        "#!/bin/bash\n"
        "# ============================================\n"
        "#  氢风 一键安装（双击运行）\n"
        "#  自动安装到「应用程序」并启动\n"
        "# ============================================\n"
        "\n"
        "LINE=$(grep -an '^#__QINGFENG_APP__$' \"$0\" | cut -d: -f1 || true)\n"
        "if [ -z \"$LINE\" ]; then\n"
        "    echo '错误：安装包数据损坏'\n"
        "    read -rp '按回车退出...'\n"
        "    exit 1\n"
        "fi\n"
        "\n"
        "DEST=/Applications\n"
        "if [ ! -w \"$DEST\" ]; then\n"
        "    DEST=\"$HOME/Applications\"\n"
        "    mkdir -p \"$DEST\"\n"
        "fi\n"
        "\n"
        "echo \"正在安装氢风到 $DEST ...\"\n"
        "rm -rf \"$DEST/qingfeng.app\"\n"
        "tail -n +$((LINE + 1)) \"$0\" | tar -xz -C \"$DEST\"\n"
        "\n"
        "# 去除隔离标记（网盘/微信下载会有；AirDrop 传输则没有）\n"
        "echo '正在解除安全限制...'\n"
        "xattr -dr com.apple.quarantine \"$DEST/qingfeng.app\" 2>/dev/null || true\n"
        "\n"
        "echo ''\n"
        "echo '==========================================='\n"
        "echo '  安装完成！'\n"
        "echo '==========================================='\n"
        "echo ''\n"
        "echo '正在启动氢风...'\n"
        "open \"$DEST/qingfeng.app\"\n"
        "\n"
        "echo ''\n"
        "echo '如果应用没有自动启动，或提示「无法打开」：'\n"
        "echo ''\n"
        "echo '  1. 打开「系统设置」→「隐私与安全性」'\n"
        "echo '  2. 底部找到被阻止的应用提示'\n"
        "echo '  3. 点击「仍要打开」'\n"
        "echo '  4. 再次点击「打开」确认'\n"
        "echo ''\n"
        "echo '以后可从「启动台」或「应用程序」正常打开。'\n"
        "echo ''\n"
        "read -rp '按回车退出...'\n"
        "#__QINGFENG_APP__\n"
    ).encode("utf-8")

    dest_command.write_bytes(header + tgz_data)
    dest_command.chmod(0o755)
    mb = len(tgz_data) // 1024 // 1024
    print(f"[成功] 安装器: {dest_command.name} ({mb} MB)")
    print("      发给朋友，双击即自动安装并启动（AirDrop 传输可免确认）")
    return True


def package_installer_zip(installer_path, dest_zip) -> bool:
    """把自解压 .command 压成 zip（蓝奏云可传），zip 内置中文 symlink 快捷入口。

    zip 内两个条目：
        1. Install QingFeng.command  自解压安装器本体（英文名，0o755 可执行）
        2. 安装氢风                   中文 symlink → 指向 Install QingFeng.command
    macOS 归档实用工具按 zip UNIX 字段还原 symlink 与可执行位。
    """
    dest_zip.parent.mkdir(parents=True, exist_ok=True)
    data = installer_path.read_bytes()
    with zipfile.ZipFile(dest_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        zi = zipfile.ZipInfo("Install QingFeng.command")
        zi.create_system = 3
        zi.external_attr = 0o100755 << 16
        zf.writestr(zi, data)
        zi2 = zipfile.ZipInfo("安装氢风请打开Install QingFeng.command")
        zi2.create_system = 3
        zi2.external_attr = 0o120777 << 16
        zf.writestr(zi2, "Install QingFeng.command")
    mb = dest_zip.stat().st_size // 1024 // 1024
    print(f"[成功] zip: {dest_zip.name} ({mb} MB)")
    if mb >= 100:
        print("[警告] 超过蓝奏云免费 100MB 上限，需付费提升或精简产物")
    return True


def main():
    os.chdir(str(PROJECT_DIR))
    args = sys.argv[1:]

    if "-h" in args or "--help" in args:
        print(__doc__)
        return

    # 参数解析
    if "--arch" in args:
        i = args.index("--arch")
        if i + 1 >= len(args):
            print("[错误] --arch 需要参数 (macX64|macM1)")
            sys.exit(1)
        arch_names = [args[i + 1]]
    else:
        arch_names = list(CONSTRUO_TARGETS.keys())
    for a in arch_names:
        if a not in CONSTRUO_TARGETS:
            print(f"[错误] 未知架构 {a}，可选: {', '.join(CONSTRUO_TARGETS)}")
            sys.exit(1)
    want_dmg = "--dmg" in args

    is_native_mac = is_mac()
    print("=" * 44)
    print("   氢风 macOS 打包工具")
    print("=" * 44)
    if is_native_mac:
        print("[平台] macOS 原生流程")
    else:
        print(f"[提示] 当前平台 {sys.platform}，执行交叉编译兜底（无签名/DMG）")

    # 只读版本（环境变量优先，否则 app_version.json；绝不修改项目文件）
    version, release_type, _, snapshot = resolve_version()
    check_version_consistency(version)
    tag = "v" + full_version(version, release_type, snapshot)
    print(f"[版本] {tag}（只读，不改动项目文件）")
    print()

    ok = True
    produced = []
    for target_name in arch_names:
        platform_label = CONSTRUO_TARGETS[target_name]
        arch_label = ARCH_LABEL[target_name]
        chip_desc = "Apple Silicon（M 芯片）" if target_name == "macM1" else "Intel"
        print(f"[信息] 目标架构: {chip_desc} → 产物前缀 qingfeng_setup_{platform_label}")

        app_dir = build_target(target_name, platform_label)
        if app_dir is None:
            ok = False
            continue
        if not verify_app(app_dir):
            ok = False
            continue
        if not postprocess_app(app_dir):
            ok = False
            continue

        dest_installer = OUTPUT_DIR / f"qingfeng_setup_{platform_label}_{tag}.command"
        if build_installer_command(app_dir, dest_installer, version, release_type, arch_label):
            produced.append(dest_installer)
            dest_zip = OUTPUT_DIR / f"qingfeng_setup_{platform_label}_{tag}.zip"
            if package_installer_zip(dest_installer, dest_zip):
                produced.append(dest_zip)
            else:
                ok = False
        else:
            ok = False

        if want_dmg:
            if not is_native_mac:
                print("[跳过] --dmg 仅支持 macOS 原生执行")
            else:
                dest_dmg = OUTPUT_DIR / f"qingfeng_setup_{platform_label}_{tag}.dmg"
                if build_dmg(app_dir, dest_dmg, version, release_type, snapshot):
                    produced.append(dest_dmg)
                else:
                    ok = False

    print()
    print("=" * 44)
    print("   打包完成" if ok else "   打包失败")
    if ok:
        for f in produced:
            print(f"   {f.name}")
        print("[提示] 发给朋友时，首选「一键安装 .command」；AirDrop 传输可免确认")
    if "macX64" in arch_names:
        print("[提示] Intel 版 .app 在 Apple Silicon Mac 上运行需安装 Rosetta")
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
        if not os.environ.get("PACKAGE_DISPATCHED") and sys.stdin and sys.stdin.isatty():
            try:
                input("按 Enter 键退出...")
            except (EOFError, OSError):
                pass
