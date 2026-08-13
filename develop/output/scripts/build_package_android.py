#!/usr/bin/env python3
"""氢风 Android APK 脚本（独立自包含）

由主编排器 build_package.py 分发调用（通过环境变量传入版本），也可单独运行。
版本来源：环境变量 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT 优先，
否则只读 app_version.json。本脚本只读版本，绝不修改任何项目文件。

流程：切换 use_viewport=fit → assembleRelease（交互输入签名密码）→ 恢复 stretch → 复制 APK

用法：
    python build_package_android.py                  # 使用 app_version.json 的版本
"""

import json
import os
import shutil
import sys
from pathlib import Path

from build_common import (
    OUTPUT_DIR, PROJECT_DIR,
    run_gradle, resolve_version, check_version_consistency,
    BuildConfig, BuildEnvironment, full_version,
)


def _set_use_viewport(value: str):
    """修改 user_config.json 中的 use_viewport（Android=fit，桌面端=stretch）"""
    config_path = PROJECT_DIR / "assets" / "asset" / "user_config.json"
    config = json.loads(config_path.read_text(encoding="utf-8"))
    config["use_viewport"] = value
    config_path.write_text(
        json.dumps(config, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    print(f"  [信息] use_viewport 已切换为: {value}")


def build_apk(env: BuildEnvironment) -> str | None:
    """编译 Android APK。返回 APK 文件路径；失败返回 None。

    Android 使用 fit 视口模式，编译后无论成败都恢复 stretch（桌面端）。
    签名密码读取顺序：环境变量 STORE_PASSWORD / KEY_PASSWORD，否则交互输入。
    """
    print("[构建] 编译 Android APK...")

    # Android 使用 fit 视口模式，编译后恢复 stretch（桌面端）
    _set_use_viewport("fit")

    try:
        store_pass = os.environ.get("STORE_PASSWORD") or input("请输入 Android storePassword: ").strip()
        key_pass = os.environ.get("KEY_PASSWORD") or input("请输入 Android keyPassword: ").strip()
        password_flags = f"-PstorePassword={store_pass} -PkeyPassword={key_pass}"
        ok = run_gradle(f"android:assembleRelease {password_flags}", jdk_path=env.jdk_path)
    finally:
        # 确保无论打包成败，user_config.json 都恢复为桌面端视口模式
        _set_use_viewport("stretch")

    if not ok:
        print("[错误] Android 打包失败")
        return None

    apk_dir = PROJECT_DIR / "android" / "build" / "outputs" / "apk"
    apks = list(apk_dir.rglob("*.apk"))
    if not apks:
        print("[错误] 未找到编译完成的 APK 文件")
        return None
    apk_file = str(apks[0])
    print(f"[信息] APK: {apk_file}")
    return apk_file


def copy_outputs(apk_file: str, version: str, release_type: str, snapshot: str):
    """复制 APK 到 output 目录"""
    print("[复制] 复制成品到 output 目录...")
    output_dir = OUTPUT_DIR
    tag = "v" + full_version(version, release_type, snapshot)
    dst = output_dir / f"qingfeng_setup_android_{tag}.apk"
    shutil.copy2(apk_file, dst)
    print(f"[成功] APK: {dst.name}")


def main():
    os.chdir(str(PROJECT_DIR))

    version, release_type, app_version_int, snapshot = resolve_version()
    check_version_consistency(version)
    tag = "v" + full_version(version, release_type, snapshot)

    print("=" * 44)
    print(f"   氢风 Android 打包: {tag}")
    print("=" * 44)

    config = BuildConfig()
    config.load()
    env = BuildEnvironment(config)
    env.find_jdk()
    env.find_android_sdk()
    config.save()
    print()

    apk_file = build_apk(env)
    ok = apk_file is not None
    if ok:
        copy_outputs(apk_file, version, release_type, snapshot)

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
