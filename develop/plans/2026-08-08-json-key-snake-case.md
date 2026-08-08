# JSON 键 snake_case 统一迁移 — 设计方案

> **状态:** 方案已确认（2026-08-08）：目标格式 snake_case（全小写下划线，`appVersion→app_version`）；程序消费键 21 个 + 场景动画键 3 个纳入迁移；locale 标识（`en_US`/`zh_CN`/`zh_TW`）属"值"不迁移；对象 tag 不迁移；直接替换、无兼容层。
>
> **背景：** 当前 JSON 配置文件键名混用 camelCase（`appVersion`、`useViewport`、`textKey`…），与程序消费的其他 snake_case 数据（如 `GameInfoKey` 运行时键 `user.sound_volume.music`、文件名 `app_version.json`）不一致。目标是将所有程序消费的 JSON 对象键统一为 snake_case。

---

## 背景与目标

- 现状：JSON 对象键命名不统一，部分 camelCase（配置/UI/主题键），部分 snake_case（运行时数据键、文件名）
- 目标：所有**程序消费的 JSON 对象键**统一 snake_case（全小写 + 下划线）
- 边界（用户确认）：
  - locale 标识（`en_US`/`zh_CN`/`zh_TW`）是"值"不是关键字，**不迁移**
  - 对象 tag（如 `select_lastRole`、`language`、`back`）是内容标识，**不迁移**
  - 兼容性：直接替换、无兼容层

## 迁移键清单（29 个）

### 程序消费键（26 个）

| 常量类 | 当前键 | 迁移后 | 出处文件（仓库内） |
|--------|--------|--------|---------------------|
| `VersionKey` | `appVersion` | `app_version` | `app_version.json` |
| `VersionKey` | `appVersionType` | `app_version_type` | `app_version.json` |
| `VersionKey` | `appVersionString` | `app_version_string` | `app_version.json` |
| `JsonKey` | `textKey` | `text_key` | 各 `layout.json` |
| `JsonKey` | `fontSize` | `font_size` | 各 `layout.json`、`message_box.json` |
| `JsonKey` | `fontColor` | `font_color` | 各 `layout.json`、`theme.json`、`ui/button|label/*.json`、`message_box.json` |
| `JsonKey` | `fontFlag` | `font_flag` | 各 `layout.json`、`message_box.json` |
| `JsonKey` | `fontArgs` | `font_args` | `message_box.json` |
| `JsonKey` | `fontName` | `font_name` | 无静态数据（预留键） |
| `JsonKey` | `padX`/`padY`（font_args 子字段） | `pad_x`/`pad_y` | `message_box.json`（`pad` 单字母不变） |
| `ThemeKey` | `fontUseSize` | `font_use_size` | `theme.json` |
| `ThemeKey` | `primaryColor` | `primary_color` | `theme.json` |
| `ThemeKey` | `secondaryColor` | `secondary_color` | `theme.json` |
| `ConfigKey` | `useViewport` | `use_viewport` | `user_config.json` |
| `ConfigKey` | `soundVolume` | `sound_volume` | `user_config.json` |
| `ConfigKey` | `logLevel` | `log_level` | 外部 `log_config.json` |
| `ConfigKey` | `fileLogLevel` | `file_log_level` | 外部 `log_config.json` |
| `GraphicsKey`/`LayoutKey` | `backgroundPicture` | `background_picture` | 各 `layout.json` |
| `LayoutKey` | `backgroundMusic` | `background_music` | 各 `layout.json` |
| `UiKey` | `messageBox` | `message_box` | `ui_config.json` |
| `UiKey` | `borderScale` | `border_scale` | 外部 `button.json`/`label.json` |
| `RequirementKey` | `priorityConfirmUi` | `priority_confirm_ui` | 各 `config.json` |
| `RequirementKey` | `pageMaxGame` | `page_max_game` | `menu_list/config.json` |
| `ScriptKey` | `argumentName` | `argument_name` | 脚本指令 JSON（预留键） |
| `ScriptKey` | `defaultValue` | `default_value` | 脚本指令 JSON（预留键） |
| `ScriptKey` | `thenCommands` | `then_commands` | 脚本指令 JSON（预留键） |
| `ScriptKey` | `elseCommands` | `else_commands` | 脚本指令 JSON（预留键） |
| `UiKey` | `backgroundColor` | `background_color` | Label kind 样式（预留键） |

### 场景动画键（3 个）

| 当前键 | 迁移后 | 出处 |
|--------|--------|------|
| `fadeIn` | `fade_in` | `menu_main/config.json` animation 节点 |
| `fadeOut` | `fade_out` | `menu_main/config.json` animation 节点 |
| `fromPage` | `from_page` | `menu_main/config.json` animation 节点 |

## 不迁移项

- **locale 标识**：`en_US` / `zh_CN` / `zh_TW`（language_config.json）——属于"值"
- **对象 tag**：`select_lastRole` / `select_nextRole` 及 layout 内 `language`/`back`/`start` 等——内容标识

## 涉及文件

### 仓库内 JSON（约 21 个）

- `assets/asset/app_version.json`
- `assets/asset/user_config.json`
- `assets/asset/theme/default_theme/theme.json`
- `assets/asset/theme/default_theme/asset/ui/ui_config.json`
- `assets/asset/theme/default_theme/asset/ui/message_box/message_box.json`
- `assets/asset/theme/default_theme/asset/ui/button/de.json`、`de2.json`
- `assets/asset/theme/default_theme/asset/ui/label/de.json`、`de2.json`、`de3.json`、`mb.content.json`、`mb.title.json`
- 各页面 `layout.json`：`config_basic`、`config_display`、`menu_list`、`menu_load`、`menu_main`
- 各页面 `config.json`：`config_basic`、`config_display`、`menu_list`、`menu_main`

### 常量类（9 个）

- `VersionKey.java`、`JsonKey.java`、`ThemeKey.java`、`ConfigKey.java`、`GraphicsKey.java`、`LayoutKey.java`、`UiKey.java`、`RequirementKey.java`

### 仓库外数据

- `qingfeng/log/log_config.json`（`logLevel`/`fileLogLevel`）
- `qingfeng/asset/user_config.json`（`soundVolume`/`useViewport`）
- `qingfeng/asset/app_version.json`（`appVersion*`）
- swxq 游戏：`theme.json`、`ui_config.json`、`button.json`、`label.json`、`message_box.json`、各页面 `layout.json`/`config.json`、story 布局（`textKey`/`fontSize`/`fontColor`/`fontFlag`）

## 实施步骤

1. 修改仓库内 JSON 文件：camelCase 键 → snake_case
2. 修改 8 个常量类：字符串值同步改 snake_case
3. 修改仓库外数据（log_config / 外部 user_config、app_version / swxq 全部）
4. 同步更新 `develop/JSON_STANDARD.md`（键名文档引用）
5. 编译验证（`./gradlew :core:compileJava`）
6. 文档：更新 `develop/CHANGELOG.md`，`DOCUMENTATION_INDEX.md` 新增本文档条目

## 验证

- 仓库内 JSON 全部 `camelCase` 键清零（脚本复查）
- `./gradlew :core:compileJava` 编译通过
- 启动器 + swxq 游戏运行时配置读取正常
