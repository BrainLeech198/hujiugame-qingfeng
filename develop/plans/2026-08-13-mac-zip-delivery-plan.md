# mac 交付物改为「zip 内嵌自解压安装器」实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 mac 交付物可传蓝奏云：把自解压 `.command` 压成 zip，zip 内放英文 `.command` + 中文 symlink 快捷入口，替代现有裸 `.app` zip。

**Architecture:** 复用 `build_package_mac.py` 现有的 `build_installer_command` 产物，新增 `package_installer_zip` 用 Python `zipfile` 把它压成 zip（写 `external_attr` 保留可执行位 + 写入 symlink 元数据，不依赖 macOS）。移除裸 `.app` zip。用一次性脚本补救现有 4 个 `.command`。

**Tech Stack:** Python 3（`zipfile` / `pathlib`），无第三方依赖，无测试框架（验证靠生成真实产物 + `zipfile` 读回校验）。

## Global Constraints

- 蓝奏云扩展名白名单：`zip` 允许、`.command` 不允许 → 交付物外层必须是 `zip`
- 产物文件名前缀英文 `qingfeng_setup_`，mac 架构标签 `mac_apple_silicon` / `mac_intel`（见 `build_package_mac.py` 的 `CONSTRUO_TARGETS`）
- zip 内：主安装器英文名 `Install QingFeng.command`（可执行 `0o100755`）；中文 symlink 名「安装氢风」（`0o120777`，内容 `Install QingFeng.command`）
- zip 体积须 < 100M（蓝奏云免费上限），生成后校验并提示
- 独立 `.command` 产物保留（AirDrop 场景），文件名不变
- 交叉编译（Windows）与 macOS 原生统一走 `zipfile`，逻辑一致

---

### Task 1: `build_package_mac.py` 新增「压 .command 为 zip」并移除裸 .app zip

**Files:**
- Modify: `develop/output/scripts/build_package_mac.py`（新增 `package_installer_zip` 函数；`main()` 调整；删除 `package_zip` 函数；同步更新模块 `__doc__` 顶部产物说明）

**Interfaces:**
- Produces: `package_installer_zip(installer_path: Path, dest_zip: Path) -> bool` —— 供 Task 2 的补救脚本复用

- [ ] **Step 1: 新增 `package_installer_zip` 函数**

在 `build_installer_command` 函数之后插入：

```python
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
        zi.external_attr = 0o100755 << 16
        zf.writestr(zi, data)
        zi2 = zipfile.ZipInfo("安装氢风")
        zi2.create_system = 3
        zi2.external_attr = 0o120777 << 16
        zf.writestr(zi2, "Install QingFeng.command")
    mb = dest_zip.stat().st_size // 1024 // 1024
    print(f"[成功] zip: {dest_zip.name} ({mb} MB)")
    if mb >= 100:
        print("[警告] 超过蓝奏云免费 100MB 上限，需付费提升或精简产物")
    return True
```

- [ ] **Step 2: 删除 `package_zip` 函数**

删除整个 `package_zip(app_dir, dest_zip)` 函数（第 140-158 行，裸 .app zip，不再使用）。

- [ ] **Step 3: 调整 `main()` 产物生成顺序**

将 `main()` 内循环里的：

```python
        dest_zip = OUTPUT_DIR / f"qingfeng_setup_{platform_label}_{tag}.zip"
        if package_zip(app_dir, dest_zip):
            produced.append(dest_zip)
        else:
            ok = False
            continue

        dest_installer = OUTPUT_DIR / f"qingfeng_setup_{platform_label}_{tag}.command"
        if build_installer_command(app_dir, dest_installer, version, release_type, arch_label):
            produced.append(dest_installer)
        else:
            ok = False
```

改为：

```python
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
```

- [ ] **Step 4: 同步模块 `__doc__` 产物说明**

将 `__doc__` 中：

```
产物（每个架构产出 zip + 自解压安装器）：
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.command   ← Apple Silicon（M 芯片）首选：双击即自动安装并启动
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.zip
    qingfeng_setup_mac_intel_v1.0.0-release.command            ← Intel 芯片
    qingfeng_setup_mac_intel_v1.0.0-release.zip
```

改为：

```
产物（每个架构产出 .command + 内嵌安装器的 zip）：
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.command   ← AirDrop 首选：双击即自动安装并启动
    qingfeng_setup_mac_apple_silicon_v1.0.0-release.zip       ← 蓝奏云/网盘传这个：解压后双击「安装氢风」
    qingfeng_setup_mac_intel_v1.0.0-release.command            ← Intel 芯片（AirDrop）
    qingfeng_setup_mac_intel_v1.0.0-release.zip                ← Intel 芯片（蓝奏云/网盘）
```

- [ ] **Step 5: 语法检查**

Run: `python -m py_compile develop/output/scripts/build_package_mac.py`
Expected: 无输出、退出码 0

- [ ] **Step 6: Commit**

```bash
git add develop/output/scripts/build_package_mac.py
git commit -m "变更(package): mac zip 改为内嵌自解压安装器（含中文 symlink 快捷入口）"
```

---

### Task 2: 一次性补救脚本，把现有 4 个 `.command` 转成可上传 zip

**Files:**
- Create: `develop/output/scripts/repack_mac_command_zip.py`（一次性的，复用 Task 1 的 `package_installer_zip`）

**Interfaces:**
- Consumes: `package_installer_zip(installer_path, dest_zip)`（from Task 1）
- Produces: 覆盖 `develop/output/qingfeng_setup_mac_*.zip`（旧裸 .app zip → 内嵌安装器 zip）

- [ ] **Step 1: 写补救脚本**

```python
#!/usr/bin/env python3
"""一次性补救：把现有 mac .command 安装器压成可传蓝奏云的 zip（覆盖裸 .app zip）。

遍历 develop/output/ 下 qingfeng_setup_mac_*_v*.command，
每个压成同名 .zip（zip 内 Install QingFeng.command + 「安装氢风」symlink）。
"""
import sys
from pathlib import Path

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
```

- [ ] **Step 2: 运行补救脚本**

Run: `python develop/output/scripts/repack_mac_command_zip.py`
Expected: 打印 4 个 `.command → .zip`，每行 `[成功] zip: ... (xx MB)`，退出码 0

- [ ] **Step 3: 校验 zip 结构与权限位**

Run: `python -c "import zipfile; [print(f.name, oct(z.external_attr>>16), z.create_system) for z in zipfile.ZipFile('develop/output/qingfeng_setup_mac_apple_silicon_v1.0.0-beta-26w33b.zip').infolist()]"`
Expected:
```
Install QingFeng.command 0o100755 3
安装氢风 0o120777 3
```
且 zip 体积 < 100M（`ls -lah` 确认）。

- [ ] **Step 4: Commit**

```bash
git add develop/output/scripts/repack_mac_command_zip.py
git commit -m "构建(package): 一次性补救脚本将现有 mac .command 压成可传蓝奏云的 zip"
```

> 注意：产物 zip 文件在 `develop/output/` 下，确认是否被 `.gitignore` 忽略（之前有 `qingfeng_setup_` 忽略规则），忽略则无需也不应 add。

---

### Task 3: 文档同步

**Files:**
- Modify: `develop/output/README.md`
- Modify: `develop/CHANGELOG.md`
- Modify: `DOCUMENTATION_INDEX.md`

- [ ] **Step 1: 更新 README.md**

- 概述段第 22 行：`build_package_mac.py`：.app + zip + 一键 .command 安装器 + 可选 dmg → 改为 `.app + 一键 .command 安装器 + zip（内嵌安装器）+ 可选 dmg`
- macOS 流水线（第 167-181 行）：`package_zip → ditto zip` 改为 `package_installer_zip → zip 内嵌自解压安装器（含中文 symlink 入口）`
- 输出成品表格（第 191-196 行）：mac 两行 zip 的「说明」列改为 `内嵌一键安装器 zip（蓝奏云可传，解压双击「安装氢风」）`；`.command` 行说明保留 AirDrop 首选

- [ ] **Step 2: 更新 CHANGELOG.md**

在最新条目（`## 2026-08-13 — 官网样式/脚本外置 + 共用 JS + onerror 抽离`）上方插入：

```markdown
## 2026-08-13 — mac 交付物改为 zip 内嵌安装器

### 构建

- **mac zip 内嵌安装器** — `build_package_mac.py` 移除裸 `.app` zip，改为把自解压 `.command` 压成 zip（zip 内英文 `Install QingFeng.command` + 中文 symlink「安装氢风」快捷入口），解决蓝奏云不允许 `.command` 直传；一次性脚本 `repack_mac_command_zip.py` 补救现有产物；`develop/output/README.md` 产物表同步
```

- [ ] **Step 3: 更新 DOCUMENTATION_INDEX.md**

在 `develop/plans/` 相关区段按现有格式新增一条 `develop/plans/2026-08-13-mac-zip-delivery.md`（及 plan 文档）索引（先 Read 该文件确认格式再改）。

- [ ] **Step 4: Commit**

```bash
git add develop/output/README.md develop/CHANGELOG.md DOCUMENTATION_INDEX.md develop/plans/2026-08-13-mac-zip-delivery.md develop/plans/2026-08-13-mac-zip-delivery-plan.md
git commit -m "文档(打包): mac zip 内嵌安装器交付物说明同步"
```

> 提交前先 `git diff develop/CHANGELOG.md` 确认是否包含用户未提交的改动；若含，仅 add 我的条目改动冲突部分需先与用户确认，避免把用户未完成工作卷进提交。

---

## Self-Review 记录

- **Spec 覆盖**：交付物形态（Task 1 产 zip）✓；symlink 技术要点（Task 1 代码）✓；移除裸 .app zip（Task 1 Step 2/3）✓；补救本次（Task 2）✓；体积校验（Task 1 函数内警告 + Task 2 Step 3 校验）✓；文档同步（Task 3）✓；风险退路已写入 spec，不阻塞实施。
- **占位符扫描**：无 TBD/TODO；每步含完整代码。
- **类型一致性**：`package_installer_zip(installer_path, dest_zip)` 在 Task 1 定义、Task 2 复用，签名一致；产物名沿用现有 `qingfeng_setup_mac_{platform_label}_{tag}`。
