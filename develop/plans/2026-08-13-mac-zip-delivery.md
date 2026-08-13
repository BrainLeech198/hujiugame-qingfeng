# 2026-08-13 mac 交付物改为「zip 内嵌自解压安装器」

> **文档定位**：mac 交付物从「裸 .app zip + 独立 .command」调整为「zip 内嵌自解压 .command + 中文 symlink 快捷入口」的设计记录，解决蓝奏云不允许 `.command` 直传的问题。
>
> **文档结构**：背景与约束 → 交付物形态 → 技术要点 → 脚本改动 → 补救本次 → 风险与退路 → 改动清单 → 文档同步。
>
> **更新规范**：实现落地后，若调整 zip 内文件名、symlink 命名或移除/新增产物类型，更新本文档并同步 `develop/output/README.md` 与 `develop/CHANGELOG.md`。

## 背景与约束

- **蓝奏云扩展名白名单**：不含 `.command`，含 `zip` / `dmg` / `pkg`。`.command` 本质是自解压 bash 脚本，被蓝奏云按扩展名拦截。
- **无 macOS 环境**：打包在 Windows 交叉编译完成，`hdiutil` / `pkgbuild` 仅 macOS 可用，因此无法生成 `.dmg` / `.pkg`。只能围绕 `zip` 做交付。
- **交付目标**：傻瓜操作，路径最短，单文件可传，双击即用。
- **现有产物问题**：裸 `.app` zip 解压后面对 `qingfeng.app` 文件夹，小白不知道该双击什么；`.command` 又无法直传蓝奏云。
- **命名约束（用户定案）**：产物文件名前缀必须英文 `qingfeng_setup_`，不用中文前缀；zip 内部 `.command` 用英文名；另建一个中文 symlink 快捷方式指向它，作为用户操作入口。

## 交付物形态

每次 mac 打包产出（双架构 × 2 类型）：

| 文件 | 用途 |
|------|------|
| `qingfeng_setup_mac_apple_silicon_v{ver}.zip` | **蓝奏云/网盘传这个**。外层英文名 |
| `qingfeng_setup_mac_apple_silicon_v{ver}.command` | **AirDrop 首选**。独立产物，英文名，直接传免确认 |
| `qingfeng_setup_mac_intel_v{ver}.zip` / `.command` | Intel 同理 |

`zip` 内部结构（双文件）：

```
qingfeng_setup_mac_apple_silicon_v{ver}.zip
├── Install QingFeng.command   ← 自解压安装器本体（英文名，可执行 0o755）
└── 安装氢风                    ← 中文 symlink → 指向 Install QingFeng.command
```

用户操作链路：**下载 zip → 双击解压 → 双击「安装氢风」→ 自动装到「应用程序」并启动**。symlink 只占几字节，zip 体积≈原 `.command`，不突破蓝奏云 100M 上限。

## 技术要点

- **symlink 写入 zip**（Python `zipfile`，Windows 下即可完成，不依赖真实文件系统 symlink）：
  - 主文件 `Install QingFeng.command`：`zinfo.external_attr = 0o100755 << 16`
  - 中文 symlink「安装氢风」：`zinfo.create_system = 3`（Unix）+ `zinfo.external_attr = 0o120777 << 16`，`writestr` 内容为目标相对路径 `Install QingFeng.command`
- macOS 归档实用工具按 zip UNIX 字段还原 symlink 与可执行位；中文名按 UTF-8 解码。
- 交叉编译（Windows）与 macOS 原生统一走 `zipfile` 压 .command，逻辑一致。

## 脚本改动（`build_package_mac.py`）

1. `build_installer_command` 本体不变；产出 `.command` 后新增一步：用 `zipfile` 压成 `qingfeng_setup_mac_{arch}_{tag}.zip`，zip 内含「Install QingFeng.command」（0o755）+「安装氢风」symlink（0o120777）。
2. 独立 `.command` 产物保持英文产物名不变（AirDrop 场景）。
3. 移除裸 `.app` zip 产物（`package_zip` 对小白无价值，被新 zip 取代）。
4. 打包完成后校验 zip 体积 < 100M 并提示蓝奏云可传。

## 补救本次

现有 `develop/output/` 已有 4 个 `.command`（双架构），写一次性脚本：每个压成同名 zip（zip 内英文 .command + 中文 symlink，保留权限位），覆盖现有裸 `.app` zip；生成后列出体积。

## 风险与退路

- **symlink 需 Mac 实测**：zip 内 symlink 的写入是标准做法，但 mac 解压后双击「安装氢风」是否如预期触发执行，无法在本机（Windows）验证，需找一台 Mac 实测一次。
- **退路**：若 symlink 双击异常，用户改双击英文 `Install QingFeng.command`（脚本内部有中文提示，同样一键安装启动）。zip 内同时提供两个入口，两手准备。

## 改动清单

| 文件 | 改动 |
|------|------|
| `develop/output/scripts/build_package_mac.py` | 新增「压 .command 为 zip（含 symlink）」逻辑；移除裸 `.app` zip 产物 |
| `develop/output/README.md` | mac 产物表格更新（zip 说明改为内含安装器） |
| `develop/CHANGELOG.md` | 记录本次变更 |
| `develop/plans/2026-08-13-mac-zip-delivery.md` | 本文档 |

## 文档同步

- `develop/output/README.md`：mac 产物说明表 + 常见问题里 `.command` 无法直传蓝奏云时的说明。
- `develop/CHANGELOG.md`：按内容拆分提交。
- 本设计文档遵循 develop/ 文档头部自描述规范。
