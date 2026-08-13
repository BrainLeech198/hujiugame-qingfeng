# 氢风打包工具

> **文档定位**：一键打包工具的使用说明和流水线详解，涵盖多平台脚本结构、版本管理体系和常见问题。
>
> **文档结构**：
> - 按 `概述 → 脚本结构 → 使用方法 → 交互流程 → 流水线说明 → 版本管理体系 → 常见问题` 顺序编排
> - 版本管理体系用表格列出三字段 × 六存储位置的对应关系
>
> **更新规范**：
> 1. 【必须】更新 `develop/CHANGELOG.md` 记录本次变更
> 2. 【必须】修改打包流程或脚本行为时同步更新本文档
> 3. 【必须】修改版本编码规则时同步更新版本管理体系表格
> 4. 【如果】修改构建说明 → 同步更新 `CONTRIBUTING.md`

氢风打包工具将项目从源码编译为可分发的安装包，按平台拆分为独立脚本，由主编排器统一调度：

- **主编排器 `build_package.py`**：确认版本号 → 统一写入版本文件 → **逐平台询问**（Enter=打包 / Esc=跳过）→ 分发各平台脚本
- **公共模块 `build_common.py`**：工具链检测、Gradle 执行、版本读写、单键询问
- **`build_package_windows.py`**：exe 安装包
- **`build_package_linux.py`**：deb + 自解压 sh
- **`build_package_android.py`**：APK
- **`build_package_mac.py`**：.app + zip + 一键 .command 安装器 + 可选 dmg

支持 Windows（exe 安装包）、Android（APK）、Linux（deb/一键 sh）、macOS（.app / 一键 .command）。

---

## 脚本结构

```
develop/output/
├── build_package.py                    主编排器（版本确认 + 逐平台询问 + 分发，置于根目录便于直接运行）
├── scripts/                            平台脚本与公共模块（收拢于此）
│   ├── build_common.py                 公共模块（各脚本共用）
│   ├── build_package_windows.py        Windows 安装包
│   ├── build_package_linux.py          Linux 安装包
│   ├── build_package_android.py        Android APK
│   ├── build_package_mac.py            macOS 安装包
│   ├── build_package_server.py         官网更新服务器脚本（独立，不受影响）
│   └── build_config.env                环境配置（自动生成）
└── qingfeng_setup_*.{exe/apk/deb/sh/zip/command}   打包产物（安装包）
```

每个平台脚本**独立自包含**：可被主编排器分发调用，也可单独运行。版本来源环境变量优先，否则只读 `app_version.json`——**只有主编排器会写入版本文件**。平台脚本收拢在 `scripts/` 子目录，主编排器置于根目录方便 `python build_package.py` 直接运行，产物统一输出到 `develop/output/` 根目录。

---

## 使用方法

### 前置条件

- JDK 21+（自动检测 JAVA_HOME 或 PATH）
- Android SDK（路径配置在 `local.properties: sdk.dir`）
- Windows 额外需要：
  - Inno Setup 6（安装包制作）
  - MinGW-w64（C 启动器编译，未安装则使用预构建 `launcher.exe`）

### 命令

```bash
# 交互打包：输入确认版本后，逐个询问各平台（Enter=打包 / Esc=跳过）
python build_package.py

# 只打包指定平台（跳过询问，参数可组合）
python build_package.py --windows
python build_package.py --linux --android
python build_package.py --windows --linux --android --mac

# 仅检测工具链环境，不打包
python build_package.py --config-only

# 单独运行某平台脚本（使用 app_version.json 的版本）
python scripts/build_package_windows.py
python scripts/build_package_linux.py
python scripts/build_package_android.py
python scripts/build_package_mac.py
```

### 交互流程

1. 输入版本信息（展示上次打包的值作为参考，留空沿用）：

   ```
   请输入版本号 (例如 1.0.0) [上次: 1.0.0]:
   请输入发布类型 (beta/release) [上次: release]:
   请输入版本整型编码 (通常递增) [上次: 1]:
   ```

2. 确认版本信息无误（Y/n）
3. 主编排器统一写入版本文件（gradle.properties / app_version.json / android/build.gradle / inno_setup.iss）
4. **逐平台询问**：

   ```
   是否打包 Windows 平台安装包？[Enter=是 / Esc=跳过] 
   是否打包 Linux 平台安装包？[Enter=是 / Esc=跳过] 
   是否打包 Android 平台安装包？[Enter=是 / Esc=跳过] 
   是否打包 macOS 平台安装包？[Enter=是 / Esc=跳过] 
   ```

5. 依次分发各平台脚本（子进程 + 环境变量传版本；各平台脚本被分发时不再单独等待，全部完成后统一退出）
6. 还原临时配置（inno_setup.iss）并汇总结果，最后统一显示 **"全部选中的平台已打包完毕，按 Enter 键退出"**

> 交互结束时机：**所有选中的平台（含跳过的）全部处理完毕后**才统一提示按 Enter 退出。中途某个平台打包失败不影响流程继续，最终汇总会逐一列出每个平台的成功/失败。

---

## 构建流水线

### 主编排器

```
input_version_interactive  → 版本输入
    ↓
confirm_version_change     → 版本确认
    ↓
update_version_files       → 统一写入版本文件（gradle.properties / app_version.json / android/build.gradle / inno_setup.iss）
    ↓
confirm_platform × 4       → 逐平台询问（Enter / Esc）
    ↓
dispatch × N               → 子进程分发平台脚本（env 传 PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT / PACKAGE_DISPATCHED）
    ↓
restore_backups            → 还原 inno_setup.iss（并汇总结果，统一提示按 Enter 退出）
```

> `PACKAGE_DISPATCHED=1`：主编排器分发时设置，平台脚本据此跳过各自的"按 Enter 退出"等待，由主编排器全部完成后统一提示。

### Windows（build_package_windows.py）

```
lwjgl3:jar               → 编译桌面 JAR
    ↓
assemble_launcher        → 编译 launcher.exe + 复制 JAR + jlink 最小 JRE + Win7 兼容补丁 + 瘦身 JAR
    ↓
build_installer          → Inno Setup 安装包
    ↓
copy_outputs             → 复制 exe 到 output
```

### Linux（build_package_linux.py）

```
construo(linuxX64)       → 跨平台包 + 瘦身 JAR（移除 Windows DLL）
    ↓
build_deb                → .deb 安装包
    ↓
build_install_sh         → 自解压一键安装 .sh
```

### Android（build_package_android.py）

```
use_viewport=fit         → 临时切换视口模式
    ↓
android:assembleRelease  → 编译 APK（交互输入签名密码）
    ↓
use_viewport=stretch     → 恢复桌面端视口模式（无论成败）
    ↓
copy_outputs             → 复制 apk 到 output
```

> **Android 签名密钥安全规范（必须遵守）**：
> - 签名密码（storePassword / keyPassword）**只允许**通过环境变量（`STORE_PASSWORD` / `KEY_PASSWORD`）或交互式输入传入，**绝对不允许**写入任何文件（脚本、配置、JSON、属性文件等）
> - 密钥库文件 `release.jks` 已被 `.gitignore` 忽略，不得提交到仓库
> - 一旦密码被写入文件并提交，即视为密钥泄露，必须更换密钥库

### macOS（build_package_mac.py）

```
construo(macX64/macM1)   → 构建 .app（默认双架构，--arch 可选）
    ↓
verify_app               → 校验 .app 结构
    ↓
postprocess_app          → ad-hoc 签名（macOS 原生）
    ↓
package_zip              → ditto zip
    ↓
build_installer_command  → 一键安装 .command（发给朋友的首选）
    ↓
build_dmg                → 可选 .dmg（--dmg，仅 macOS 原生）
```

### 输出成品

| 平台 | 命名格式 | 说明 |
|------|---------|------|
| Windows | `qingfeng_setup_windows_v{version}-{type}.exe` | 安装包 |
| Android | `qingfeng_setup_android_v{version}-{type}.apk` | APK |
| Linux | `qingfeng_setup_linux_v{version}-{type}.deb` | deb 安装包 |
| Linux | `qingfeng_setup_linux_v{version}-{type}.sh` | 自解压一键安装脚本 |
| macOS | `qingfeng_setup_mac{M1}_{version}-{type}.zip` | .app zip |
| macOS | `氢风一键安装_{M1|Intel}_{version}-{type}.command` | 一键安装器（AirDrop 免确认） |
| macOS | `qingfeng_setup_mac{M1}_{version}-{type}.dmg` | 可选 dmg |

---

## 版本管理体系

### 三个版本号字段

| 字段 | 类型 | 说明 | 对比用途 |
|------|------|------|---------|
| `app_version` | int | 单调递增的整型编码 | 运行时更新检测的主依据 |
| `app_version_string` | string | `major.minor.patch` 格式 | 展示给用户看 |
| `app_version_type` | int | `0`=beta, `1`=release | 同版本号时判断 beta→release 升级 |

### 版本存储位置

| 存储位置 | 文件 | 用途 |
|---------|------|------|
| 源码内 | `assets/asset/app_version.json` | 打包到 JAR/APK 内部，运行时读取 |
| 用户目录 | `~/hujiugame/qingfeng/asset/app_version.json` | 首次运行由 UpdateChecker 复制到外部 |
| 官网 | `docs/data/versions.json`（GitHub Pages） | 远程版本检测对照 |
| Gradle | `gradle.properties: projectVersion` | JAR 文件名和 manifest |
| Android | `android/build.gradle: versionCode/versionName` | 系统级版本标识 |
| 安装包 | `lwjgl3/setup/inno_setup.iss: MyAppVersion` | 安装包属性显示 |

### 版本号使用规范

- **app_version（整型）**: 每次发布递增 1。原则上不跳跃、不回退
- **app_version_string**: 遵循语义化版本 `major.minor.patch`。major 不兼容时递增 major，功能新增递增 minor，bug 修复递增 patch
- **app_version_type**: beta 阶段用 `0`，正式发布用 `1`。同版本号从 beta 升级到 release 时触发更新提醒

---

## 常见问题

### 打包后 APK versionCode 未更新

检查 `android/build.gradle` 是否被 Git 恢复。主编排器在 `update_version_files()` 阶段写入，如果打包前 `git checkout` 过该文件，需要重新运行。

### 如何跳过版本确认直接打包

设置环境变量可非交互运行：

```bash
export PACKAGE_VERSION=1.0.1
export RELEASE_TYPE=release
export APP_VERSION_INT=2
python build_package.py
```

主编排器会直接采用环境变量值，不再逐个输入。

### 如何自动化指定平台（跳过逐平台询问）

```bash
python build_package.py --windows --linux --android --mac
```

### 产出版本号与预期不符

`gradle.properties` 中的 `projectVersion` 是 Gradle 编译入口。如果 JAR 文件名版本不对，检查主编排器的 `update_version_files()` 是否成功修改了 `projectVersion`。

### 单独运行平台脚本时版本不一致

平台脚本只读版本不写入。若直接运行 `build_package_windows.py` 等，会读取 `app_version.json`，并与 `gradle.properties` 比对，不一致会给出警告。建议始终从主编排器 `build_package.py` 入口打包。
