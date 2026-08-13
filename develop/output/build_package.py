#!/usr/bin/env python3
"""氢风 自动打包工具（主编排器）

流程：输入并确认版本号 → 统一写入版本文件 → 逐平台询问（Enter=打包 / Esc=跳过）
      → 分发到各平台脚本（子进程 + 环境变量传版本）→ 还原临时配置 → 汇总产物

各平台构建由独立脚本承担（build_common.py 提供公共逻辑）：
    build_package_windows.py  /  build_package_linux.py
    build_package_android.py  /  build_package_mac.py

用法：
    python build_package.py                        # 交互：逐个询问 Windows/Linux/Android/macOS
    python build_package.py --windows --linux      # 只打包指定平台（跳过询问，可组合）
    python build_package.py --config-only          # 仅检测并保存环境配置
"""

import os
import subprocess
import sys
from pathlib import Path

# 主编排器置于 output 根目录，公共模块与平台脚本在 scripts/ 子目录，注入 sys.path 以正常 import
sys.path.insert(0, str(Path(__file__).resolve().parent / "scripts"))

from build_common import (
    SCRIPT_DIR, PROJECT_DIR,
    BuildConfig, BuildEnvironment,
    input_version_interactive, confirm_version_change,
    update_version_files, restore_backups, confirm_platform,
)

# 平台定义：(标识, 脚本文件名, 显示名)。询问顺序即此顺序。
PLATFORMS = [
    ("windows", "build_package_windows.py", "Windows"),
    ("linux",   "build_package_linux.py",   "Linux"),
    ("android", "build_package_android.py", "Android"),
    ("mac",     "build_package_mac.py",     "macOS"),
]

# 显式指定平台（--windows / --linux / --android / --mac），可组合
NEW_FLAG_MAP = {
    "--windows": "windows",
    "--linux": "linux",
    "--android": "android",
    "--mac": "mac",
}


def detect_all_and_save():
    """完整检测工具链并保存配置（--config-only 用）"""
    config = BuildConfig()
    config.load()
    env = BuildEnvironment(config)
    env.find_jdk()
    env.find_iscc()
    env.check_mingw()
    env.find_android_sdk()
    config.save()


def dispatch(script_name: str, version: str, release_type: str, app_version_int: int) -> bool:
    """以子进程运行平台脚本，环境变量传入版本。返回是否成功。"""
    script = SCRIPT_DIR / script_name
    if not script.exists():
        print(f"[错误] 未找到脚本: {script}")
        return False
    env = os.environ.copy()
    env["PACKAGE_VERSION"] = version
    env["RELEASE_TYPE"] = release_type
    env["APP_VERSION_INT"] = str(app_version_int)
    # 标记为分发模式：平台脚本末尾不再单独"按 Enter 退出"，由主编排器全部完成后统一提示
    env["PACKAGE_DISPATCHED"] = "1"
    cmd = [sys.executable, str(script)]
    print(f"\n===== 分发: {script_name} =====")
    r = subprocess.run(cmd, env=env)
    return r.returncode == 0


def parse_platforms(args: list[str]) -> list[str] | None:
    """解析平台选择：返回显式指定的平台标识列表；无参数返回 None（交互询问全部）。"""
    specified = [NEW_FLAG_MAP[f] for f in args if f in NEW_FLAG_MAP]
    return specified or None


def main():
    os.chdir(str(PROJECT_DIR))
    args = sys.argv[1:]

    print("=" * 44)
    print("   氢风 自动打包工具")
    print("=" * 44)
    print()

    if "--config-only" in args:
        detect_all_and_save()
        print("配置检测完成，已保存。")
        return

    # 版本确认 + 统一写入版本文件
    version, release_type, app_version_int = input_version_interactive()
    print()
    print("=" * 44)
    print(f"   开始打包: v{version}-{release_type}")
    print("=" * 44)
    print()
    confirm_version_change(version, release_type, app_version_int)
    update_version_files(version, release_type, app_version_int)
    print()

    # 逐平台询问（或按参数指定）
    requested = parse_platforms(args)
    if requested is None:
        chosen = [(pid, script, label) for pid, script, label in PLATFORMS if confirm_platform(label)]
    else:
        chosen = [(pid, script, label) for pid, script, label in PLATFORMS if pid in requested]

    if not chosen:
        print("未选择任何平台，退出。")
        restore_backups()
        return

    try:
        results = {}
        for pid, script, label in chosen:
            print(f"\n[平台] 打包 {label} ...")
            results[pid] = dispatch(script, version, release_type, app_version_int)
    finally:
        # 无论成败都还原被临时修改的配置文件（inno_setup.iss）
        restore_backups()

    # 汇总
    print()
    print("=" * 44)
    if all(results.values()):
        print("   打包完成")
    else:
        print("   部分失败")
    for pid, script, label in chosen:
        print(f"   {label}: {'成功' if results.get(pid) else '失败'}")
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
            input("全部选中的平台已打包完毕，按 Enter 键退出...")
        except (EOFError, OSError):
            pass
