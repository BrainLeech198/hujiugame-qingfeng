# Beta 快照细分版本（Minecraft 法则 YYwWWa）

> **文档定位**：beta 版本引入 Minecraft 快照法则 `YYwWWa` 作为同一 `major.minor.patch` 下的更细分版本的设计记录。
>
> **文档结构**：背景约束 → 版本模型 → 生成规则 → 改动清单 → 不改项。
>
> **更新规范**：实现已落地；后续调整快照格式或生成规则时更新本文档，并同步 `develop/output/README.md` 与 `develop/CHANGELOG.md`。

## 背景与约束

- **只有 beta 有日期码，release 不允许有日期码字符串**（配置层强制为空）
- 一个 `1.0.0` 允许任意多个日期码（`26w32a`、`26w33a`、`26w33b`…），每次 beta 是一次独立构建；新旧靠 `app_version` 整型递增区分
- 产物命名带细分（beta 时），release 命名不变
- 修订字母：按 ISO 周自动生成默认 + 可手动覆盖

## 版本模型

`app_version.json` 新增字段 `app_version_snapshot`，格式 `YYwWWa`（如 `26w33a`）：

```
beta    {app_version: 1, app_version_type: 0, app_version_string: "1.0.0", app_version_snapshot: "26w32a"}
beta    {app_version: 2, app_version_type: 0, app_version_string: "1.0.0", app_version_snapshot: "26w33a"}
release {app_version: 3, app_version_type: 1, app_version_string: "1.0.0", app_version_snapshot: ""}
```

- `app_version_string` 恒为纯 `major.minor.patch`（`parseVersion` 严格三段整数约束不受影响）
- 显示：beta → `v1.0.0-beta-26w33a`；release → `v1.0.0-release`（不变）
- 更新检测以 `app_version` 整型为主；**整型相同时，双方都是 beta 则再按日期码（快照码）细分新旧**（如 `26w33a` → `26w33b` 判更新）；字符串回退比较逻辑不动

## 快照码生成规则

- `YYwWWa` = ISO 年两位 + `w` + ISO 周两位（补零）+ 修订字母
- 自动生成：取当天 `date.isocalendar()` → `26w33`；与上一快照同周则字母递增（a→b→c），否则回到 `a`
- 打包器交互：beta 时显示自动默认值（回车接受 / 手改），严格校验 `^\d{2}w\d{2}[a-z]$`；release 不询问、强制空串

## 改动清单

| 文件 | 改动 |
|------|------|
| `assets/asset/app_version.json` | 加 `app_version_snapshot: ""` |
| `core/.../type/key/VersionKey.java` | 加常量 `APP_VERSION_SNAPSHOT` |
| `core/.../core/UpdateChecker.java` | 读快照；`doFileVersionDifferent` 同时比较快照（beta→beta 触发资源同步）；`generateVersionString` beta 非空时追加 `-{快照}` |
| `develop/output/scripts/build_common.py` | `current_week_code` / `generate_snapshot_default` / `validate_snapshot` / `full_version`；`resolve_version`/`input_version_interactive`/`confirm_version_change`/`update_version_files` 全改 4 元组并写快照；inno MyAppVersion 用 `full_version` |
| `develop/output/build_package.py` | 4 元组解包；dispatch 传 env `APP_VERSION_SNAPSHOT`；tag 用 `full_version` |
| 四平台脚本 `build_package_{windows,linux,android,mac}.py` | `resolve_version` 解包加快照；产物名/显示 tag 用 `full_version`；deb Version、dmg volname 同步 |

## 不改

- `gradle.properties` projectVersion 保持 `{version}`（JAR 名/构建稳定，快照不进 gradle 版本）
- `android/build.gradle` versionName 保持 `{version}`（现状 beta 也不带 `-beta`）
- `parseVersion` 严格三段整数不动
- 官网已加 `newest_version_snapshot` 字段并同步 `versions` 下载区展示（含 `UpdateChecker` 按日期码判断更新）
- 官网下载区改为两阶段弹窗：先选下载来源（蓝奏云 / GitHub / Gitee，mac 的 M1 / Intel 为两个独立来源）→ 再在大弹窗（左上角返回 / 右上角关闭）列该来源的「描述 + 下载链接」，与「不改」的 `versions.json` 数据结构直接映射（`download.<平台>.<源>` = 单对象或数组）
