#!/usr/bin/env python3
"""一次性补救：把现有 mac .command 安装器压成可传蓝奏云的 zip（覆盖裸 .app zip）。

遍历 develop/output/ 下 qingfeng_setup_mac_*_v*.command，
每个压成同名 .zip（zip 内 Install QingFeng.command + 「安装氢风」symlink）。
"""
import sys

from build_package_mac import OUTPUT_DIR, package_installer_zip

if __name__ == "__main__":
    installers = sorted(
        OUTPUT_DIR.glob("qingfeng_setup_mac_*_v*.command")
    )
    if not installers:
        print("[错误] 未找到 mac .command 产物")
        sys.exit(1)
    ok = True
    for inst in installers:
        dest = inst.with_suffix(".zip")
        print(f"[信息] {inst.name} → {dest.name}")
        if not package_installer_zip(inst, dest):
            ok = False
    print("全部完成" if ok else "存在失败")
    sys.exit(0 if ok else 1)
