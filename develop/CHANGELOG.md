# 更新日志

> **文档定位**：项目变更日志，按时间倒序记录每次提交的变更内容。
>
> **文档结构**：
> - 按日期倒序排列，每个日期一个条目
> - 每个日期条目标题格式：`日期 — 核心主题1 + 核心主题2 + ...`
> - 条目内段落按 `新增 → 功能 → 变更 → 重构 → 修复 → 资产 → 文档 → 构建 → 网站 → 类型 → 移除 → 编码规范 → 优化 → 其他` 顺序排列
> - 同一日期多条独立变更用 `---` 分隔
>
> **更新规范**：
> 1. 【必须】每次提交前更新本文档，新条目插在最前面
> 2. 【必须】遵循上述文档结构的格式要求
> 3. 【如果】新增/修改 JSON 配置格式 → 同步更新 `develop/JSON_STANDARD.md`
> 4. 【如果】修改脚本指令/值系统 → 同步更新 `develop/SCRIPT_INTERNAL_STANDARD.md`
> 5. 【如果】新增/重命名/删除 `.md` 文件 → 同步更新 `DOCUMENTATION_INDEX.md`
> 6. 【如果】新建设计方案文档 → 建议在 `develop/plans/` 目录记录
> 7. 【如果】本次更新比较重要、与项目关键设计相关 → 同步写入 `temp/CLAUDE_MEMORY.md`（gitignored 本地工作记忆，仅当前开发机可见），便于后续 AI 对话延续上下文
> 8. 【必须】CHANGELOG 条目不独立提交：每个内容改动 = 一笔提交，同时包含对应改动的文件 + 本文档中该改动对应的条目。先改文件并写对应条目 → 一并提交 → 再改下一个文件、写下一条目；按内容逐条拆分提交，禁止攒一堆 CHANGELOG 更新最后统一提交（例：文件 `a`、`b` 均有改动 → 提交 1 = `a` + 「更新了 a」条目；提交 2 = `b` + 「更新了 b」条目）
> 9. 【必须】每次发布新版本（beta 或正式版）→ 在 `develop/PUBLISH.md` 顶部新增该版本发布记录（版本号 + 版本类型 + 发布时间戳 + 该版本最后一次更新 commit + 面向玩家的 bug 修复/新增功能摘要）；发布时间戳 = 最后一次更新版本下载地址（`docs/data/versions.json`）的 git 提交时间
> 10. 【必须】CHANGELOG 条目按日期归组：同一天的所有提交主题共同包含在同一个 `## <日期> — <概括标题>` 下，禁止拆成多个 `## ` 日期标题；每个提交块以 `**<主题>**（commit <7位短哈希>）` 标记，块内每条 `- ` 条目行尾标注引入它的提交短哈希 `（commit <7位短哈希>）`。新条目随内容改动提交后，其哈希在下一笔内容改动提交中一并补写（补写仅改 hash，不新增条目）
> 11. 【必须】写版本发布说明（官网 `docs/data/versions.json` 的 `log`、`develop/PUBLISH.md` 面向玩家摘要）时，按「上一版本发布点 → 本版本发布点」之间的 commit 区间梳理玩家可见改动（修复的 Bug / 新增功能 / 优化），逐一写入，不要遗漏跨版本才生效的修复（打包之后完成的 bug 修复会随下一个版本发出，须算入下一版本的说明）；beta 测试版按此梳理书写即可，release 正式版属重要更新，需正式、系统地书写

## 2026-09-02 — 官网迁移至 Vue 3 + Vite 构建 + 版权许可增强

**构建(官网)：Vite + Vue 3 脚手架搭建（commit 328ce1d）**

### 构建

- **构建工具链** — 引入 Vite 6 + @vitejs/plugin-vue 5 + Vue 3.5 + vue-router 4.5 + vue-i18n 11，`index.html` 作为 SPA 入口，hash 路由（commit 328ce1d）
- **构建配置** — `outDir: 'docs'` 输出构建产物、`base: './'` 适配 GitHub Pages 子路径部署；`.gitignore` 追加 `node_modules/`、`dist/`（commit 328ce1d）

---

**重构(官网)：页面组件化 Vue 迁移（commit —）**

### 重构

- **SPA 组件化改造** — 静态多页面重构为 Vue 单页应用：TopNav / SiteFooter / SkipLink / DownloadModal / Lightbox 公共组件 + Home / Help / History / Community / Copyright / License / Thirdparty 七个页面视图（commit —）

### 网站

- **CC BY-NC 4.0 官方链接** — license 页正文补充 Creative Commons 官方许可链接（by-nc/4.0），明确项目授权条款（commit —）
- **CC 官方徽章按钮** — 版权页标题栏新增 licensebuttons.net 官方 88×31 徽章，悬停增强 + 移动端缩放适配（commit —）
- **副标题接入 i18n** — 首页打字机文案改用语言文件 key，切换语言自动以新语言重播（commit —）
- **减动效淡入** — `prefers-reduced-motion: reduce` 下跳过逐字动画、改为整句淡入，避免文字生硬闪现（commit —）

### 资产

- **10 语言文件补充** — locales 新增 `close` key；下载图标 / logo / 修复步骤图迁入 `public/`（commit —）

## 2026-09-01 — 官网全面升级（性能/交互/可访问性） + 版权页面重构

**升级(官网)：全面视觉/交互/性能优化（commit 434d4bd）**

### 优化

- **Google Fonts 动态注入** — 改为 JS 插入 `<link>` 标签加载，避免 `@import` 渲染阻塞（commit 434d4bd）
- **移动端 backdrop-filter 降级** — 导航栏/弹窗在700px以下禁用 `backdrop-filter`，减少合成层开销（commit 434d4bd）
- **移动端背景光晕降级** — `body::before/after` blur 80px→20px，禁用漂移动画（commit 434d4bd）
- **will-change 生命周期管理** — 滚动动画元素入场后移除 `will-change`，避免持续 GPU 占用（commit 434d4bd）

### 网站

- **Hero 打字机效果** — 循环逐字输入/删除4句标语，等待页面淡入结束后启动（commit 434d4bd）
- **Hero 鼠标聚光灯** — `mousemove` 驱动 `radial-gradient` 跟随光标（commit 434d4bd）
- **Feature 卡片光晕** — CSS `--glow-x/--glow-y` 变量 + `radial-gradient` 跟随鼠标（commit 434d4bd）
- **Feature 卡片 hover 多层阴影** — 青色外发光 + 抬起效果（commit 434d4bd）
- **数据亮点条** — 15语言/4平台/5格式三组数字，IntersectionObserver 触发滚动计数动画（commit 434d4bd）
- **按钮涟漪点击效果** — 点击时在鼠标位置生成白色涟漪波纹（commit 434d4bd）
- **Brand 彩蛋** — 连击导航品牌名5次触发 Emoji 雨动画（Web Animations API）（commit 434d4bd）
- **Lightbox 增强** — 支持滚轮缩放（以光标为中心）、拖拽平移、键盘方向键移动、+/- 缩放、0 重置、双击重置（commit 434d4bd）
- **页面入场淡入** — `body { opacity: 0 }` → `.loaded` 淡入 + 导航栏滑入 + 骨架屏微光（commit 434d4bd）
- **色板与直角风格** — 色板收窄为深邃蓝调，全站 `--radius-card: 0`（commit 434d4bd）
- **导航栏毛玻璃** — `backdrop-filter: blur(12px)` + 滚动时背景加深（commit 434d4bd）
- **社区分享区网格背景** — `background-image` 线性网格装饰（commit 434d4bd）

### 变更

- **跨平台描述统一** — 10 语言 `feature_cross_platform_desc`/`game_intro_desc2`/`game_intro_desc4` 统一为四大平台（Windows/Linux/macOS/Android）（commit 434d4bd）
- **子页面布局** — history/community/license 页 body padding 移至 `.main-wrapper`，页脚 `align-self: stretch`（commit 434d4bd）

### 资产

- **10 语言文件同步** — 新增 `stat_platforms`、`stat_formats`、`game_intro_desc1_short` 等 i18n key（commit 434d4bd）

---

**重构(官网)：版权页面迁入 html/copyright/ 子目录（commit b5fccc2）**

### 重构

- **版权页面目录化** — LICENSE.html、PROJECT_THIRDPARTY.html、THIRDPARTY_LICENSES.html 从 `docs/` 根目录移至 `docs/html/copyright/`，新增 `index.html` 入口页（commit b5fccc2）

---

## 2026-08-31 — 版本配置清理（删除26w33b/26w34b，26w34b→26w34b）

**构建(版本)：版本配置清理与版本码重排**

### 构建

- **app_version 重排** — 删除26w33b（版本码2）和原26w34b（版本码3），26w34b改名为26w34b并继承版本码2；版本序列收敛为26w33a(1)→26w34b(2)（commit 625f9d5）
- **android versionCode 同步** — `android/build.gradle` versionCode 4→2，与 app_version.json 对齐（commit 625f9d5）

**文档(版本)：PUBLISH.md 与 versions.json 同步清理**

### 文档

- **PUBLISH.md 条目合并** — 删除26w33b和原26w34b发布记录，26w34b改为26w34b并合并两版更新摘要（mac卡死修复/版本码判定/安卓BGM双播/动画回退等）（commit 625f9d5）
- **versions.json 条目合并** — versions key "4"→"2"，newest_version 4→2，删除版本"2"(26w33b)和"3"(原26w34b)，log合并为相对26w33a的完整更新说明（commit 625f9d5）

---

## 2026-08-29 — 清理已实现的设计方案 + 动画空壳化 + Manager 体系收敛

**文档(plans)：引擎分离架构设计方案（方案 C: Pragmatic Layered）**

### 文档

- **引擎分离设计文档** — 8 节完整设计：包结构与模块边界、Application 生命周期驱动、EventBus 统一（合并 EventQueue+EventDispatcher）、SceneManager+TransitionManager 协调、AssetManager 资产管理、UiManager 拆分、依赖管理与初始化顺序、InstanceContent 瘦身路径；含 5 阶段渐进迁移策略和 3D 扩展路径（commit 9d3a44a）

---

**修复(文本)：GameInfoMap 未注入 + 游戏会话 LanguageManager 未切换**

### 修复

- **GameInfoMap is null** — TextManager 删除后 `languageManager.setGameInfoManager()` 调用丢失，导致 `{game$key}` 文本模式全部返回 "GameInfoMap is null"；在 InstanceContent.init() 中 GameHost 创建后注入 GameInfoManager 到启动器 LanguageManager（commit 44f85d0）
- **游戏内文本显示原始 textKey** — JsonTextParser 静态 LanguageManager 启动时固定为启动器实例，游戏会话创建自己的 LanguageManager 后未切换；新增 JsonTextParser.setLanguageManager/getLanguageManager，GameSessionManager.enterGame() 中切换到游戏 LanguageManager 并注入 GameInfoManager，quitGame() 恢复启动器实例，enterGame() 错误路径均加回滚（commit 44f85d0）

---

**重构(动画)：TransitionManager 空壳化 + Action→Command 重命名**

### 重构

- **TransitionManager 空壳化** — 移除原有 1195 行动画执行逻辑（initFadeOut/fadingOut/initFadeIn/fadingIn/smooth_move 分发），保留双事件模型骨架（setPendingTask → executePendingTask）；startTransition 立即执行并恢复渲染管线，无动画过渡（commit 3952fb5）
- **Action→Command 重命名** — AnimationAction/AnimationActionParser/AnimationActionType/AnimationActionParam 及参数类全部重命名为 AnimationCommand 系列（commit 3952fb5）
- **AnimationObjectClass 枚举新增** — 提取动画对象类型枚举，替代原 AnimationAction 内嵌类型判断（commit 3952fb5）

### 移除

- **删除旧动画任务类** — AnimationAction.java、AnimationActionParser.java、AnimationActionType.java、AnimationActionParam.java、NoneAnimationActionParam.java、SmoothMoveAnimationActionParam.java（commit 3952fb5）

---

**资产(动画)：动画 JSON 配置格式适配 Action→Command**

### 资产

- **menu_main 动画重写** — fade_in 任务格式从 `synchronizationGraphics`/`schedule` + `smooth_move` 改为 `graphics` + `normal`/`none`，适配新的 AnimationCommand 体系（commit 26146aa）
- **config_basic/config_display/menu_list 精简** — 移除旧格式 fade_in/fade_out 空配置块（commit 26146aa）

---

**重构(架构)：Manager 体系收敛 — 24 个 Manager/Service 类分层整理**

### 重构

- **TextManager 合并到 LanguageManager** — TextManager 作为不必要的间接层被移除，文本解析（`{language$block#key}` / `{game$key}`）由 LanguageManager 直接承担；GameHost 构造参数 16→15；UiManager 创建改用 languageManager（commit c6ab54b）
- **ConfigService 提取** — GameResolver + GameUserConfigLoader 合并为统一的 ConfigService，负责启动器配置初始化链（UserConfig → Theme → Language）和游戏配置加载/销毁；GameHost/InstanceContent/GameSessionManager 同步适配（commit c6ab54b）
- **EngineContext 引入** — AudioManager + GraphicsManager + AnimationManager 打包为 EngineContext，PlayLocalData 持有单个 EngineContext 替代三个独立字段，支持整体传递和统一生命周期管理；GameResourceLoader 构造参数 9→5（commit c6ab54b）
- **LayoutManager → LayoutService 重命名** — 127 处引用跨 13 文件批量替换，语义从"管理器"转为"服务"（commit c6ab54b）
- **InitService 职责分离** — 从 Init 渲染机提取分帧初始化逻辑到独立的 InitService，Init 只保留视觉反馈；构造参数 10→5；InitService 在 lambda 内创建，每个 Init 实例拥有独立实例（commit c6ab54b）

### 移除

- **删除冗余类** — TextManager.java、GameResolver.java、GameUserConfigLoader.java、LayoutManager.java（commit c6ab54b）

### 新增

- **新增服务类** — ConfigService.java、EngineContext.java、LayoutService.java、InitService.java（commit c6ab54b）

### 资产

- **语言 JSON 格式规范化** — 16 个 language requirement.json 统一冒号前去空格（commit c6ab54b）
- **.gitignore** — 新增 `/develop/tool/` 排除本地开发工具目录（commit c6ab54b）

---

**文档(plans)：清理 develop/plans/ 下已实现的 6 篇设计方案文档**

### 文档

- **删除已实现方案** — 逐一比对代码库，确认以下 6 篇方案已全部落地后删除：`2026-08-09-animation-uikind-progress.md`（UiKind 重构 + 控件级动画配置）、`2026-08-09-uiobject-jsonentity-constructor.md`（UiObject JsonEntity 构造器）、`2026-08-13-beta-snapshot-version.md`（Beta 快照版本 YYwWWa）、`2026-08-13-mac-zip-delivery.md` + `2026-08-13-mac-zip-delivery-plan.md`（mac zip 内嵌安装器）、`2026-08-27-init-renderer-simplification.md`（InitService 职责分离）（commit af403ee）
- **同步索引** — `DOCUMENTATION_INDEX.md` 移除上述 6 篇的索引条目（commit af403ee）

---

## 2026-08-27 — Init 渲染机职责分离

**重构(init)：引入 InitService 提取初始化逻辑，Init 只保留视觉反馈**

### 重构

- **新增 InitService** — 从 Init.java 提取分帧初始化逻辑（6 个 stepXxx 方法）到独立的 `core/InitService.java`，Init 渲染机通过 `initService.stepInit()` 委托初始化（commit c6ab54b）
- **Init 构造函数简化** — 参数从 10 个减为 5 个（InitService、UpdateChecker、GraphicsManager、UiManager、EventQueue），直接依赖从 10 个减为 5 个（commit c6ab54b）
- **InstanceContent 注册更新** — Init 的渲染注册改为 lambda 内创建 InitService，每个 Init 实例拥有独立的 InitService 实例（commit c6ab54b）

---

## 2026-08-19 — 发布 v1.0.0-beta-26w34b + 打包器版本码自动递增 + mac 安装脚本友好提示

**构建(mac)：macOS 安装脚本添加 Gatekeeper 故障处理提示**

### 构建

- **安装脚本启动失败提示** — build_package_mac.py 安装器头脚本移除 `set -e` 避免非关键错误退出，安装完成后添加分隔线与明确提示：如应用未自动启动或提示「无法打开」，指引用户通过「系统设置 → 隐私与安全性 → 仍要打开」信任应用；添加「按回车退出」等待用户确认（commit edffdd3）

---

**构建(版本)：发布 v1.0.0-beta-26w34b**

### 构建

- **app_version 升级** — app_version 3→4，snapshot 26w34b→26w34b（commit <hash>）
- **android versionCode 升级** — versionCode 3→4（commit <hash>）
- **打包器版本码自动递增** — build_common.py input_version_interactive 版本码不再手动询问，改为 last+1 自动递增（commit <hash>）

**网站(版本)：官网新增 v1.0.0-beta-26w34b 下载入口**

### 网站

- **versions.json 新增 v26w34b 条目** — newest_version 升至 4，Windows/Android/Linux/mac Apple 芯片/mac Intel 五处蓝奏云下载链接更新；更新说明明确回退动画系统范围，保留安卓双播修复（commit <hash>）

---

## 2026-08-18 — type/key 分类重构 + 事件系统枚举化 + 动画系统实现 + 渲染机过渡接入 + 页面动画配置 + 其他小重构

**重构(keys)：type/key 常量类按领域分类到子包**

### 重构

- **type/key 分类整理** — 16 个 Key 常量类按领域拆到 `config/`（ConfigKey/RequirementKey/ThemeKey/LanguageKey/VersionKey/GameInfoKey）、`layout/`（LayoutKey/GraphicsKey/AudioKey）、`script/`（ScriptKey）、`story/`（StoryKey）、`ui/`（UiKey/UniversalUiKey）、`animation/`（AnimationKey）、`common/`（JsonKey/FileHandleKey/DialogKey），新增 `AudioKey`（audio 节独立），各使用点 import 路径同步更新（commit `a53ed85`）

**重构(事件)：事件系统枚举化 + EXECUTE 执行事件 + 联动转换**

### 重构

- **事件类型枚举化** — Event 接口与 EventAction 枚举取代原 Event 常量类，事件类 eventName 字段改 eventAction，EventDispatcher 改用枚举 switch（commit 3a3259c）
- **EXECUTE 执行事件** — 新增 Push/Pop/Set/ResetGameStateExecute 四类（淡出完成后入队真正切换状态栈）（commit 3a3259c）
- **GameStateEventAction 联动** — 状态操作事件子集枚举，EventAction ↔ GameStateEventAction 双向转换（commit 3a3259c）
- **PushGameStateInitSpecially** — Init 专用压栈事件，修复 eventAction 误用普通 push 导致的 ClassCastException（commit 3a3259c）
- **RecoverNormalRenderPipeLine** — 过渡结束恢复正常渲染事件（commit 3a3259c）
- **事件类重构** — 各事件类字段命名统一为 eventAction、import 顺序修正（commit 3a3259c）

**新增(动画)：页面切换淡入淡出动画执行引擎 + 控件预偏移**

### 新增

- **动画执行引擎** — TransitionManager 完整状态机（空壳事件→淡出→EXECUTE 切页→淡入→恢复），主循环逐帧驱动（commit 36b4adc）
- **控件预偏移** — 淡入开始前将 smooth_move 对象强制置到动画起点（原始坐标 - 偏移量），淡入插值从外部滑入原位，消除"控件先显示在终点再位移"的跳变（commit 36b4adc）
- **时间戳计时** — 淡入淡出改用时间戳计算已用时间，规避切页顿卡时 delta 突变导致动画跳帧/卡顿（commit 36b4adc）
- **AnimationComponentType 枚举** — 组件自报类型（getType），Animation 组件容器改 Map 存储（commit 36b4adc）
- **动画数据层完善** — Animation/组件/窗口/任务/动作全链 JsonEntity 构造补 null 防御、无效组件不入 Map、分层调试回溯日志（commit 36b4adc）

**重构(渲染)：渲染机抽象基类 + 页面过渡动画接入 + FADING 渲染管线状态**

### 重构

- **AbstractGameRender 基类** — 统一持有 gameStateDataContainer + 提供 getter，消除 9 个渲染机重复字段/init/getter（commit ce8757a）
- **渲染机过渡接入** — 各渲染机注入 AnimationManager，onInit 上缴动画配置，transitionRender 改为驱动 TransitionManager 淡入淡出（commit ce8757a）
- **GameRender 接口新增 transitionRender** — 渲染机过渡动画渲染入口（commit ce8757a）
- **RenderPipeline FADING 状态** — GameRenderPipeLineState 枚举 + RenderPipeline 按状态分流渲染过渡动画（commit ce8757a）
- **渲染机动画初始化** — Init/MenuMain/MenuList/MenuLoad/ConfigBasic/ConfigDisplay 各页面 setAnimation 接入（commit ce8757a）

**新增(配置): 各页面淡入淡出动画配置**

### 新增

- **menu_main 动画** — 4 按钮 schedule 串行滑入/滑出 + logo(title)/masker 同步位移，fade_in 右侧滑入、fade_out 左侧滑出（commit 1abebfa）
- **menu_list 动画** — 左栏 5 按钮右移入/左移出（commit 1abebfa）
- **menu_load/config_basic/config_display 动画** — 各自控件 smooth_move 切入切出配置（commit 1abebfa）
- **directory_structure 同步** — 新增页面 config 条目（commit 1abebfa）

**重构(keys)：type/key 分类后全量 import 路径适配**

### 重构

- **业务文件 import 同步** — script/data、ui、game、input、manager、util 等 69 个业务类的 type.key.* import 路径更新到子包（commit 096dc2c）

**重构(数据)：JsonEntity 布尔字段命名修正**

### 重构

- **isMap 字段去前缀** — JsonEntity 内部 isMap 字段改 map，getter 仍 `isMap()`，构造器赋值与 42 处读点同步（commit 55eff1f）

**重构(类型)：InitState 枚举化 + SceneStack 双 INIT 修复**

### 重构

- **InitState 枚举化** — 初始化阶段 int 常量改枚举（value/getValue/next），Init 状态机适配（commit fc4b279）
- **SceneStack 双 INIT** — 删除构造器栈底哨兵 INIT，保留 GameHost.init 显式 push，状态栈 `[INIT, INIT, MENU_MAIN]` 收敛为 `[INIT, MENU_MAIN]`（commit fc4b279）
- **GameState 补充** — state 相关补充字段（commit fc4b279）

**编码规范(字段)：布尔字段命名规范化**

### 编码规范

- **Main 懒加载字段** — isSlashed/isLazyInitialized 去前缀为 slashed/lazyInitialized，补 is/set 访问（commit 0cd9adc）
- **TextInputUtils 输入状态** — isInput 字段去前缀为 inputDone，补 setInput（commit 0cd9adc）

**新增(动画)：控件位置接口 + 游戏侧动画管理器注入**

### 新增

- **InteractableObject 位置接口** — 新增 getX/getY/setPosition，支撑 smooth_move 位移动画（commit 9510cd5）
- **游戏侧动画管理器** — GameResourceLoader 注入 EventQueue，创建游戏内 AnimationManager（commit 9510cd5）

**重构(事件)：loadGame 空壳压栈事件携带来源状态**

### 重构

- **GameSessionManager.loadGame** — 增加 nowGameState 参数，PushGameState 空壳事件携带 outState 供过渡链识别来源页（commit feb8561）

**重构(keys)：LayoutManager 使用 AudioKey（audio 节独立）**

### 重构

- **LayoutManager 关联调整** — backgroundMusic/music 字段改用独立 AudioKey，type/key import 适配（commit 0996d43）

**编码规范(ui)：UiManager 代码整理**

### 编码规范

- **CustomFont 类结构与注释整理** — UiManager 内部类与注释格式统一（commit 03ba284）

**修复(版本)：更新检测版本号显示修正**

### 修复

- **版本号显示日志** — doVersionDifferent/checkWebVersion 版本号拼接顺序与 displayVersionString 修正（commit b8cdca9）
- **type/key import 适配** — ConfigKey/VersionKey import 路径更新（commit b8cdca9）

**构建(版本)：发布 v1.0.0-beta-26w34b**

### 构建

- **app_version 升级** — app_version 2→3，snapshot 26w33b→26w34b（commit 5dbedb9）
- **android versionCode 升级** — versionCode 2→3（commit 5dbedb9）

**新增(文档)：官网版本数据维护规范 DOC_STANDARD + 官网日志修正**

### 文档

- **DOC_STANDARD.md 新增** — 官网 versions.json 日期时间格式（yyyy-MM-dd HH:mm）、更新日志只写用户可感知价值、跨版本修复补全原则、与其他文档联动（commit d485a18）
- **DOCUMENTATION_INDEX 更新** — 新增 DOC_STANDARD 条目（commit d485a18）
- **官网 v26w34b log 修正** — date 补完整时间，log 移除"控件预偏移"（内部必须非卖点）、补安卓背景音乐双播修复（commit d485a18）
- **PUBLISH 同步** — 发布 commit 更新为官网最终提交（commit d485a18）

**修复(网站)：游戏介绍折叠改为按 110px 高度判定**

### 网站

- **游戏介绍折叠改高度判定** — 不再按段落数判断折叠，改为测量内容实际高度超过 110px 才折叠；折叠态 `max-height:110px + 滚动条`，与更新日志折叠样式一致（commit a6a2313）

## 2026-08-16 — 默认语言改英文 + 目录结构补全 + 12 个新语言翻译包 + 首次运行按设备语言改写默认语言 + 设备语言自动检测与 15 语言扩展 + 键前缀/标签前缀常量注释补充动态段占位标注 + AnimationManager 一级服务化 + 显示配置下沉 UserConfigManager + 全屏切换职责迁移 + 崩溃处理职责下沉 CrashUtils + RenderPipeline 方法重命名区分注册与更新 + 全屏切换调用空值防御 + 官网卡片重排与折叠展开 + 官网导航新增遇到问题 + 官网语言扩展与识别修复

**新增(网站)：官网主页卡片重排与长文字折叠展开（游戏介绍 / 更新日志）**

### 网站

- **主页卡片重排** — 游戏介绍/资源下载/社区分享三个主卡片移至提示卡片之前，顺序为 logo → 游戏介绍 → 资源下载 → 社区分享 → 提示卡片 → 页脚（commit 69ee315）
- **游戏介绍折叠** — 长文本（第 3/4 段起）默认折叠，展开按钮按功能导向显示「展开全部/收起」（commit 69ee315）
- **更新日志折叠（主页 + 历史页）** — 日志超固定 110px 阈值才折叠，折叠态内可用滚动条浏览，展开按钮同游戏介绍（commit 69ee315）
- **折叠滚动条样式** — 暗色玻璃风滚动条（WebKit 渐变滑条 + Firefox scrollbar-color）（commit 69ee315）

---

**新增(网站)：官网顶部导航新增「遇到问题」入口**

### 网站

- **导航新增「遇到问题」** — 顶部导航在社区分享后新增「遇到问题」链接，锚点跳转 Watt Toolkit 提示卡片，配合 scroll-margin-top 避免被吸顶导航遮挡（commit 3abc966）
- **nav_trouble 多语言 key** — 9 个语言文件新增 nav_trouble 翻译（commit 3abc966）

---

**新增(网站)：官网语言扩展与设备语言识别修复**

### 网站

- **官网新增西班牙语语言包** — 新增 es.json，官网支持语言增至 10 种（commit ）
- **游戏介绍 15 语言文案** — desc2 改为列明游戏原生支持 15 种界面语言（简体中文/繁體中文/English/Español/Français/Português/Русский/日本語/한국어/Deutsch）（commit ）
- **设备语言识别修复** — getBrowserLang 由语言主干匹配改为完整 locale 小写优先精确匹配，zh-tw/zh-hk/zh-mo 正确映射繁体 zh-TW，修复繁体用户错显简体（commit ）

**变更(配置)：user_config 默认语言由简体中文改为英文**

### 变更

- **user_config.json 默认语言改 en_US** — 内部默认配置 language 字段由 zh_CN 改为 en_US，与设备语言检测默认值一致（commit ）

---

**修复(资产)：directory_structure 补全新语言目录与 page 配置条目**

### 修复

- **directory_structure.json 工具重新生成** — 补全 12 个新语言目录条目（含 language.json/requirement.json），并自动修复 config_basic/config_display/menu_main 三个 page 目录缺失的 config.json 条目（commit f680356）

---

**新增(资产)：language_config 注册 15 语言与 12 个新语言翻译包**

### 资产

- **language_config.json 注册 15 语言** — 全部语言名称统一为 `XXX语言·地区` 格式（如 `Français·France`），新增 zh_HK/en_GB/es_ES/es_MX/fr_FR/fr_CA/pt_BR/pt_PT/ru_RU/ja_JP/ko_KR/de_DE 12 个语言条目（commit 2c07f66）
- **12 个新语言目录翻译包** — 每个目录含 language.json（name/version/default/blocks 声明 requirement.json）与 requirement.json（message_box/menu.main/menu.list/menu.load/config.basic 全套 UI 翻译），键结构与 en_US 完全一致（commit 2c07f66）

---

**新增(功能)：UserConfigManager 首次运行按设备语言改写默认语言**

### 新增

- **UserConfigManager 接入设备语言检测** — parseJson 首读文件为空（首次运行）时走 `initUserConfig`：复制内部默认配置到外部后，按 `DeviceLanguage.detectDefault()` 改写 user_config 的 language 字段并落盘，用户可手动改；与默认语言相同时不写盘避免无谓写入（commit 2e1c653）

---

**新增(类型)：DeviceLanguage 设备语言检测枚举与 15 语言路径/显示名常量**

### 类型

- **FileName 语言路径常量全覆盖 15 语言** — 新增 12 个语言路径常量（zh_HK/en_GB/es_ES/es_MX/fr_FR/fr_CA/pt_BR/pt_PT/ru_RU/ja_JP/ko_KR/de_DE），zh_CN/zh_TW/en_US 命名化为常量，DEFAULT_LANGUAGE_PATH 由 zh_CN 改为 en_US（commit 88c5259）
- **Name 语言显示名常量全覆盖 15 语言** — 新增 12 个语言显示名常量（`XXX语言·地区` 格式，如 `Français·France`），DEFAULT_LANGUAGE_NAME 改为 English·United States（commit 88c5259）
- **DeviceLanguage 设备语言检测枚举** — 15 个语言枚举（语言_地区 大写目录名），`fromLocale` 按语言+地区精确匹配、未命中回退该语言默认地区、不支持语言回退英文，`detectDefault` 基于 `Locale.getDefault()` 四平台通用（commit 88c5259）

---

**编码规范(注释)：键前缀/标签前缀常量注释补充动态段占位标注**

### 编码规范

- **UiManager 标准标签前缀** — `getImageStandardTag`/`getLabelStandardTag`/`getButtonStandardTag` Javadoc 补充完整标签形态 `ui.image.<imageTag>`/`ui.label.<labelTag>`/`ui.button.<buttonTag>`（commit 81c2843）
- **UiManager Pixmap 合并前缀常量** — `PIXMAP_IMAGE`/`PIXMAP_LABEL`/`PIXMAP_BUTTON` 注释补充完整键形态 `IMG_<imageKindName>`/`LB_<labelKindName>`/`BT_<buttonKindName>_up|_down|_disabled`（commit 81c2843）
- **FileName 崩溃日志前缀** — `CRASH_LOG` 注释补充完整键形态 `crash-<时间戳>`（commit 81c2843）
- **三个 Manager 标准日志标签补 Javadoc** — `ImageManager`/`LabelManager`/`ButtonManager` 的 `getImageStandardTag`/`getLabelStandardTag`/`getButtonStandardTag` 标注前缀 `[Image] <imageTag>`/`[Label] <labelTag>`/`[Button] <buttonTag>`（commit 81c2843）

---

**新增(服务)：AnimationManager 升级为与 Audio/Graphics/Ui 同级的一级服务并接入游戏侧 gameAnimationManager**

### 新增

- **AnimationManager 空壳类** — 动画执行管理器骨架，承担页面切换控件级动画（fade_in/fade_out）执行与渲染机衔接，当前仅占位生命周期接口（init/update/dispose），执行逻辑待设计实现（commit e0a15e5）
- **InstanceContent 接入动画管理器** — 新增 `animationManager` 字段（与 audio/graphics/ui 同级）、init 创建实例、`getAnimationManager()` getter、dispose 销毁；不主动每帧驱动动画，推进时机由 AnimationManager 内部自行安排（commit e0a15e5）
- **PlayLocalData 持有动画管理器** — 新增 `animationManager` 字段 + `getAnimationManager()`/`setAnimationManager()`（模仿 audioManager 模式）、dispose 销毁（commit e0a15e5）
- **GameResourceLoader 创建游戏侧动画管理器** — loadResource 在 script 之后创建 `gameAnimationManager`（init + setAnimationManager），disposeResource 反向先销毁动画（commit e0a15e5）

---

**重构(配置)：initLibGDX 显示配置逻辑下沉为 UserConfigManager 静态特例函数**

### 重构

- **UserConfigManager 新增静态特例函数 `initDisplayConfig`** — 把 Main.initLibGDX 里读取 user_config、应用分辨率、确定视窗的三段逻辑下沉为 UserConfigManager 静态方法（返回 UseViewport）；语义保留：配置优先应用、桌面首次启动按屏幕 80%/16:9 检测分辨率并写仅含 resolution 小配置（由 UpdateChecker protect 机制与内部默认配置合并）、视窗按平台默认（desktop→stretch / android→fit）（commit 44e1bb6）
- **Main.initLibGDX 简化调用** — 原手写文件读取/JSON 判断替换为一行 `useViewport = UserConfigManager.initDisplayConfig()`，删除搬走后失效的 import（JsonEntity/ConfigKey/PlatformUtils/HashMap/Map）（commit 44e1bb6）

---

**重构(配置)：全屏切换职责从 Main 迁移到 UserConfigManager**

### 重构

- **UserConfigManager 新增 `toggleFullscreen`** — 承接全屏/窗口模式切换，进入全屏前记忆窗口化尺寸（windowedWidth/Height 字段），退出时恢复；全屏状态以 `Gdx.graphics.isFullscreen()` 实际窗口为准，不再依赖字段记忆，避免与 fullscreen 配置字段漂移（commit 5cc6979）
- **Main 移除全屏职责** — 删除 `toggleFullscreen` 静态方法与 windowedWidth/windowedHeight/isFullscreen 三个静态字段，Main 不再承担窗口状态职责（commit 5cc6979）
- **UniversalInputHandlerFunction 改走 UserConfigManager** — `Main.toggleFullscreen()` 改为 `InstanceContent.getInstance().getUserConfigManager().toggleFullscreen()`（commit 5cc6979）

---

**重构(崩溃处理)：safeCrash 安全崩溃兜底从 Main 下沉到 CrashUtils**

### 重构

- **CrashUtils 新增静态 `safeCrash`** — 安全崩溃处理入口，先尝试 `crash(Throwable)`，失败时退化为 System.err 打印 + 抛 RuntimeException，避免崩溃处理自身不可用（类加载失败、弹窗组件异常等）导致原始崩溃信息丢失；崩溃处理职责完整收拢到 CrashUtils（commit 2152719）
- **Main 移除崩溃处理实现** — 删除私有 `safeCrash` 方法与 5 处调用改为 `CrashUtils.safeCrash(...)`，Main 只负责调用崩溃处理，不再自行实现（commit 2152719）

---

**重构(渲染)：RenderPipeline 方法重命名区分注册与每帧更新**

### 重构

- **RenderPipeline 方法职责拆分命名** — 原 `update(GameStateDataContainer)`（按状态从注册表获取并初始化渲染机）改名 `register(...)`，原 `updateFrame(float)`（每帧调用渲染机更新与输入处理）改名 `update(...)`，方法名与职责对齐，消除"更新"一词歧义（commit ac5ec56）
- **调用方同步更新** — `GameHost.run` 的 `renderPipeline.updateFrame(deltaTime)` 改为 `renderPipeline.update(deltaTime)`；`SceneStack.updateGameRender` 的 `renderPipeline.update(...)` 改为 `renderPipeline.register(...)`（commit ac5ec56）

---

**优化(输入)：全屏切换调用增加 InstanceContent 空值防御**

### 优化

- **UniversalInputHandlerFunction 空值防御** — `handleToggleFullscreen` 中 `InstanceContent.getInstance()` 以 `Objects.requireNonNull` 包裹，防止单例未初始化时调用空指针（commit 53ee6fb）

---

**修复(音频)：背景音乐随机切换偶发双播修复**

### 修复

- **loadBackgroundMusic 自然播完回调先 stop 再移除播放记录** — 背景音乐随机切换时偶发双播：曲目自然播完瞬间 isPlaying() 已返回 false 但播放记录尚未移除（libGDX AndroidMusic 将 completion 回调 postRunnable 延迟到下一帧），playLayout 会误将其 resume（Android start() 从头播放），随后记录被移除、下一帧随机新曲导致两曲同播；completion 回调内改为先 m.stop() 兜底再移除记录，保证单曲（commit <hash>）

## 2026-08-15 — 动画数据层落地 + Javadoc 性能标注规范 + 热路径 Javadoc 性能标注 + 官网下载区优化（恢复提取码 / 首页卡片化 / 更新时间）+ 26w33a mac Intel 提取码文案补删 + GameState 扁平化重构 + UiKey 嵌套类化

**优化(Javadoc)：热路径方法补充性能表现与适用场景标注**

### 优化

- **主循环链路性能标注** — `GameHost.run`（每帧热路径入口，帧耗时决定帧率下限）、`RenderPipeline.updateFrame`/`render`（每帧更新/绘制入口）、`InstanceContent.update`（每帧输入聚合点）、`SceneStack.updateGameState`/`loadGameLayout`/`loadGameConfig`（切换时同步 IO + JSON 解析，一次性高开销）、`EventDispatcher.handleEvent`（事件分发 switch + 日志拼接）（commit <hash>）
- **输入处理性能标注** — `VirtualInputHandler` 的 update/getLists/refreshInteractableObjectMap（每帧，HashSet 相等性判断 + 全量 isShown + stage.hit）、`ControllerInputHandler.update`（每帧摇杆轮询）（commit <hash>）
- **场景渲染性能标注** — `GamePlay` 的 update/render/localHostUpdate（每帧 + 任务栈 map 查询）、`MenuMain` 的 update/render（每帧 4×isButtonClicked + 4×handleAsk）、`ScriptExecutor.update`（每帧 new ArrayList + shuffle + 指令数上限）（commit <hash>）
- **资源与 UI 工具性能标注** — `UiManager` 查找类方法（O(1) map 查询，动画应缓存引用）、`GraphicsManager` 纹理与绘制方法（缓存命中 O(1) vs 未命中解码、putGif 每帧帧推进）、`MessageBox.showAsk`/`handleAsk`、`LogUtils` 时间格式化与同步 IO（commit <hash>）

---

**编码规范(注释)：耗性能方法 Javadoc 必须标注性能表现与适用场景**

### 编码规范

- **CLAUDE.md 新增性能标注规范** — 耗性能的方法（每帧调用、频繁查找、复杂计算等）Javadoc 必须标注性能表现与适用场景，供调用者判断是否适合在每帧/热路径使用（commit 8d2f30f）
- **CODING_STYLE.md「13. 注释」补充性能标注规范** — 同规则 + 正例（findObject Javadoc 含 `<p>` 性能段落）与反例（缺少性能说明，调用者无法判断能否在每帧循环使用）示例（commit 8d2f30f）

---

**动画(数据层)：动画数据模型与配置解析落地**

### 新增

- **动画容器 `Animation`** — 对应 config 的 `animation` 节点 `{fade_in, fade_out}`，持有整组动画组件；字段构造 + JsonEntity 构造双构造器，携带 valid 与 json，解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该容器（commit b6864a5）
- **`AnimationComponent` 接口 + FadeIn/FadeOut 组件** — `animation/component` 包：组件接口统一暴露有效性判断与构造来源 JSON；`FadeIn`/`FadeOut` 各自持有通用动画窗口（default）与来源页特例窗口（from_page.xxx）两组对象，`getObject(state)` 特例优先、无特例回退通用窗口（commit b6864a5）
- **`FadeInObject`/`FadeOutObject` 动画窗口对象** — 持有整段窗口时长（duration）与同步/串行两组任务（synchronization 同时启动、schedule 按序执行），字段构造 + JsonEntity 构造双构造器，任务解析 fail-soft（commit b6864a5）
- **`AnimationTask` 动画任务信封** — 持有动画目标（`AnimationObject`）与动画动作（`AnimationAction`），目标解析 fail-fast 捕获降级、动作解析 fail-soft，双构造器（commit b6864a5）
- **`AnimationAction` 动作信封 + 参数体系** — `animation/task/action` 包：动作持有类型（`AnimationActionType`：none/smooth_move）+ 相对延迟 + 参数对象，构造时按类型校验参数实现类匹配；`AnimationActionParser` 解析器 + `AnimationActionParam`/`NoneAnimationActionParam`/`SmoothMoveAnimationActionParam` 参数类（commit b6864a5）
- **`AnimationKey` 嵌套键常量** — 嵌套类按 `animation/` 包层级组织：`Component`（FadeIn/FadeOut 节点与窗口键）、`Task`（object/action）、`Task.Object`（class 类别）、`Task.Action`（type/delay/param + none/smooth_move 值）、`Task.Action.Param`（orientation/speed/duration）（commit b6864a5）

### 重构

- **`AnimationObject` 目标键迁入 `AnimationKey.Task.Object`** — `AnimationObject`/`UiAnimationObject`/`GraphicsAnimationObject` 的 class 类别键由 `AnimationKey.Target` 迁入 `AnimationKey.Task.Object` 嵌套类（commit b6864a5）
- **`ControlScriptCommand` 动作参数表泛型收紧** — `Map<ScriptCommandAction, Class<?>>` 收紧为 `Map<ScriptCommandAction, Class<? extends ScriptCommandParam>>`，编译期校验参数类型（commit b6864a5）

---

**重构(键结构)：RequirementKey.Ui 内部嵌套类化**

### 重构

- **Ui 键扁平常量改为嵌套类** — `RequirementKey.Ui` 下 `MENU_MAIN_*`/`MENU_LIST_*`/`CONFIG_BASIC_*`/`GAME_MENU_*` 扁平常量重组为 `MenuMain`/`MenuList`/`ConfigBasic`/`GameMenu` 内部嵌套类，`MenuMain`/`GameMenu`/`MenuList`/`ConfigBasic` 页面类引用同步更新（commit 432b422）
- **配置 JSON 同步** — `config_basic/layout.json` 移除语言选中相关项、`menu_main/config.json` 同步调整（commit 432b422）

---

**重构(状态系统)：GameState 扁平化，消除主次分层**

### 重构

- **GameState 扁平枚举化** — 原 `GameState`（int 主状态）+ `GameSubState`（int 子状态）+ `StateStructure`（双 int 数据类）+ `GameStatePageInfo`（两层 Map）整合为单一扁平枚举 `type/game/GameState`，枚举值即完整可流转状态，携带布局目录名/是否需要配置/是否游戏内/中文显示名元数据；主状态本身无独立页面，真正可流转 9 个叶子状态；删除全项目零引用的 SAVE/EDIT 死代码（commit bbebb21）
- **状态流转调用点全面改造** — `SceneStack` 改 `Stack<GameState>`、`GameRenderRegistry` 改 `Map<GameState, Supplier>` 枚举键、`PushGameState`/`SetGameState` 事件携带枚举、`GameStateDataContainer` 持有枚举、渲染机注册改用枚举、页面类事件参数同步更新（commit bbebb21）
- **修复 CONFIG_DISPLAY 休眠缺陷** — `InstanceContent` 补注册 `ConfigDisplay` 渲染机（此前无注册，推到该状态会崩溃）（commit bbebb21）
- **CODING_STYLE 同步** — 移除已删除的 `StateStructure`/`getGameStateName` 示例引用，改用现存类（commit bbebb21）

---

**网站(更新时间)：版本卡显示更新时间（精确到分钟）**（commit e3b7bb3）

### 网站

- **versions.json 各版本新增 date 字段** — 版本条目补充发布时间戳（`YYYY-MM-DD HH:MM`，与 PUBLISH.md 发布时间一致：26w33b=2026-08-14 23:51、26w33a=2026-08-13 17:56）（commit e3b7bb3）
- **历史版本页/首页版本卡显示更新时间** — `docs/js/history.js`/`docs/js/main.js` 在版本名下方一行小字显示「更新时间：时间戳」（date 为空则不显示），`docs/css/history.css`/`docs/css/index.css` 配套样式；9 语言 locale 新增 `update_time` 键（commit e3b7bb3）

---

**网站(首页)：资源下载区照搬历史版本卡片排版**（commit b526abe）

### 网站

- **首页资源下载改为整卡渲染** — `docs/index.html` 下载区改为空容器，`docs/js/main.js` 按历史版本页同款结构整卡渲染最新版本（版本名 + 最新标签 + 更新日志 + 四平台下载按钮），`docs/css/index.css` 引入 version-card 卡片样式并补充 error-message（commit b526abe）

---

**网站(下载)：恢复蓝奏云提取码 1698（撤销测试版免密）**（commit f25998f）

### 网站

- **全版本下载描述恢复「提取码 1698」** — `docs/data/versions.json` 10 条下载描述（26w33a/26w33b × Windows/Android/Linux/mac Apple 芯片/mac Intel）补回「· 提取码 1698 ·」；26w33b `log` 移除「测试版下载链接不再需要访问提取码，下载更省心」条目（蓝奏云非会员只能使用访问密码，撤销此前免密决定）（commit f25998f）
- **PUBLISH.md 26w33b 摘要移除免密说明** — 面向玩家摘要删除「测试版下载链接不再需要访问提取码，下载更省心」（commit f25998f）

---

**重构(网站/version.json)：补删 26w33a mac Intel 遗留提取码文案**

### 重构

- **26w33a mac Intel 描述补删遗留「提取码 1698」** — `docs/data/versions.json` 旧版 `versions."1"`（26w33a）mac Intel 条目描述补删上批（commit e0bd53e）遗漏的「提取码 1698」；蓝奏云旧文件已改为免密，链接地址不变（commit cafa0c4）

---

## 2026-08-14 — 游戏 launcher_version 强制改为整型版本码 + 发布 v1.0.0-beta-26w33b

**文档(规范)：新增发布说明书写规则**（commit a28ad48）

### 文档

- **CHANGELOG.md 新增规则 11 / PUBLISH.md 新增规则 5** — 写版本发布说明须按上一版本发布点 → 本版本发布点之间的 commit 区间梳理玩家可见改动，跨版本生效的修复须算入下一版本说明；beta 按此梳理书写，release 正式版需正式、系统书写（commit a28ad48）

---

**文档(发布)：按 commit 区间细化 26w33b 发布说明**（commit 58124e4）

### 文档

- **26w33b 更新说明补全为 commit 区间梳理** — `docs/data/versions.json` 版本 2 的 `log` 与 `PUBLISH.md` 面向玩家摘要，按 26w33a 发布点（`adf2635`）→ 26w33b 发布点（`9500e9d`）之间的 commit 区间梳理，补入区间内完成的 mac 卡死修复（点击路径打开资源管理器 / 系统原生弹窗）与 mac 安装包引导改进（commit 58124e4）

---

**版本(beta)：26w33b 版本号写入打包文件**（commit f00949c）

### 变更

- **app_version.json 升至版本码 2（快照 26w33b）** — `assets/asset/app_version.json` 的 `app_version` 由 `1` → `2`、`app_version_snapshot` 由 `26w33a` → `26w33b`（26w33b 打包写入；`app_version_string` 保持 `1.0.0`、`app_version_type` 保持 `0`=beta）（commit f00949c）
- **Android versionCode 升至 2** — `android/build.gradle` 的 `versionCode` 由 `1` → `2`（`versionName` 保持 `"1.0.0"`，与版本码递增同步；`gradle.properties` 的 `projectVersion` 保持 `1.0.0` 无需改动）（commit f00949c）

---

**文档(发布)：新增 v1.0.0-beta-26w33b 发布记录**（commit f76adf0）

### 文档

- **PUBLISH.md 新增 v1.0.0-beta-26w33b 发布记录** — 发布信息：发布时间戳 = versions.json 提交 `9500e9d` 时间（2026-08-14 23:51），最后一次更新 `9500e9d`；面向玩家摘要细化（版本判定升级 / 修复误弹「版本不一致」 / 新增「无法识别游戏版本要求」提示 / 测试版下载免密）（commit f76adf0）

---

**网站(beta)：下载区更新为 26w33b 免密链接**（commit 9500e9d）

### 网站

- **versions.json 新增 v1.0.0-beta-26w33b 条目** — 新增 `versions."2"`（newest_version=2 / 26w33b），Windows、Android、Linux、mac Apple 芯片、mac Intel 五处蓝奏云下载链接全部更新为 26w33b 免密地址（commit 9500e9d）
- **测试版下载取消访问提取码** — 按新策略测试版链接不再设置访问密码，26w33b 五条描述移除「提取码 1698」文案；旧版 26w33a 链接（上传时仍设密码）描述保留提取码（commit 9500e9d）
- **26w33b 更新摘要细化** — 版本 `log` 字段改写为详细更新说明（版本判定升级 / 修复误弹「版本不一致」/ 新增「无法识别游戏版本要求」提示 / 测试版下载免密）（commit 9500e9d）

---

**变更(AppVersionTable)：追加版本码 2 → "1.0.0" 映射 + 打包 README 同步**（commit bc61f3a）

### 变更

- **AppVersionTable 追加版本码 2 映射** — `type/AppVersionTable` 新增 `2 → "1.0.0"`（26w33b 发布，`app_version` 递增至 2；发版手动维护，`build_package.py` 不自动同步）（commit bc61f3a）
- **打包 README 补充 AppVersionTable 发版维护说明** — `develop/output/README.md`「版本管理体系」新增小节，说明版本码 → 版本字符串对照表及其发版手动维护要求（commit bc61f3a）

---

**文档(CLAUDE.md)：新增非项目更新不落盘约定**（commit 405209b）

### 文档

- **CLAUDE.md 新增「非项目更新/项目外创作不写入工作区」约定** — 项目外创作（如外部小说改编游戏）的决策与素材只记入 Claude 持久记忆，不写入本项目工作区任何文件（含 `temp/CLAUDE_MEMORY.md`、`develop/`、`CLAUDE.md` 等）；确需落盘时先征求用户同意（commit 405209b）

---

**文档(JSON_STANDARD)：game.json 章节补全实际字段**（commit a66e6d8）

### 文档

- **JSON_STANDARD.md 5.1 补全实际字段** — game.json 表格补齐 `id`/`name`/`version`/`launcher_version`，标注 `launcher_version` 为整型版本码（对应启动器 `app_version`）、`version` 为字符串（第三方创作者自定，启动器不判断不干涉）（commit a66e6d8）

---

**变更(launcher_version)：judgeGame minor 判定与未知码弹窗**（commit 78d7f8a）

### 变更

- **判定保留 minor 兼容语义** — `MenuList.judgeGame()` 经 `AppVersionTable` 还原版本码为字符串后走 `doMinorCompatible(String)`（主版本相等 + 次版本 ≥，保持原语义）；未知版本码或缺字段（还原为 null）→ 弹「无法识别游戏版本要求」弹窗，建议不要强制运行（`UpdateChecker.doAppVersionCompatible` 无调用者，已删除）（commit 78d7f8a）
- **三语言新增「无法识别游戏版本要求」弹窗文案** — `RequirementKey.Language.MessageBox` 新增 `GAME_LAUNCHER_VERSION_UNKNOWN` 键，zh_CN/zh_TW/en_US 的 `requirement.json` 补 `game_launcher_version_unknown.title/content.1/content.2`（commit 78d7f8a）

---

**重构(launcher_version)：launcher_version 解析与存储链 int 化**（commit 94c8600）

### 重构

- **游戏 `launcher_version` 值改为整型版本码** — `game/<id>/game.json` 的 `launcher_version` 由字符串 `"1.0.0"` 改为整型码 `1`（对应当前启动器 `app_version` 码），与启动器版本码体系对齐（commit 94c8600）
- **解析返回值 String→int** — `GameLogicService.parseGameLauncherVersion()` 由 `getString` 改为 `getInt`，缺字段返回 `-1`（不再打印 error，视为未声明，由消费端决定处理）（commit 94c8600）
- **存储链类型统一** — `Player`/`EnterGame`/`GameSessionManager` 中 `gameLauncherVersion` 类型由 String 统一改为 int（commit 94c8600）
- **新增 `type/AppVersionTable` 对照表** — 版本码 → 版本字符串映射（当前 `1 → "1.0.0"`），供 `MenuList.judgeGame()` 还原版本码后做 minor 兼容判断，发版时手动维护（commit 94c8600）

---

## 2026-08-13 — mac 死锁修复 + 官网深色主题美化 + 测试版发布与快照细分 + 打包重构 + 版权素材清理

**文档(发布)：建立版本发布记录体系 + CHANGELOG 条目绑定提交哈希**（commit 10d6d92）

### 文档

- **新增 `develop/PUBLISH.md` 版本发布记录** — 记录每次发布版本（beta/正式版）的版本号、版本类型、发布时间戳（= 最后一次更新版本下载地址 `docs/data/versions.json` 的 git 提交时间）、该版本最后一次更新 commit、面向玩家的 bug 修复/新增功能摘要；并写入 v1.0.0-beta-26w33a 发布记录（发布时间 2026-08-13，最后一次更新 `adf2635`）（commit 10d6d92）
- **CHANGELOG 头部新增规则 9/10** — 规则 9：每次发布新版本须在 PUBLISH.md 记录发布信息与时间戳；规则 10：同一天更新归组到同一日期标题、条目绑定引入它的 git 提交短哈希（commit 10d6d92）
- **回填 CHANGELOG 全部条目 git 提交短哈希** — 138 个提交块按日期归组为 49 个日期（一天一个 `## <日期>` 标题），673 条条目行尾标注引入它的 commit 短哈希（commit 10d6d92）
- **修订 v1.0.0-beta-26w33a 发布摘要** — `develop/PUBLISH.md` 面向玩家摘要移除发布边界（最后一次更新下载地址提交 `adf2635`）之后才完成的 mac 死锁修复（`b742781`/`c7528e8`），摘要仅保留该版本发布时已上线的官网功能，发布时间戳与「该版本最后一次更新 `adf2635`」不变（commit b58ae51）

---

**修复(mac)：原生弹窗改用 osascript 避免 AWT 死锁**（commit c7528e8）

### 修复

- **macOS 原生弹窗改用 osascript（非 AWT）** — 新增 `lwjgl3/imp/MacOsAppleScriptDialog.java`（AppleScript `display dialog` 实现），`Lwjgl3Launcher` macOS 分支注入替换 Swing `JOptionPane`（含崩溃弹窗），消除同类 AWT 与 LWJGL3 主线程死锁隐患；其余平台保持 Swing 实现不变（commit c7528e8）

---

**修复(mac)：点击路径打开资源管理器死锁**（commit b742781）

### 修复

- **macOS 点击路径标签打开资源管理器不再死锁** — `lwjgl3/imp/DesktopExplorerOpener.java` macOS 上完全绕开 `java.awt.Desktop`（AWT 与 LWJGL3 抢占主线程会死锁卡死游戏），改走 CLI `open` 命令；进程启动移入后台守护线程消费输出，不再阻塞 GL 线程（Windows/Linux 保持 AWT 优先 + CLI 回退）（commit b742781）

---

**官网(新增)：首页反馈卡片与页脚邮件反馈入口**（commit fada51c）

### 网站

- **首页新增邮件反馈卡片** — `docs/index.html` 提示卡片区新增第三张反馈卡片（反馈文案 + 可点击邮箱 + 发送邮件按钮，指向 mailto:brainleech198@foxmail.com），9 语言 i18n 同步新增 5 个键（tip_feedback_title / tip_feedback_text / tip_feedback_button / copy_button / copied_feedback）（commit fada51c）
- **页脚新增邮箱显示与复制** — `docs/index.html`、`docs/html/history_versions.html`、`docs/html/community_share.html` 三个页面页脚品牌列新增邮箱链接与「复制」按钮；`docs/js/common.js` 新增复制逻辑（clipboard API + execCommand 兜底，复制后按钮短暂提示「已复制」）；`docs/css/common.css`、`docs/css/index.css` 新增配套样式（commit fada51c）
- **官网 README 补充邮件反馈渠道** — `docs/README.md` 说明段新增邮件反馈条目（commit fada51c）

---

**网站(beta)：下载区五个平台链接换新**（commit adf2635）

### 网站

- **下载区五个平台链接换新** — `docs/data/versions.json` Windows、Android、Linux、mac Apple 芯片版、mac Intel 版五处蓝奏云下载链接全部替换为新地址（提取码 1698 不变）（commit adf2635）

---

**网站(版权)：官网维修步骤示意图换为游戏运行截图**（commit f407091）

### 网站

- **官网维修步骤示意图换为游戏运行截图** — `docs/resource/image/repair1.png`、`repair2.png` 由第三方素材替换为游戏运行截图（步骤一维修标识 / 步骤二确定退出），`docs/THIRDPARTY_LICENSES.html` 登记「原创」不变（commit f407091）

---

**资产(版权)：主菜单/启动背景图换为自制素材**（commit 62004cb）

### 资产

- **主菜单/启动背景图换为自制素材** — `menu.background.png`、`app_init.png` 由豆包AI第三方素材替换为自制素材（AI 生成 + 手工二次设计），`assets/THIRDPARTY_LICENSES.md` 及官网 `docs/PROJECT_THIRDPARTY.html` 相应条目由「1.1 AI 生成」移入「三、原创素材」（commit 62004cb）

---

**官网(修复)：许可证页 CC 联系链接超链接化**（commit 84371bd）

### 网站

- **许可证页 CC 联系链接超链接化** — `docs/LICENSE.html` 简/繁/EN 三个 tab 末尾的 Creative Commons 联系入口由纯文本改为指向 https://creativecommons.org/ 的超链接（新窗口打开），与 CC 官方模板一致（commit 84371bd）

---

**文档(清理)：删除已实施完成的官网美化设计文档**（commit a919a88）

### 文档

- **移除已落地方案的陈旧文档** — 官网玻璃拟态美化方案已全量实施，删除 `develop/plans/2026-08-13-website-beautify.md` 设计文档（原未登记 DOCUMENTATION_INDEX）（commit a919a88）

---

**文档(计划)：mac zip 交付设计记录与实施计划**（commit d35da2f）

### 文档

- **mac zip 交付设计记录** — `develop/plans/2026-08-13-mac-zip-delivery.md` 记录 mac 交付物从「裸 .app zip + .command」调整为「zip 内嵌自解压 .command + 中文 symlink」的设计（蓝奏云扩展名白名单 / 无 macOS 环境 / 命名定案）（commit d35da2f）
- **mac zip 交付实施计划** — `develop/plans/2026-08-13-mac-zip-delivery-plan.md` 逐任务拆解 `build_package_mac.py` 改造与一次性补救脚本的实施步骤（commit d35da2f）

---

**mac 交付物改 zip 内嵌安装器 + 产物命名更直观**（commit 65433a7）

### 构建

- **mac 交付物改 zip 内嵌安装器** — 蓝奏云不接受 `.command` 直传，`build_package_mac.py` 改为把自解压 `.command` 压成 zip（zip 内置 `Install QingFeng.command` + 「安装氢风」中文 symlink，`zipfile` 写 `external_attr` 保留可执行位，Windows 交叉编译亦可）；移除裸 `.app` zip，独立 `.command` 保留（AirDrop 免确认）；新增一次性补救脚本 `repack_mac_command_zip.py` 批量转换现有产物（commit 65433a7）
- **mac 产物命名** — `build_package_mac.py` 产物文件名架构标签由 `mac`/`macM1` 改为 `mac_apple_silicon`/`mac_intel`（zip / .command / .dmg 同步），Apple Silicon（M 芯片）与 Intel 一目了然；打包时打印「目标架构 → 产物前缀」便于核对；`develop/output/README.md` 产物表按双架构拆分列出，并同步 `CONTRIBUTING.md` 输出成品表（顺手修正过时的 `tar.gz` 记录）（commit 65433a7）

---

**文档(计划)：beta 快照版本方案更新**（commit b1bde6a）

### 文档

- **beta 快照版本方案更新** — 明确更新检测在整型相同时按日期码（快照码）细分新旧；官网下载区两阶段弹窗（先选来源再列描述+链接）与 `versions.json` 数据结构映射说明（commit b1bde6a）

---

**官网(修复)：历史版本页补 mac 下载按钮**（commit c9fbcff）

### 网站

- **历史页补 mac 下载按钮** — `history.js` 版本卡下载行补上 Mac 下载按钮（`download-mac.png` + `data-platform="mac"`），`history.css` 下载网格由 3 列扩为 4 列并在 700px 断点降为 2 列；mac 的 Apple/Intel 双来源由弹窗来源选择展示（commit c9fbcff）

---

**官网(beta)：下载区接入蓝奏云真实链接**（commit c7975e1）

### 网站

- **官网下载区接入真实链接** — `versions.json` 下载区替换示例链接为蓝奏云真实地址（含提取码 1698），Windows/Android/Linux/mac（含 Intel 双来源）各一处，描述补充安装指引；顶层新增 `newest_version_snapshot` 字段供更新检测按快照码判断（commit c7975e1）

---

**版本(beta)：测试版更新弹窗 + 版本切 beta 快照**（commit e6e5180）

### 功能

- **测试版更新弹窗** — 检测到测试版更新时，弹窗标题/正文改用「发现测试版更新」并提示可能不稳定（`MenuMain` 按 `isNewestVersionBeta()` 切换文案 key，requirement.json 三语言新增 `update_detected_beta.*`）（commit e6e5180）
- **版本切 beta 快照** — `app_version.json` 版本类型由 release 切为 beta、快照码置 `26w33a`（commit e6e5180）

---

**版本(beta)：更新检测整型相同时按快照码细分新旧**（commit a309e32）

### 功能

- **beta 快照码更新判断** — 更新检测主判断整型版本号相等时，双方均为测试版则再按快照码（`YYwWWa` 定宽字典序即时间序）细分新旧，快照码升级判定为可更新；`VersionKey` 新增 `NEWEST_VERSION_SNAPSHOT`、`VersionType` 新增 `UNKNOWN`，`UpdateChecker` 记录远程版本类型并暴露 `isNewestVersionBeta()`（commit a309e32）

---

**官网：深色主题配色微调（卡片暖墨分层 + 页脚下划线生长修复）**（commit c551263）

### 网站

- **卡片暖墨分层** — 深青背景下的卡片由同色系青色改为中性暖墨灰（`--card-bg` 等色板变量收敛到 common.css），hover/下载标签/hero 分层暖墨，青色只作强调色（边框/按钮/链接/光晕），背景青与卡片墨形成冷暖对比（commit c551263）
- **页脚下划线生长修复** — 全站页脚链接下划线由 2px 加粗到 3px、过渡由 0.3s 放慢到 0.45s（index/history/community/license 四页同步），生长过程在深色背景上清晰可见（commit c551263）

---

**官网：许可证页深色化**（commit 0ed4f3f）

### 网站

- **许可证页深色化** — 三页许可证页挂载 common.css，容器/表格/引用块/代码/tab 按钮全量深色，正文链接与页脚下划线统一青色（commit 0ed4f3f）

---

**官网：社区页深色化**（commit ba548e4）

### 网站

- **社区页深色化** — 社区页挂载 common.css（共享深色色板/氛围层/emoji 动效），讨论卡/返回钮/页脚全量深色，讨论标题青白渐变字（commit ba548e4）

---

**官网：历史页深色化**（commit 75a4515）

### 网站

- **历史页深色化** — 版本卡/返回钮/分页/下载按钮/页脚全量深色，版本标题青白渐变字，hover 加青色辉光；「最新」标签改暖金点缀，与首页形成同套分级（commit 75a4515）

---

**官网：首页深色化**（commit ad40628）

### 网站

- **首页深色化** — hero/提示卡/修复面板/卡片/下载区/更新日志/社区/页脚/吸顶导航全量深色，标题改青白渐变字 + 光晕，按钮改青色渐变 + 辉光，卡片 hover 上浮加青色氛围光；hero 顶部青色高光分层、logo 投影改青色辉光（commit ad40628）

---

**官网：深色深邃底色板 + 灵动动效系统**（commit b950d22）

### 网站

- **深色深邃底色板** — 全站配色由亮白改为深邃蓝青色底（#0a0f1f→#14303c 流动渐变），色板分级分层：基础卡/升卡/凹井三层玻璃底、暖金「最新」标签与青色按钮形成冷暖对比、青紫双光晕氛围层；统一收敛为 common.css :root 变量（--card-bg/--text-*/--accent/--glow 等），各页共用（commit b950d22）
- **背景灵动氛围层** — body::before/::after 两团浮动光晕（青色 + 紫青）缓慢漂移，配合首页既有背景流动渐变（commit b950d22）
- **卡片入场强化** — reveal 入场动画升级为 translateY+scale+blur 渐入，选择器提权避免被页面类覆盖（commit b950d22）
- **图标/emoji 微交互** — 卡片/页面标题 emoji 悬停弹跳，提示卡图标悬停摆动，下载图标悬停放大（commit b950d22）
- **弹窗深色化** — 两阶段下载弹窗改深色玻璃 + 青色描边（commit b950d22）

---

**官网：修复卡片 hover 图片模糊**（commit 7258943）

### 网站

- **修复 hover 模糊** — 玻璃卡 backdrop-filter 与 hover transform 同用时触发 Chrome 合成 bug，卡内图片/内容重栅格化变糊；移除全站带 hover 位移的玻璃元素 backdrop-filter（卡片/按钮/版本卡/讨论卡等，保留半透明背景与上浮动效），无位移容器（main-wrapper/页脚/导航）保留毛玻璃（commit 7258943）

---

**官网：导航社区资源链接 + 版本/社区按钮文案调整**（commit 34b5e36）

### 网站

- **导航加社区资源** — 顶部导航新增「社区资源 →」链接直达社区页（commit 34b5e36）
- **文案调整** — 版本页入口「历史版本 →」改为「所有版本 →」，社区按钮「社区分享 →」改为「社区资源 →」，9 语言同步（commit 34b5e36）

---

**官网：移除 Hero 标语文案**（commit c5ba684）

### 网站

- **移除 Hero 标语** — 首页 hero 区「Roblox 式叙事创作平台」标语全站移除（用户反馈不必要），同步删除 index.html 元素、index.css 样式与 9 语言 hero_tagline key（commit c5ba684）

---

**官网美化：附属页轻量换皮**（commit 5b8aedf）

### 网站

- **附属页换皮** — history 版本卡片/返回钮、community 讨论卡片、license 容器与 tab 按钮同步首页圆角/毛玻璃/hover 风格，结构不重构（commit 5b8aedf）
- **分页按钮描边化** — history 页分页按钮补上描边幽灵钮样式，与返回按钮一致（commit 5b8aedf）

---

**官网美化：页脚三栏毛玻璃 + 全站元信息统一**（commit 3530246）

### 网站

- **页脚三栏** — 品牌/快速链接/许可信息分区，毛玻璃背景，链接 hover 下划线滑入（commit 3530246）
- **全站统一** — 六页页脚结构一致，license 三页返回/页脚样式对齐（commit 3530246）

---

**官网美化：下载弹窗打磨**（commit 1185144）

### 网站

- **弹窗打磨** — 圆角统一 32px、标题加 📥 徽章、分隔线细化、平台按钮 hover 光晕、遮罩过渡微调（commit 1185144）

---

**官网美化：字体排版增强 + 背景流动渐变**（commit 798ed12）

### 网站

- **排版增强** — 区块标题 2.2→2.6rem，正文 1.15rem/行高 1.8/字距微调，版本号 tabular-nums 等宽（commit 798ed12）
- **背景动效** — 渐变背景 28s 缓慢流动，prefers-reduced-motion 自动降级（commit 798ed12）

---

**官网美化：卡片与按钮打磨**（commit c904577）

### 网站

- **卡片打磨** — 圆角 56→32px、阴影统一变量、hover 上浮 6px + 内描边青蓝高亮、标题左侧青蓝渐变竖条（commit c904577）
- **按钮打磨** — 主按钮保持渐变并加 hover 光晕，次级入口改描边幽灵按钮，弹窗平台按钮 hover 发光（commit c904577）

---

**官网美化：吸顶导航 + 锚点 + 汉堡菜单**（commit 5e429a4）

### 网站

- **吸顶导航** — 顶部固定玻璃导航（初始半透明、滚动后毛玻璃），含游戏介绍/资源下载/社区分享/历史版本锚点，平滑滚动；body 顶部留白 72px 补偿避免遮挡首屏内容（commit 5e429a4）
- **汉堡菜单** — 移动端折叠为 ☰ 下拉，aria-expanded 同步（commit 5e429a4）
- **i18n** — 新增 nav_intro/nav_download/nav_community/nav_menu_label，9 语言同步（commit 5e429a4）

---

**官网美化：Hero 品牌标语**（commit f31e75c）

### 网站

- **Hero 标语** — logo 下方新增品牌标语「Roblox 式叙事创作平台」，青蓝渐变文字，9 语言同步（commit f31e75c）

---

**官网美化：字体引入 + 设计变量基座**（commit f130021）

### 网站

- **字体引入** — 引入 Noto Sans SC（SIL OFL 1.1，font-display: swap），`:root` 定义 `--font-body` 字体栈，离线回退系统字体（commit f130021）
- **设计变量基座** — 新增 `--radius-card: 32px`、`--shadow-soft`、`--shadow-hover`，为后续卡片/按钮统一圆角阴影（commit f130021）
- **锚点平滑滚动** — `html { scroll-behavior: smooth }`（commit f130021）

---

**官网样式/脚本外置 + 共用 JS + onerror 抽离**（commit 5156482）

### 网站

- **样式/脚本外置** — `index.html`/`html/history_versions.html`/`html/community_share.html`/`LICENSE.html`/`THIRDPARTY_LICENSES.html`/`PROJECT_THIRDPARTY.html` 内联 `<style>`（约 800 行）与 `<script>` 全部抽离到 `css/`、`js/` 目录，页面改为 `<link>`/`<script src>` 引用，可缓存、符合 CSP（commit 5156482）
- **共用 JS** — i18n 三函数（localeMap/getBrowserLang/loadMessages/applyI18n）+ 图片加载失败兜底抽为 `js/common.js`（数据路径由 `<script data-base>` 传入，index 用 `data/`、html/ 子页用 `../data/`）；两阶段下载弹窗（showPlatformSelection 等）抽为 `js/common-modal.js`，index 与历史页共用；页面专属逻辑分 `main.js`/`history.js`/`community.js`/`license.js`（commit 5156482）
- **内联 onerror 抽离** — 删除全部手写长 SVG data-URI 兜底，改为 `data-fallback*` 属性 + common.js 捕获阶段 error 监听统一处理（`imgErrorSvg` 生成兜底图，参数化保持原视觉：下载图标深蓝底白字、logo 浅蓝底深字 + grayscale）（commit 5156482）
- **死 CSS 清理** — 删除从未被引用的 `.option-btn`、`.modal-group-header`；修复步骤占位 div 的内联 `style="display:none"` 归入 `.img-placeholder` 类（commit 5156482）

---

**官网动效优化 + 修复指引面板 Bug**（commit 5156482）

### 修复

- **修复指引面板图片放大后消失** — 展开修复步骤面板后点击面板内图片放大、关闭 lightbox 时会误触「点击面板外部关闭」逻辑导致面板收起。外部点击关闭判断增加忽略 `.lightbox` 内的点击（commit 5156482）

### 网站

- **弹窗动效** — 下载弹窗遮罩淡入淡出（`.show` 类 + opacity/visibility 过渡）、弹窗本体入场/出场位移缩放、阶段一→二宽度 500↔720px 平滑过渡、弹窗内容逐项 stagger 淡入、返回按钮淡入（commit 5156482）
- **滚动进入动画** — 首页 hero/提示/卡片与历史版本卡片进入视口时从下方淡入浮现（IntersectionObserver one-shot + 兄弟 stagger），无 IO 或 JS 失效时页面照常显示（commit 5156482）
- **hover 统一** — 统一 `--ease-out` 缓动曲线；hover 位移收敛（卡片 -4px、按钮 -2px）；`transition: all` 改为具体属性，渐变背景按钮改用 inset 阴影暗化平滑过渡，消除渐变 snap（commit 5156482）
- **减动效支持** — `prefers-reduced-motion` 下关闭全部动画/过渡，布局不受影响（commit 5156482）
- **版本介绍文案** — `versions.json` 1.0.0 条目的更新日志改为「首个公开测试版 + 客套话」，去除技术性条目（commit 5156482）

---

**官网发布 Beta 26w33a（蓝奏云单源）+ UpdateChecker 日期码更新判断**（commit 0bc8850）

### 新增

- **远程快照字段** — `versions.json` 新增 `newest_version_snapshot`（beta 日期码）；`VersionKey` 同步新增 `NEWEST_VERSION_SNAPSHOT` 常量（commit 0bc8850）

### 功能

- **Beta 日期码更新判断** — `checkWebVersion` 整型 `newest_version` 相同时，双方都是 beta 则再按日期码（`YYwWWa` 定宽，字典序即时间序）细分新旧，`26w33a` → `26w33b` 也能触发更新提示（commit 0bc8850）
- **更新弹窗区分测试版/正式版** — 检测到新版本时按远程最新类型区分弹窗：最新为测试版 → 标题「发现测试版更新」并告知可能不稳定；正式版 → 沿用原描述。`UpdateChecker` 新增 `isNewestVersionBeta()`，`MessageBox` 新增 `update_detected_beta.title/content` 语言键（zh_CN/zh_TW/en_US）（commit 0bc8850）

### 网站

- **版本数据** — `newest_version_type` 置为 beta（0），写入 `versions["1"]` 条目：名称 `v1.0.0-beta-26w33a`、更新日志、四平台下载（commit 0bc8850）
- **蓝奏云单源** — 本次 beta 下载全部走蓝奏云：Windows `.exe`、Android `.apk`、Linux `.deb`（蓝奏云不支持 `.sh`）、macOS M1 / Intel 两组 `.zip`（Intel 用 `lanzouyun_intel` 源归入「Intel 处理器」分组）；URL 暂为占位符，待上传后替换（commit 0bc8850）
- **下载项数组支持** — 阶段二弹窗渲染兼容 `download.<平台>.<源>` 的单个对象与对象数组两种格式，同一来源可配置多个具体文件，逐项列出（commit 0bc8850）
- **两阶段下载弹窗** — 点击下载入口先弹「选择下载平台」：只显示该系统的下载来源按钮（蓝奏云 / GitHub / Gitee，按版本实际可下载来源动态显示，mac 的 M1 / Intel 作为两个独立来源按钮），不含任何文件信息；点击某来源后切到更大的第二层弹窗，左上角「返回」回到来源选择、右上角「关闭」关掉全部下载窗口，内容为该来源的「描述 + 下载超链接」逐项列表。主页四平台卡片与历史版本页各版本下载按钮均先进来源选择（commit 0bc8850）

---

**Beta 快照细分版本（Minecraft 法则 YYwWWa）**（commit 23863c8）

### 新增

- **快照字段** — `app_version.json` 新增 `app_version_snapshot` 字段（格式 `YYwWWa`，如 `26w33a`），仅 beta 填充、release 恒空；`VersionKey` 同步新增常量，展示为 `v1.0.0-beta-26w33a`（commit 23863c8）
- **资源同步加快照** — `doFileVersionDifferent` 同时比较快照，同一 `major.minor.patch` 的多次 beta 换快照时也能触发资源增量同步（commit 23863c8）

### 功能

- **打包器快照自动联想** — 发布类型选 beta 时，打包器按当天 ISO 周自动生成默认快照码（同周字母递增 a→b→c，跨周回到 a），可回车接受或手动覆盖，格式严格校验 `YYwWWa`；release 不询问、强制空串（commit 23863c8）

### 变更

- **产物命名带细分** — beta 产物/安装包命名与版本属性带快照码（inno MyAppVersion、deb Version、dmg volname、各平台安装包文件名），如 `qingfeng_setup_windows_v1.0.0-beta-26w33a.exe`；release 不变（commit 23863c8）

### 文档

- **打包文档同步** — `develop/output/README.md` 交互流程、分发 env、版本管理体系表格补充快照字段与自动生成规则（commit 23863c8）

---

**官网新增 Mac 下载入口（M1/Intel 链接分组）**（commit 844bfa1）

### 网站

- **Mac 下载卡片** — 首页资源下载区新增「Mac端」下载卡片，图标使用 `download-mac.png`（iMac 一体机样式，256×256）（commit 844bfa1）
- **下载弹窗分组** — Mac 弹窗将下载源按 M1 / Intel 分组：各源（GitHub/蓝奏云/Gitee）M1 版默认在前，`_intel` 后缀源归入「Intel 处理器」分组，后续每新增一个源补一对即可（commit 844bfa1）
- **9 语言文案** — 各语言 locale 新增 `mac_button` / `mac_intel_group` 按钮与分组标题文案（commit 844bfa1）
- **图标版权声明** — `THIRDPARTY_LICENSES.html` 新增 `download-mac.png` 条目（作者 Kyo-Tux (Asher)，Aeon Icons 系列，icon-icons.com/imac-mac-apple/272，免费商用无需署名），商标声明并入 Apple，并修复重复的商标声明段落（commit a2a9e4f）
- **分发站说明** — `docs/README.md` 平台列表补充 macOS（Mac M1 / Mac Intel）（commit 844bfa1）

---

**.gitignore 打包产物忽略规则收窄为 qingfeng_setup_ 前缀**（commit 920e270）

### 构建

- **忽略规则精确化** — 打包产物忽略规则由宽泛的 `*.zip`/`*.dmg`/`*.command` 收窄为 `qingfeng_setup_*` 前缀（与 apk/deb/sh/exe 统一）；删除无独立产物的 `*.tar.gz` 与 PyInstaller 遗留的 `*.spec` 规则（commit 920e270）

---

**mac 安装器文件名前缀统一英文**（commit 6a2ea4b）

### 变更

- **mac 产物命名统一** — mac 安装器文件名前缀由中文「氢风一键安装_」改为与其他平台一致的 `qingfeng_setup_`（`qingfeng_setup_mac{M1}_v{version}-{type}.command`），docstring 与 README 输出成品表同步（commit 6a2ea4b）

---

**JDK 版本同步镜像 21.0.11_10 → 21.0.12_8**（commit f226df5）

### 构建

- **JDK 版本对齐** — nju/tuna 镜像只保留 JDK 最新版，`21.0.11_10` 已下线返回 404，导致 construo 交叉编译 macOS .app 时下载 JDK 失败；`lwjgl3/build.gradle` 4 处 construo jdkUrl、`gradlew`/`gradlew.bat` 自动下载 URL 同步更新至 `21.0.12_8`，build.gradle 补充注释提醒三处需同步（commit f226df5）

---

**平台脚本失败时退出码非零**（commit 8d02cfa）

### 修复

- **退出码准确** — 各平台脚本 main() 末尾失败时 `sys.exit(1)`，主编排器按子进程返回码判断成败，打包失败的平台不再被汇总误报为"成功"（commit 8d02cfa）

---

**安卓签名密钥安全规范**（commit 00e3b5c）

### 编码规范

- **签名密钥禁止落盘** — `develop/output/README.md` 新增 Android 签名密钥安全规范：storePassword/keyPassword 只允许经环境变量或交互输入传入，禁止写入任何文件；`release.jks` 已被 `.gitignore` 忽略；一旦密码写入文件并提交即视为密钥泄露，需更换密钥库（commit 00e3b5c）

---

**打包交互改为全部平台完成后统一退出**（commit 9f1fe9f）

### 重构

- **统一退出时机** — 主编排器分发平台脚本时设置 `PACKAGE_DISPATCHED=1`，各平台脚本据此跳过各自的"按 Enter 退出"等待；全部选中平台（含跳过）处理完毕后由主编排器统一提示"按 Enter 键退出"，中途某个平台失败不中断流程，最终汇总逐一列出各平台成败（commit 9f1fe9f）

### 文档

- **output/README.md 同步** — 交互流程补充统一退出说明，流水线图标注 `PACKAGE_DISPATCHED` 机制（commit 9f1fe9f）

---

**日志三维改造方案存档**（commit f196250）

### 文档

- **方案存档** — `develop/plans/2026-08-10-log-3d-refactor.md`：LogUtils 空间/异常/上下文/时间四维改造 + Script 90 处 throw 消息统一 + GraphicsObject tag 可空优化设计方案；`DOCUMENTATION_INDEX.md` 补索引行（已确认，暂未实施）（commit f196250）

---

**menu_main 场景动画配置更新**（commit 55ffee0）

### 资产

- **menu_main 场景动画配置** — 场景动画 `type` 字段 snake_case 化（`smoothMove` → `smooth_move`、`backgroundPicture` → `background_picture`），`distance` 参数改为 `speed`，动作节点补充 `delay` 参数（commit 55ffee0）

---

**初始化注释补充 + 动画对象判空简化**（commit b5b513a）

### 重构

- **动画对象判空简化** — `UiAnimationObject` / `GraphicsAnimationObject` 构造后 `target` 恒非 null，移除冗余 `target != null &&` 判断（commit b5b513a）

### 编码规范

- **Init Javadoc 补充** — 初始化流程各步骤（进度条 / 资源修复 / 用户配置 / 音频 / 图形 / UI / 更新检查）补充方法注释（commit b5b513a）
- **GraphicsObject 注释修正** — 构造器 Javadoc 由"UiKind"误标修正为 GraphicsKind，清理残留的未闭合注释块（commit b5b513a）

---

**版本检测 URL 常量提取**（commit 65b6836）

### 重构

- **`WebSite.VERSION_JSON_PATH`** — 官网版本列表路径 `data/versions.json` 提取为常量；`UpdateChecker` 检测地址改用该常量并补充网络回调 Javadoc（commit 65b6836）

---

**官网文案统一为氢风/QingFeng**（commit a9f60d9）

### 网站

- **品牌表述统一** — 官网（`index.html` / `README.md` / 9 语言 locale / `community_share.html` / `history_versions.html`）文案去除"启动器"字样，统一为"氢风（QingFeng）"；修复指引文案同步改为"游戏加载界面"（commit a9f60d9）

---

**打包脚本收拢进 scripts 目录 + 主编排器置根**（commit bee5536）

### 重构

- **脚本目录收敛** — 平台脚本与公共模块收拢进 `develop/output/scripts/` 子目录；主编排器 `build_package.py` 置于 `develop/output/` 根目录，import 时注入 `scripts/` 到 sys.path；产物统一输出到 `develop/output/` 根目录（各平台脚本 `SCRIPT_DIR` → `OUTPUT_DIR`，server 脚本 chdir 改为上级 output 目录）（commit bee5536）

### 文档

- **output/README.md 同步** — 脚本结构图与命令用法更新为 scripts/ 布局（commit bee5536）

---

**.gitignore 忽略 mac 打包产物**（commit c167012）

### 构建

- **.gitignore 更新** — 新增忽略 mac 打包产物 `*.zip` / `*.dmg` / `*.command`；`build_config.env` 忽略路径随脚本迁移至 `scripts/`（commit c167012）

---

**打包工具按平台拆分 + 主编排器逐平台询问**（commit ca77060）

### 新增

- **平台脚本** — `build_package_windows.py` / `build_package_linux.py` / `build_package_android.py`：将原 `build_package.py` 的单体 7 步流水线按平台拆分为独立自包含脚本，各自完成工具链检测、构建与产物复制，可被主编排器分发也可单独运行（commit ca77060）
- **`build_common.py` 公共模块** — 路径常量、`BuildConfig` / `BuildEnvironment`（JDK/ISCC/MinGW/Android SDK 检测）、`run_gradle`、版本读取 / 交互输入 / 确认 / 统一写入 / 还原、`confirm_platform` 单键询问（Enter=是 / Esc=跳过，Windows 用 msvcrt、Unix 用 termios，非 tty 环境默认打包）（commit ca77060）

### 功能

- **主编排器逐平台询问** — `build_package.py` 确认版本并统一写入版本文件后，逐个询问"是否打包 Windows/Linux/Android/macOS 平台安装包？[Enter=是 / Esc=跳过]"，Enter=打包 / Esc=跳过；新增 `--windows / --linux / --android / --mac` 显式指定平台跳过询问（可组合），`--config-only` 保留（commit ca77060）

### 变更

- **旧 CLI 参数移除** — 旧语义 `--linux-only` / `--linux` / `--mac` 删除，统一改用新的显式平台参数，不再兼容（commit ca77060）
- **版本写入只归主编排器** — 平台脚本只读版本（环境变量 `PACKAGE_VERSION / RELEASE_TYPE / APP_VERSION_INT` 优先，否则 `app_version.json`），绝不写项目文件（commit ca77060）

### 重构

- **mac 脚本并入新结构** — `build_mac_package.py` 改名 `build_package_mac.py`，改 import `build_common`，版本解析改为环境变量优先（commit ca77060）

---

**output/README.md 打包工具文档重写**（commit 19890e4）

### 文档

- **`develop/output/README.md` 重写** — 按新多脚本结构更新概述、脚本结构、命令用法、逐平台询问交互流程、各平台流水线说明与产物表（commit 19890e4）

---

## 2026-08-10 — 动画目标类 + RequirementKey.Config 收编 + NativeDialog 回调重构

**动画目标类：AnimationObject + Ui/Graphics 子类**（commit 4c6d207）

### 新增

- **`AnimationObject` 抽象基类 + 子类** — 动画目标定位体系（`animation.task.object` 包）：`fromJson` 按 object 节点 `class` 分发 `UiAnimationObject`（target=UiObject）/`GraphicsAnimationObject`（target=GraphicsObject），子类即类别，无需额外类别枚举（commit 4c6d207）
- **`GraphicsObject` 数据类** — 以"类别 + tag"定位 graphics 元素，模仿 `UiObject`（含 JsonEntity 构造）（commit 4c6d207）
- **`GraphicsKind` 枚举** — graphics 元素类型（backgroundPicture/picture/gif），displayString 绑 `GraphicsKey`，带 `fromString()`（commit 4c6d207）
- **`AnimationKey.Target` 常量** — object 节点 `class` 字段及 `ui`/`graphics` 类别值（commit 4c6d207）

### 重构

- **`AnimationObject` 体系补 Script 式构造器** — 抽象基类加 `valid`/`json` 与 `isValid()`/`getJson()`；子类字段构造 + JsonEntity 构造双构造器，字段构造时 `buildJson()` 按目标生成 object 节点 JSON；`fromJson` 改 fail-fast：对非 Map、未知 class、解析无效的目标抛 `IllegalArgumentException`（消息带完整 json），由动画加载点 catch 降级，符合"内层 throw、边界兜底"错误策略（commit 4c6d207）
- **`AniomationObject` 拼写修正** — 空壳类改名 `AnimationObject` 并变抽象基类（commit 4c6d207）

---

**RequirementKey.Config 通用字段收进 Universal 内部类**（commit 275b055）

### 重构

- **`RequirementKey.Config.Universal` 内部类** — 优先级确认 UI 三个通用配置键从 `Config.UNIVERSAL_PRIORITY_CONFIRM_UI*` 收进 `Config.Universal`，去掉冗余 `UNIVERSAL_` 前缀（嵌套后避免 `Config.Universal.UNIVERSAL_...` 双重前缀），`VirtualInputHandler` 引用同步更新（commit 275b055）

---

**NativeDialog showConfirm 改用两个 Runnable 回调**（commit 944c23e）

### 重构

- **`NativeDialog.showConfirm` 签名改为两个 Runnable** — 原 `ConfirmCallback` 接口（`onConfirm`/`onCancel`）改为 `showConfirm(title, message, Runnable onConfirm, Runnable onCancel)`，调用处可直接用 lambda；`NativeDialogUtils` 新增只传 `onConfirm` 的单参重载；未注入 fallback 默认执行 `onCancel`（安全默认，不误触发确认型操作）；`ConfirmCallback` 接口随之删除（commit 944c23e）

---

## 2026-08-09 — Init 防重入 + BGM 静音修复 + 事件类统一 + UiObject JsonEntity 构造

**Init 维修图标防重入**（commit 0ec09f1）

### 修复

- **`Init.repairGame` 防重入** — 双击过快会触发两次并发修复，两个线程同时同步资源导致文件损坏，入口处加 `if (isRepairing) return` 直接返回（commit 0ec09f1）

---

**修复安卓文件选择器往返后 BGM 静音**（commit 77f5065）

### 修复

- **`AudioManager.playLayout` 恢复 BGM 实际播放状态** — 原以 `bgMusicPlayingObjectMap.containsKey` 判断"在播"即 return，但 Android 打开系统文件选择器快速往返时暂停在 GL 线程、恢复在主线程且被窗口焦点门控，存在顺序颠倒导致 BGM 被暂停却不再恢复的竞态，native 已停而记录仍在、永不重播。改为命中记录时改调 `playBackgroundMusic(tag, false)` 校验 native 状态：在播 no-op，未播则恢复（commit 77f5065）

---

**官网：修复「查看修复方法」按钮 emoji 消失 + 图片点击放大**（commit bdc9d86）

### 修复

- **「查看修复方法」按钮 emoji 消失** — `bindRepairButton` 用 `textContent` 覆盖整个按钮导致 🔧 emoji 与文案 span 被清空，改为仅更新按钮内 `[data-i18n]` span 的文案（commit bdc9d86）

### 网站

- **图片点击放大 lightbox** — `index.html` 新增 lightbox（CSS + HTML + JS），logo、修复步骤图等非交互图片点击放大，排除 `<a>`/`<button>` 内的下载图标（避免与下载行为冲突）；支持点击背景、关闭按钮或 ESC 关闭（commit bdc9d86）

---

**事件类统一命名 + 运行时主题/语言切换**（commit 8870857）

### 新增

- **`RefreshUiManager` 事件 + 分发处理** — 运行时主题切换：调用方用目标主题创建并 init 新 UiManager → 事件携带 → `EventDispatcher` 替换进 `InstanceContent` → 场景重进 → 旧 UiManager 帧间释放（commit 8870857）
- **`SceneStack.refreshGameState()`** — 重进当前状态，使渲染机用新 UiManager 重建页面（commit 8870857）
- **`InstanceContent` 封装替换型 setter** — `setUiManager`/`setTextManager`/`setLayoutManager`/`setVirtualInputHandler` 在替换实例时连带切换图形字体来源、布局管理器、虚拟输入等依赖引用（null 安全，缺失依赖由各自 setter 反向补绑）（commit 8870857）

### 变更

- **`LanguageManager.reload` 清空语言块缓存** — 切换语言前 `blockMap.clear()`：块名不随语言变化，缓存命中会返回旧语言内容（commit 8870857）

### 重构

- **事件类统一命名** — `EventEnterGame`→`EnterGame`、`EventPlayGame`→`PlayGame`、`EventPopGameState`→`PopGameState`、`EventPushGameState`→`PushGameState`、`EventQuitGame`→`QuitGame`、`EventResetGameState`→`ResetGameState`、`EventSetGameState`→`SetGameState`（commit 8870857）
- **输入处理器动态获取 UiManager** — `KeyboardInputHandler`/`ControllerInputHandler` 不再持有构造时固定的 UiManager，改为经 `virtualInputHandler.getUiManager()` 动态获取，切主题后输入跟随新实例（commit 8870857）

### 修复

- **`GameResourceLoader` 字体链路** — 删除把已释放的启动器 UiManager 绑回图形管理器的冗余调用；游戏内标准注入改为 `gameGraphicsManager.quoteUiManager(gameUiManager)`（commit 8870857）
- **`Init.initUi` 去除冗余字体注入** — 与 `uiManager.init` 引用同一实例，多余的 `setGraphicsQuoteFont` 调用已删除（commit 8870857）

---

**UiObject 支持 JsonEntity 构造 + 复用点替换**（commit 7677006）

### 新增

- **`UiObject(JsonEntity)` 构造函数** — 从配置解析 type（→`UiKind`）+ tag 字段构造 UI 对象标识；字段缺失或解析失败时对应字段为 null，由调用方校验（commit 7677006）

### 重构

- **type/tag 键常量收进 `UiKey.UiObject`** — 字符串唯一来源 `UiKey.UiObject.TYPE/TAG`（"type"/"tag"），`RequirementKey.Config.UNIVERSAL_PRIORITY_CONFIRM_UI_TYPE/TAG` 转发引用（commit 7677006）
- **`VirtualInputHandler` 复用替换** — `setPriorityConfirmSelectObject(JsonEntity)` 手工 type/tag 解析改为 `new UiObject(priorityConfig)` + 前置校验，删除冗余解析与 UiKind import（commit 7677006）

---

## 2026-08-08 — JSON 键 snake_case 迁移 + 脚本常量规约 + 虚拟输入优先选中 + 动画配置

**ScriptCommand/ValueCommand type/action 常量按体系拆分**（commit 8efa790）

### 重构

- **ScriptCommand 与 ValueCommand 常量分体系** — 原 `ScriptKey.Command.Type/Action`（混含两个体系）拆分为 `ScriptKey.Script.Type/Action`（ScriptCommandType/ScriptCommandAction）与 `ScriptKey.Value.Type/Action`（ValueCommandType/ValueCommandAction），与 `ScriptKey.Trigger.Type/Action` 三体系并列；`ScriptKey.Command` 仅保留信封字段（`type`/`action`/`param`）与参数结构（commit 8efa790）
- **枚举与解析器引用同步迁移** — `ScriptCommandType`/`ScriptCommandAction`/`ValueCommandType`/`ValueCommandAction` 及 `ScriptCommandParser`（14 处 switch case）的引用改为对应体系常量（commit 8efa790）

---

**脚本 Type/Action 字符串常量规约进 ScriptKey**（commit 173342f）

### 重构

- **脚本指令 type/action 常量统一归入 `ScriptKey`** — 新增 `ScriptKey.Command.Type`/`ScriptKey.Command.Action`（ScriptCommandType/ValueCommandType 的 type 值与 ScriptCommandAction/ValueCommandAction 的 action 值）与 `ScriptKey.Trigger.Type`/`ScriptKey.Trigger.Action`（TriggerType/TriggerAction）；6 个枚举类的 displayString 改引用 ScriptKey 常量（commit 173342f）
- **删除枚举类 `*_STRING` 静态常量** — `ScriptCommandParser`（14 处）与 `TriggerCommandParser`（2 处）switch case 迁移到 `ScriptKey.Command.Type/Action` 与 `ScriptKey.Trigger.Type/Action`，字符串值单一来源；`SCRIPT_INTERNAL_STANDARD.md` 枚举/解析器规范示例同步更新（commit 173342f）

---

**JSON 键 snake_case 迁移补漏（font_args 子字段 + 脚本/样式键）**（commit 1fbf364）

### 重构

- **`JsonKey` font_args 子字段补迁移** — `padX`/`padY` 改为 `pad_x`/`pad_y`（`pad` 单字母不变）（commit 1fbf364）
- **脚本/样式键补迁移** — `ScriptKey` 的 `argumentName`→`argument_name`、`defaultValue`→`default_value`、`thenCommands`→`then_commands`、`elseCommands`→`else_commands`；`JsonKey` 的 `fontName`→`font_name`；`UiKey` 的 `backgroundColor`→`background_color`（当前均无静态 JSON 数据引用，仅常量值迁移）（commit 1fbf364）

---

**JSON 键统一 snake_case 迁移实施**（commit 9723176）

### 重构

- **程序消费 JSON 键全部改为 snake_case** — 8 个常量类（`JsonKey`/`ThemeKey`/`ConfigKey`/`GraphicsKey`/`LayoutKey`/`UiKey`/`RequirementKey`/`VersionKey`）字符串值同步迁移；仓库内 21 个 JSON 文件（config/layout/theme/ui/app_version/user_config）+ 仓库外数据（log_config、外部 user_config/app_version、swxq 全部）约 94+60 处键名统一；`LayoutManager` 注释、`JSON_STANDARD.md`、`CONTRIBUTING.md`、`output/README.md` 键名引用同步（commit 9723176）
- **locale 标识与对象 tag 不迁移** — `en_US`/`zh_CN`/`zh_TW` 语言标识属"值"、`select_lastRole`/`select_nextRole` 对象 tag 属内容标识，保持原样（符合方案边界）（commit 9723176）

---

**JSON 键 snake_case 统一迁移方案**（commit 8f0cd26）

### 文档

- **新增 `2026-08-08-json-key-snake-case.md` 设计方案** — 程序消费 JSON 键 camelCase→snake_case 全量迁移（24 键映射表、常量类对应、仓库内外部文件清单、实施步骤、验证方式）；同步更新 `DOCUMENTATION_INDEX.md`（commit 8f0cd26）

---

**EventDispatcher/RenderPipeline/FileSuffix 注释整理**（commit 81a511c）

### 编码规范

- **`EventDispatcher` 移除未使用参数** — `handleEventOfEnterGame`/`handleEventOfPlayGame` 移除未使用的 `event` 参数，各事件处理方法补充 Javadoc（commit 81a511c）
- **`RenderPipeline`/`FileSuffix` 注释整理** — `updateFrame`/`render` 补充调用注释；`FileSuffix` 错误 Javadoc 注释改为块注释（commit 81a511c）

---

**页面切换过渡动画设计深化**（commit 5970ecf）

### 文档

- **`2026-08-07-page-transition-animation.md` 设计深化** — 取消主题底色清屏（改为纯内容 alpha 渐变 + 保持现有黑色清屏）；动画期间屏蔽输入（禁止任何用户操作导致的游戏逻辑更新，消除 pendingSwitch 竞争）；每帧推进挂钩点暂定（commit 5970ecf）

---

**menu_main 场景动画配置数据**（commit 161cf40）

### 资产

- **`menu_main/config.json` 新增场景动画配置** — `animation` 节点（`fadeIn`/`fadeOut`）数据，含 `smoothMove` action、`synchronization`/`schedule` 数组、`orientation`/`distance`/`duration` 参数及 `fromPage` 键（commit 161cf40）

---

**config_basic 语言信息 label 加宽**（commit e517987）

### 资产

- **`config_basic/layout.json` 语言信息 label 加宽** — 两处 `{language$requirement.json#config.basic.language}` label 尺寸由 `928×100` 调整为 `1855×110`，适配长语言名展示（commit e517987）

---

**GameStatePageInfo 补全页面配置加载映射**（commit 2f41fa6）

### 修复

- **`GameStatePageInfo` 补全 CONFIG/GAME 状态配置映射** — `GAME_STATE_CONFIG_MAP` 由 CONFIG/GAME 为 `null` 改为显式映射：CONFIG（basic/display 均启用）、GAME（menu/role 启用、play 关闭），供页面配置加载与优先选中使用（commit 2f41fa6）

---

**数字型常量统一收编**（commit f7623de）

### 重构

- **`Numeric` 新增通用数字常量嵌套类** — `Time`（异步销毁延迟/HTTP 超时/线程 join/线程池终止等待）、`Alpha`（禁用态压暗、消息框遮罩）、`Layout`（组件内边距）、`Input`（摇杆死区）、`Http`（成功状态码）；`AudioManager`、`GraphicsManager`、`UiManager`、`ButtonManager`、`LabelManager`、`MessageBox`、`UpdateChecker`、`Main`、`ControllerInputHandler` 的裸数字替换为常量引用（commit f7623de）
- **类内私有数字收编为私有常量** — `MenuMain` 版本号区域点击尺寸与文字参数、`Init` 进度条高度比例与维修图标位置尺寸、`TextInputUtils` 桌面输入框对话框布局尺寸、`ValueTask` 指令参数个数（一元/二元）各改为 `private static final` 常量（commit f7623de）
- **`Numeric` 默认字体缩放档位与主题对齐** — `FONT_NORMAL_SCALE_LIST` 由 `{0.5f, 0.8f, 1.2f}` 改为 `{0.8f, 1.2f, 1.5f}`，与 `default_theme/theme.json` 的 `fontUseSize` 数值一致，作为字段缺失/解析失败时的兜底（JSON 数据文件无法引用 Java 常量，故数值对齐而非单一来源）（commit f7623de）

---

**游戏内页面接入虚拟输入优先选中（game_menu/game_role）**（commit fa8278d）

### 功能

- **`GameMenu`/`GameRole` 接入虚拟输入优先选中** — `init` 在布局加载（`addLayout`）之后调用 `virtualInputHandler.setPriorityConfirmSelectObject(configJson)`，从页面 `config.json` 解析 `priorityConfirmUi.type/tag` 设置优先选中对象；游戏数据 `game_menu/config.json` 新增 `priorityConfirmUi`（start 开始按钮）、`game_role/config.json`（select 确认按钮）；两个页面构造函数新增 `VirtualInputHandler` 参数，`InstanceContent` 注册时注入（commit fa8278d）

---

**启动器页面接入虚拟输入优先选中（menu_list/config_basic/config_display）**（commit 5604a69）

### 功能

- **`MenuList`/`ConfigBasic`/`ConfigDisplay` 接入虚拟输入优先选中** — `init` 在 `uiManager.addLayout` 之后调用 `virtualInputHandler.setPriorityConfirmSelectObject(configJson)`，从页面 `config.json` 解析 `priorityConfirmUi.type/tag` 设置优先选中对象；`menu_list/config.json` 新增 `priorityConfirmUi`（import 导入按钮），新建 `config_basic/config.json`（language 语言标签）、`config_display/config.json`（back 返回按钮）；三个页面构造函数新增 `VirtualInputHandler` 参数，`InstanceContent` 注册时注入（commit 5604a69）

---

**主菜单接入虚拟输入优先选中配置**（commit d9997ce）

### 功能

- **`MenuMain` 接入虚拟输入优先选中** — `init` 在 `uiManager.addLayout` 之后调用 `virtualInputHandler.setPriorityConfirmSelectObject(configJson)`，从页面 `config.json` 解析 `priorityConfirmUi.type/tag` 设置优先选中对象（启动按钮），虚拟输入激活且原选中对象消失时自动落到该按钮；`MenuMain` 构造函数新增 `VirtualInputHandler` 参数，`InstanceContent` 注册时注入（commit d9997ce）

---

**VirtualInputHandler 优先选中按控件类型查找 + 游戏内 UiManager 切换 + 编码规范常量使用规则**（commit c306a46）

### 修复

- **`VirtualInputHandler.setPriorityConfirmSelectObject(JsonEntity)` 优先选中按控件类型查找** — `priorityConfirmUi.type` 语义由固定 `"tag"` 字面量改为控件类型（`button`/`label`/`image`），配合 `tag` 走 UiManager 分类型集合（`getButton`/`getLabel`/`getImage`）精确查找，替代全量遍历交互对象集合；配置校验改为 tag 非空 + type 合法（commit c306a46）
- **优先选中支持游戏内页面** — `isInGame()` 时切换为游戏内 `UiManager`（`gameHost.getPlayLocalData().getUiManager()`），使 `game_menu`/`game_role` 等游戏内页面同样可配置优先选中（commit c306a46）

### 编码规范

- **`CODING_STYLE.md` 新增「12. 常量使用」规范** — 非常量类中禁止直接书写字符串/数字字面量（魔法值），必须引用已有且语义相符的常量类（`xxxKey` 等）常量，语义不存在时新增后引用；涵盖数字型常量收编；后续章节编号顺延（commit c306a46）

---

## 2026-08-07 — UiKey/LayoutKey 布局字段 kind/show 单一来源 + RequirementKey.Language 嵌套重构 + 提交规范 + 文件关联图标独立化 + 许可证三文件拆分 + BGM 播放记录同步 + 页面切换过渡动画方案

### 文档

- **新增页面切换过渡动画设计方案** — `develop/plans/2026-08-07-page-transition-animation.md`：切页淡出淡入动画，先顺序（内容 alpha + 主题底色清屏）后交叉（双渲染机/截图）迭代路线；config.json 新增 `animation` 节点（`immediatelyOut/In` 强制立即、`outDuration/inDuration` 时长），user_config 新增 `allowFadeOut/In` 总开关；激活判定 `!immediately && allowFade`；控件级 animation（graphics/ui 元素切入切出）列为待确认扩展；同步更新文档索引（commit d5c0c5f）
- **官网说明补充许可证与版权页面** — `docs/README.md` 新增「许可证与版权」小节，说明官网提供三语许可证查看页 `LICENSE.html` 与项目素材第三方版权声明页 `PROJECT_THIRDPARTY.html`，由首页页脚进入（commit 4f43d09）

---

### 变更

- **许可证改为三文件拆分（英文官方原文为主文件）** — 根目录 `LICENSE` 更换为知识共享官方英文全文（含 Section 1-8 与 Creative Commons Notice，SPDX 文本，GitHub/Gitee 可识别为 CC BY-NC 4.0）；新增 `LICENSE.zh-CN`（简体）与 `LICENSE.zh-TW`（繁体）官方中文全文，替换原「简体→繁体→英文」单文件拼接；`README.md` 三语许可链接与 `assets/THIRDPARTY_LICENSES.md` 头部引用同步更新（commit e4b0b23）

---

### 重构

- **`UiKey` 布局引用字段转发 `LayoutKey.Ui`** — `Button`/`Label`/`Image` 组件的 `KIND`/`SHOW` 由硬编码字符串改为转发引用 `LayoutKey.Ui.KIND`/`LayoutKey.Ui.SHOW`，字符串值以 `LayoutKey` 为唯一来源，调用方语义不变；`ButtonInfo`/`LabelInfo`/`ImageInfo` kind 解析改用各自组件类常量，`JsonShowParser` show 解析改用 `LayoutKey.Ui.SHOW`（commit 37b0da8）
- **`LayoutKey` 新增 `Ui` 嵌套类** — 收编 layout.json 的 ui 节元素引用字段 `KIND="kind"`/`SHOW="show"`，作为字符串唯一来源（commit 37b0da8）

---

### 重构

- **`RequirementKey.Language` 按真实 requirement.json 嵌套重构** — 第一层 key（`message_box`/`menu_main`/`menu_list`/`menu_load`/`config_basic`）对应嵌套类 `MessageBox`/`MenuMain`/`MenuList`/`MenuLoad`/`ConfigBasic`；游戏语言集独立为 `InGame`（对应游戏独立语言包）；弹窗标识常量（`*_KEY`）在 `MessageBox` 中单列并注释"非 JSON key"；`MenuMain`/`MenuList`/`GameMenu` 引用同步更新（commit 70b3ba3）

---

### 文档

- **CLAUDE.md 提交规则补充** — "变更日志"条目写明 CHANGELOG 条目不独立提交，每个内容改动 = 一笔提交（改动文件 + 该改动对应的 CHANGELOG 条目一并提交），按内容逐条拆分，禁止攒一堆条目最后统一提交（commit 78f6fa3）

---

### 变更

- **许可证更换为 CC BY-NC 4.0** — 根目录 `LICENSE` 及 `assets/LICENSE`、`lwjgl3/setup/LICENSE` 副本更换为知识共享「署名-非商业性使用 4.0 国际」（CC BY-NC 4.0）官方文本（简体/繁体/英文三语，简→繁→英顺序），个人与非商业使用自由、商业使用需另行授权；`README.md` 三语许可说明同步更新（commit 4528c5b）
- **许可证商用条款细化** — 商业使用由"禁止"改为"须事先取得作者书面授权、分成比例与方式双方协商确定"（商讨制），保留个人与非商业使用免费；`README.md` 三语许可说明同步更新（commit a2ad78f）
- **项目许可证从 MIT 更换为非商业许可** — 根目录 `LICENSE` 及 `assets/LICENSE`、`lwjgl3/setup/LICENSE` 副本更换为「氢风非商业许可证」（允许修改与个人使用、禁止商业售卖、二次创作作品归创作者所有、第三方组件按各自许可单独授权）；`README.md` 三语许可声明与 `assets/THIRDPARTY_LICENSES.md` 头部引用同步更新（commit 139b570）
- **`inno_setup.iss` 文件关联图标独立化** — 5 种后缀（`.qfg`/`.qfl`/`.qft`/`.qfgl`/`.qfgt`）改用各自独立图标，新增 5 个图标常量与 `[Files]` Source 条目，5 处 `DefaultIcon` 分别指向 `qfg.ico`/`qfl.ico`/`qft.ico`/`qfgl.ico`/`qfgt.ico`（commit e560a77）

### 资产

- **填充 5 个文件关联图标** — `qfg.ico`/`qfl.ico`/`qft.ico`/`qfgl.ico`/`qfgt.ico` 基于默认主题配色二创生成（主主题色 `#3F48CC` + 副主题色 `#FDA1FF` + 黑色描边），含 16/24/32/48/64/128/256 多尺寸（commit e61694a）

### 文档

- **启动器说明更新** — `lwjgl3/setup/README.md` 目录结构补充 5 个图标文件，新增「文件关联」小节（后缀/关联名/图标文件对照表 + 独立替换说明），图标填充说明由"占位待替换"更新为主题配色二创（commit 4f62f4c）
- **第三方素材版权登记** — `assets/THIRDPARTY_LICENSES.md` 按文件逐一登记 4 个文件关联图标来源（qfl/qfgl = document 5_122642 + language-setting 159167，qft/qfgt = document 5_122642 + color-lens 90605；均 icon-icons.com 免费商用授权；qfgl/qfgt 分别为 qfl/qft 的配色互换变体），`qfg.ico` 列入原创素材清单（commit cecef0d）

### 构建

- **打包流程同步三语许可证** — `build_package.py` 安装器步骤将根目录 `LICENSE`/`LICENSE.zh-CN`/`LICENSE.zh-TW` 复制到 `lwjgl3/setup/`，并生成安装向导展示用的三语合并文件 `LICENSE.combined.txt`（UTF-8 BOM）；`inno_setup.iss` 的 `LicenseFile` 指向该合并文件（简体→繁体→英文三段展示），`[Files]` 将三语许可证安装到程序目录；`assets/` 同步三语许可证副本（随 APK 分发）（commit 6e56faa）

### 网站

- **官网新增许可证与项目素材版权页** — 新增 `docs/LICENSE.html`（三语许可证查看页，简体/繁體/English tab 切换）与 `docs/PROJECT_THIRDPARTY.html`（项目素材第三方版权声明，由 `assets/THIRDPARTY_LICENSES.md` 渲染）；`docs/index.html` 页脚改为三链接排列：项目许可证 → 项目素材第三方版权声明 → 网站素材第三方版权声明；9 种语言 locale 新增 `footer_license`/`footer_project_thirdparty`/`footer_website_thirdparty` 页脚文案（commit 7ab8fb6）
- **官网子页面页脚同步三链接** — `docs/html/community_share.html` 与 `docs/html/history_versions.html` 页脚同步为与首页一致的三链接排列（项目许可证 / 项目素材第三方版权声明 / 网站素材第三方版权声明），复用 `footer_license` 等 i18n 文案（commit 3739068）

### 修复

- **LICENSE 中文版 Notice 段混入官网导航文本** — `LICENSE.zh-CN`/`LICENSE.zh-TW` 的 Creative Commons Notice 段因提取截断范围过大，混入 CC 官网页脚导航文本（"Learn more about our work"、"Who we are"、"PO Box 1866" 等）；修正提取逻辑（截断到 Notice 段 `</div>` 结束），重新生成三语许可证及所有副本（assets/、lwjgl3/setup/、`LICENSE.combined.txt`、官网 `LICENSE.html`），并同步 `PROJECT_THIRDPARTY.html` 头部引用文案（commit f1b2f3f）
- **页面切换背景音乐未切换（双播）** — `AudioManager.playLayout` 新增播放记录同步：切页后清理不在当前页面 BGM 列表的旧曲目（`disposeBackgroundMusic`），新旧列表无交集时立即停旧播新、有交集保留共享曲目继续播、新页面空列表时静音；`playLayout` 是启动器与游戏内两个 AudioManager 实例共用的入口，两端行为统一（commit d5c0c5f）

---

### 重构

- **`backgroundMusic`/`music` 收编进 `audio` 节点（Java 端）** — `LayoutKey` 新增 `Audio` 嵌套类（`BACKGROUND_MUSIC`/`MUSIC`），顶层同名常量移除；`LayoutManager.loadLayoutMusic` 改从 `audio` 节点读取，单曲/多曲语义不变（commit d5c0c5f）

---

### 资产

- **4 个 layout.json 音频节点收编** — `menu_main`/`menu_list`/`config_basic`/`config_display` 顶层 `backgroundMusic` 移入 `audio` 节点（commit d5c0c5f）

---

### 文档

- **JSON_STANDARD.md 背景音乐配置同步** — 顶层字段表新增 `audio` 行；3.2 节字段位置改为 `audio.backgroundMusic` 并补充结构示例与单曲/多曲说明（commit d5c0c5f）

---

### 重构

- **`backgroundPicture` 收编进 `graphics` 节点（Java 端）** — `GraphicsKey` 新增 `BACKGROUND_PICTURE` 常量；`LayoutManager.loadLayoutGraphics` 优先读 `graphics.backgroundPicture`，缺失则回退顶层旧格式（兼容第三方页面）；`LayoutKey.BACKGROUND_PICTURE` 注释标注为旧版顶层格式、保留用于回退（commit 93fc0e3）

---

### 资产

- **5 个 layout.json 背景图收编** — `menu_main`/`menu_list`/`menu_load`/`config_basic`/`config_display` 顶层 `backgroundPicture` 移入 `graphics` 节内，与 `picture`/`gif` 子分类并列（commit 93fc0e3）

---

### 文档

- **JSON_STANDARD.md 背景图配置同步** — `backgroundPicture` 从 Layout 顶层字段表移入 `graphics` 描述；3.3 节字段位置改为 `graphics.backgroundPicture` 并补充旧格式回退说明；模板合并行为条目同步（commit 93fc0e3）

---

## 2026-08-06 — 官方语言/主题 Internal 句柄化 + 默认配置损坏恢复预想方案入库 + 版权素材清理替换 + 文件后缀关联补全与四类资源包注册 + 启动器单实例限制 + 启动器源码整理 + 启动器说明文档同步

### 新增

- **`FileHandleKey` 常量类** — `type/key/FileHandleKey.java`，收编文件句柄类型字符串：`INTERNAL="internal"` / `EXTERNAL="external"`，取代散落的字符串字面量（commit 83c0f6a）
- **`FileSuffix` 后缀常量类** — `type/file/FileSuffix.java`，收编文件后缀常量：游戏文件 `.qfg`、资源包（语言 `.qfl`/主题 `.qft`/游戏语言 `.qfgl`/游戏主题 `.qfgt`）、压缩包（zip/rar/7z/tar/gz）、图片（png/jpg/jpeg/bmp/gif）、文本（txt/json/xml/csv）；`FileChooser` 移除 21 个 `EXT_*` 常量改用 `FileSuffix`，`MenuList` 游戏文件导入改用 `FileSuffix.EXT_GAME`（commit 81b9f0b）
- **四类资源包后缀平台关联注册** — 语言包 `.qfl`/主题包 `.qft`/游戏语言包 `.qfgl`/游戏主题包 `.qfgt` 三平台文件关联：Windows `inno_setup.iss` 新增四组 ProgID 注册（双击带参运行 `launcher.exe "%1"`）；Linux `build_package.py` MIME 注册由 1 个扩展为 5 个 mime-type（含 4 个新包类型），文件名 `x-qingfeng-game.xml` 改名 `x-qingfeng.xml`；Android `AndroidManifest.xml` intent-filter 追加 4 个 `pathPattern`（commit b1bbd2e）
- **官网游戏介绍文案重写** — `docs/index.html` 游戏介绍四段文案基于项目真实功能重写（视觉小说引擎与跨平台启动器定位、三语言界面/主题系统/三平台支持、资源包管理与版本检测修复、专属文件格式多平台关联与规划中的扩展能力），移除 2D 地图/视频播放/可视化编辑器/一键导出 `.qfg` 等未实现的不实宣传；同步更新 9 种语言 locales（zh/zh-TW/en/de/fr/ja/ko/pt/ru）与 fallback 文本，并顺带修复 en/de/ko/pt 四个语言文件 `repair_step2_text` 中未转义的 ASCII 引号导致的 JSON 解析错误（commit 9ba5ca4）

### 功能

- **官方语言/主题不对外暴露（Internal 句柄化）** — `LanguageManager`/`ThemeManager` 增加 `kind` 路径类型字段与 `getKind()`；命中词典时读取 `name`/`kind`，`kind="internal"` 用 Internal 句柄直读官方内容，省略 `kind`（或非 internal）用 External 句柄读第三方独立目录。官方备选语言/主题不再复制到 External，从根上消除"清空 External 后只恢复默认"的目录缺失问题（commit 83c0f6a）

### 变更

- **语言/主题词典配置改为嵌套结构** — `language_config.json` / `theme_config.json` 每个 key 的 value 从字符串改为 `{name, kind}` 对象（如 `{"default_theme": {"name": "默认主题", "kind": "internal"}}`），同步新增 `LanguageKey.Config`/`ThemeKey.Config` 的 `NAME`/`KIND` 常量（commit 83c0f6a）
- **回退分支不再复制官方目录** — 词典缺省回退时不再 `copyDirectory` 官方目录到 External，改为 Internal 句柄直读 + 修复用户配置 + 融合内部词典（`combined()` 旧优先）补回官方条目，保留第三方字段（commit 83c0f6a）
- **`update_config.json` prohibit 新增官方语言/主题目录** — `asset/language/zh_CN`/`zh_TW`/`en_US`、`asset/theme/default_theme`，官方内容不再随更新覆盖 External（commit 83c0f6a）

### 重构

- **`DialogKey` 声明为 final 类** — 工具常量类禁止继承，与 `FileHandleKey`/`LanguageKey`/`ThemeKey` 保持一致（commit 83c0f6a）

### 文档

- **新增默认配置损坏恢复预想方案** — `develop/plans/2026-08-06-language-theme-default-recovery.md`：盘点 Internal 化现状 + "用户删除默认配置"场景差距（外部词典官方条目被删时融合仅在回退分支触发）+ 将来实现方向（融合前置/词典校验/用户配置完整性）；同步更新文档索引（commit 83c0f6a）
- **JSON_STANDARD** — 1.1 主题词典、11.1 语言词典更新为嵌套 `{name, kind}` 结构，补充 `kind` 字段说明与自动修复行为变化（commit 83c0f6a）

---

### 资产

- **版权隐患素材清理与替换** — 5 个 icon-icons.com 来源不明确的图片（`app_repair.png`、`icon.png`、`download-windows/android/linux.png`）从 git 历史彻底抹除（filter-repo 历史重写 + Gitee/GitHub 双远程 force push），全部以同名的可商用新素材回填：（commit 8ca0967）
  - `app_repair.png`（512×512）— Muhamad Taupik / CC BY 4.0（icon-icons.com/230003）
  - `icon.png`（512×512）— Thalita Torres / 免费商用无需署名（icon-icons.com/75465）
  - `download-windows/android/linux.png`（256×256）— Microdot Graphic（windows/linux，CC BY 4.0）/ RoundIcons（android，免费商用）
  - `setup.ico`/`setup.png` — Lokatara Studio / 免费商用无需署名（icon-icons.com/181413）二次创作
  - `repair1.png`/`repair2.png`（官网维修步骤截图）— 截图内嵌旧版维修图标（版权隐患），从 git 历史抹除后重新截图回填

### 文档

- **THIRDPARTY_LICENSES.md** — 素材来源全面登记：图标替换条目重写（app_repair/icon/setup.ico 记录作者+具体链接）；补登记原创素材（icon.ico、console.ico、logo.*、error.png、icon128.png、android launcher 系列）；背景音乐补具体 B站来源链接（BV1uFcwe1EDV）并修正轻音乐包编号为③（commit 8ca0967）
- **THIRDPARTY_LICENSES.html** — 官网平台下载图标拆分为三个独立来源条目（Windows/Linux=Microdot Graphic CC BY 4.0，Android=RoundIcons 免费商用），补充作者链接与商标声明（commit 8ca0967）

---

### 修复

- **`inno_setup.iss` .qfg 文件关联补默认 ProgID** — 新增 `HKCR\.qfg` 默认 ProgID 绑定（`Software\Classes\.qfg` 默认值 → `QFGameFile.qfg`），双击 `.qfg` 文件直接触发 `launcher.exe "%1"` 带参运行，无需手动选择打开方式；与原有 OpenWithProgids 打开方式候选并存，卸载时 `uninsdeletevalue` 一并清除（commit b1bbd2e）

---

### 其他

- **`.gitignore` 打包产物忽略规则对齐新命名** — `develop/output` 产物忽略从通配扩展名（`*.apk`/`*.deb`/`*.sh`/`*.exe`/`*.tar.gz`）改为明确匹配新产物命名 `qingfeng_setup_*` 前缀（无连字符）；删除磁盘上旧命名 `qing-feng_setup_*` 产物（commit 18e245f）

---

### 新增

- **启动器单实例限制** — `lwjgl3/setup/launcher.c` 使用命名互斥体（`Local\com.hujiugame.qingfeng.launcher`）实现进程级单实例：已有氢风在运行时，双击 `launcher.exe` 或经文件关联打开 `.qfg` 会弹窗提示「氢风已经在运行中了」并退出，不再启动第二个实例；同步更新 `lwjgl3/setup/README.md` 工作流并新增「单实例限制」章节（commit 61f31d9）

---

### 重构

- **启动器源码整理（launcher.c）** — `lwjgl3/setup/launcher.c`（704→826 行）整体按编码规范重构：文件内自包含 `_WIN32_WINNT/_WIN32_IE 0x0601` 声明（编译命令无需 -D）、消除 `debug_log` 前向声明；缓冲大小参数统一为元素个数（新增 `ARRAY_LEN` 宏），魔法字符串/数字收编为 `#define` 常量；单实例「已在运行」从错误弹窗改为信息提示（非致命语义修正）；`check_java_version` 改为限时等待（5 秒）再读输出，防 java.exe 挂起永久阻塞启动器；抽取 `java_in_jdk_dir`/`drain_pipe`/`run_java`/`get_extra_args` 辅助函数，Java 输出改为边运行边读到 EOF（消除输出量超管道缓冲时子进程写阻塞导致的死锁）；全文件 `-Wall -Wextra` 零警告编译，工作流/用户可见文案/退出码/Win7 兼容约束全部不变（commit 61f31d9）

---

### 文档

- **`lwjgl3/setup/README.md` 同步更新** — 工作流新增「0. 单实例检测」步骤，新增「单实例限制」章节（命名互斥体行为与会话范围说明），目录结构 `launcher.c` 行数更新为 826（commit 1bbb4d0）

---

## 2026-08-05 — 修复启动器 BGM 退出游戏后不自动播放 + macOS 打包预想方案入库

### 修复

- **启动器主题 BGM 退出游戏后不自动播放** — 进入游戏时 `GameResourceLoader.loadResource` 调 `AudioManager.stopAll()` 只停声不移除 playing 表，退出回主菜单后 `playLayout` 以 `containsKey` 判断"是否在播"把已停止的 BGM 误判为仍在播，导致不再随机重播（静音）。修复：`stopAll()` 清空 `musicPlayingObjectMap`/`musicPlayingPathMap`/`bgMusicPlayingObjectMap`/`bgMusicPlayingPathMap` 播放记录，保留 loaded 表缓存资源，`playLayout` 见 map 为空自动重播启动器主题 BGM（commit 15b978e）

---

### 文档

- **新增 macOS 打包支持预想方案** — `develop/plans/2026-08-05-macos-packaging.md`：盘点 mac 打包现状（construo 双 target / ICNS / 交叉编译入口已就绪）与差距清单（`-XstartOnFirstThread` 未进 construo 启动脚本 / 代码签名 / `--macM1` 未接线），"打开自动安装自动运行"可行性结论；同步更新文档索引（commit 2766898）

---

## 2026-08-04 — 原生弹窗关键字常量收编 + 仓库名统一 + 半透明遮罩纹理升级

### 新增

- **`DialogKey` 原生弹窗关键字常量枚举类** — 新增 `DialogKey.FileChooser` 常量（`IMPORT_GAME_TAG` / `IMPORT_GAME_NAME`），收编文件选择弹窗的 tag 与标题关键字（commit e293b74）

### 文档

- **官网与仓库地址统一** — 仓库名从 `hujiugame-qingfeng` 统一为 `hujiugame.qingfeng`：`README.md`（三语言官网/Releases/Issues 链接）、`docs/README.md`（官网分发站）、`develop/output/README.md`（UpdateChecker versions.json 检测地址）、`assets/THIRDPARTY_LICENSES.md`（项目主页，原误写 `BrainLeech198/qingfeng`）（commit e293b74）

### 编码规范

- **`MenuList` 未收编字符常量收编至 `DialogKey`** — 替换 `"import_game"` / `"选择游戏"` 字面量为 `DialogKey.FileChooser` 常量（commit e293b74）
- **`VirtualInputHandler.setPriorityConfirmSelectObject` 补全注释** — 补充优先选中配置 json 解析与格式校验相关中文注释（commit e293b74）

---

### 编码规范

- **命名统一 `qing-feng` → `qingfeng`** — 项目内残留的连字符命名统一为无连字符：`build_package.py` 安装包文件名前缀、`inno_setup.iss` Windows 安装器注释与安装目录、`CONTRIBUTING.md` / `develop/output/README.md` 安装包命名文档；Android 应用显示名（label）与包名（applicationId）本已是"氢风" / `com.hujiugame.qingfeng`，无需改动（commit 7cc01c5）

---

### 变更

- **UI 遮罩不透明度整体上调一档** — `black16 → black32`、`black32 → black48`：config_basic/config_display/menu_list 三个页面的 tab 遮罩、`button/de2` 三态、`label/de2` 与 `label/de3` 背景同步升级；`black64` 引用与 white 系列不变（commit b0a2545）
- **`directory_structure.json` 同步** — transparent 目录清单追加 `black48.png` 与 `white48.png`（commit b0a2545）

### 资产

- **新增半透明纹理 `black48.png` / `white48.png`** — 1×1 纯色纹理，48% 不透明度（RGBA alpha 122），与现有 16/32/64 系列取整规则一致，供 UI 遮罩使用（commit b0a2545）

---

## 2026-07-29 — 文档更新规范自描述 + 主题版权自动生成方案 + BGM 双播修复 + Android 视口适配 + 配置驱动优先选中 + 打包稳健性

### 文档

- **所有文档头部新增自描述规范** — develop/ 下 8 份文档（CHANGELOG.md、JSON_STANDARD.md、SCRIPT_INTERNAL_STANDARD.md、DOCUMENTATION_INDEX.md、COMMIT_STYLE.md、CODING_STYLE.md、THIRDPARTY_LICENSES_STANDARD.md、output/README.md）各自头部补充三区块：**文档定位**（职责范围）、**文档结构**（编排顺序和格式要求）、**更新规范**（变更时需遵循的规则和同步更新指引）（commit 7e791e2）
- **CLAUDE.md 新增启动必读指令** — 顶部添加 `> **启动必读**`，指令新会话首次回复前先读取 `temp/CLAUDE_MEMORY.md` 恢复历史上下文（commit 7e791e2）
- **CLAUDE.md 文档维护章节更新** — 移除已删除的 `develop/REVIEW.md` 引用，补充 `develop/plans/` 设计方案目录、CONTRIBUTING.md、docs/README.md、develop/output/README.md 等多份文档的维护提醒，新增"各文档头部自描述规范"说明；新增"本地工作记忆"章节引用 `temp/CLAUDE_MEMORY.md`（commit 7e791e2）
- **`temp/CLAUDE_MEMORY.md` 头部补充自描述规范** — 按统一三区块格式（文档定位/文档结构/更新规范）补充头部，gitignored 文件纳入本地工作记忆管理体系（commit 7e791e2）
- **CHANGELOG.md 头部检查项补充** — 新增 `develop/CHANGELOG.md` 自身（每次提交必须更新）和 `develop/plans/` 目录（新建设计方案时建议记录）（commit 7e791e2）
- **`develop/plans/2026-07-29-theme-copyright-generator.md`** — 主题第三方版权声明自动生成方案，声明清单 JSON + 运行时生成器模式，含许可模板库、校验告警、增量维护策略（commit 7e791e2）
- **`DOCUMENTATION_INDEX.md`** — 新增主题版权自动生成方案条目（commit 7e791e2）

---

### 修复

- **AudioManager BGM 双播** — `playLayout()` 改用 `bgMusicPlayingObjectMap.containsKey()` 判断曲目是否已启动，不再依赖 Android 生命周期后不可靠的 `Music.isPlaying()`（commit 35570b5）
- **AudioManager BGM 自然播完无下一首** — `loadBackgroundMusic()` 新增 `setOnCompletionListener`，自然播完后从播放记录中移除 tag，下一帧 `playLayout()` 自动随机下一首（commit 35570b5）
- **Android 主菜单版本号点击无效** — `MenuMain` 手动坐标转换公式与 `FitViewport` 不兼容，改用 `useViewport.getViewport().unproject()` 正确转换屏幕坐标到虚拟坐标系（commit 35570b5）
- **Android 启动崩溃** — `directory_structure.json` 移除 `.claude` 目录条目（开发机独有目录，APK 中不存在）（commit 35570b5）

### 新增

- **VirtualInputHandler.setPriorityConfirmSelectObject(JsonEntity)** — 从配置中读取 `priorityConfirmUi.type/tag`，按 tag 匹配交互对象并设为优先选中，供各页面统一调用（commit 35570b5）

### 变更

- **MenuMain 构造函数新增 useViewport 参数** — 从 InstanceContent 传入，用于屏幕坐标到虚拟坐标的视口转换（commit 35570b5）

### 构建

- **build_package.py try/finally** — Android 打包 `step_build_apk()` 无论成功或失败，`finally` 块确保 `useViewport` 恢复为 `stretch`（commit 35570b5）

---

## 2026-07-26 — 用户配置上载 + GameInfoKey 内类化 + 配置界面语言 + 手柄虚拟控制重写

### 新增

- **`UserConfigManager.uploadTo(GameInfoManager)`** — 将用户配置（语言、主题、视窗、全屏、分辨率、音量）统一上载至运行时 `GameInfoManager`（commit a68c5e6）
- **`LanguageManager.uploadTo` / `ThemeManager.uploadTo`** — 语言/主题管理器新增同名上载方法，将语言名称和主题名称写入 `GameInfoManager`（commit 5bc61d7）
- **`ConfigDisplay` 子页面** — 新增显示配置场景（新建 `ConfigDisplay.java`），注册 `GameSubState.CONFIG_DISPLAY = 1`，`GameStatePageInfo` 映射至 `config_display` 布局（commit 5bc61d7）
- **`ConfigBasic` 语言切换项** — 新增 `refreshItems()` 方法，根据 `itemSelectStateMap` 切换语言标签/语言选中标签的显示；`RequirementKey.Ui` 新增 `CONFIG_BASIC_LANGUAGE` / `CONFIG_BASIC_LANGUAGE_SELECTED`（commit 5bc61d7）
- **`RequirementKey.Config` 优先级 UI 常量** — 新增 `UNIVERSAL_PRIORITY_CONFIRM_UI` 系列常量，为后续配置驱动的优先级选中做准备（commit 466fbd7）
- **语言文件补充** — 三语言 `requirement.json` 新增 `config.basic` 区块（`back` / `language`），旧 `resolution` 字段移至 `config.display`（commit 5bc61d7）
- **手柄模式轮换** — `ControllerInputHandler` 移除 X/Y 开/关虚拟鼠标，改为 START 键循环：`NONE → CONTROLLER_SELECT → CONTROLLER_VIRTUAL_MOUSE → NONE`（commit 5bc61d7）
- **虚拟选择框优先级对象** — `VirtualInputHandler` 新增 `setPrioritySelectObject(InteractableObject)`，在 `tryToKeepSameSelectObject` 失败时自动选中该优先级对象（commit 5bc61d7）
- **取消选择保留** — `refreshSelectObject()` 拆分为 confirm/cancel 两段管线，取消框同确认框一样在页面刷新时保留上次选中对象（commit 5bc61d7）

### 重构

- **`GameInfoKey` 内类化** — 29 个平铺常量重组为 6 个嵌套内部类（`Launcher`/`User`/`GameList`/`Game`/`Play`），`User` 含 `Resolution`/`SoundVolume` 子类，`Play` 含 `TreeStructure` 子类（commit a68c5e6）
- **`GameUserConfigLoader` 清理** — 删除错位的 `putInfo(USER_LANGUAGE/USER_THEME)` 调用（commit a68c5e6）
- **`InstanceContent` 提取 `registerRenderRegistry()`** — 将内联的渲染注册表构建逻辑提取为独立静态方法（commit 5bc61d7）
- **`refreshSelectObject()` 管线拆分** — 确认框和取消框的逻辑分离为独立步骤，提高可维护性（commit 5bc61d7）
- **`ControllerInputHandler` DPAD 行为** — 方向键不再自动进入 `CONTROLLER_SELECT` 模式，仅当前已在该模式时执行方向移动（commit 5bc61d7）
- **`VirtualInputHandler.prioritySelectObject` 消耗型** — 成功选中优先对象后立即置 null，确保仅一次生效，防止后续页面刷新重复选中（commit 7e791e2）

### 修复

- **`uploadTo` 调用时机** — 从 `InstanceContent.init()` 移至 `Init.initUserConfig()` 中 `gameResolver.load()` 之后，避免 `UserConfigManager` 未初始化就尝试上载导致 NPE（commit a68c5e6）
- **`ControllerInputHandler` 模式轮换超时** — 进入 `CONTROLLER_SELECT` 模式时立即调用 `resetVirtualSelectTime()`，防止计时器残余值导致选择框瞬间超时关闭（commit 5bc61d7）

---

## 2026-07-25 — Story/Config/Version/游戏服务 常量收编 + TextManager/LogLevel 内部枚举 + 页面配置修复

### 新增

- **`StoryKey`** — Story 子系统 JSON 字段常量：`Tree` (BLOCK/TYPE/ID/IN/OUT/Type.ROOT/BRANCH/NODE/LEAF)、`Role` (ID/ROOT)、`PAGE`（commit 3a1819b）
- **`ConfigKey`** — 配置文件 JSON 字段常量：`Game` (ID/NAME/VERSION/LAUNCHER_VERSION)、`Content` (COUNT/ROLE/SCRIPTS/TEMPLATES)、`Log` (LOG_LEVEL/FILE_LOG_LEVEL)、`Directory` (DIRECTORY/FILE)（commit 3a1819b）
- **`VersionKey`** — 版本相关 JSON 字段常量：`APP_VERSION`/`APP_VERSION_TYPE`/`APP_VERSION_STRING`、`NEWEST_VERSION`/`NEWEST_VERSION_TYPE`/`NEWEST_VERSION_STRING`、`Update` (PROTECT/PROHIBIT)（commit 3a1819b）
- **`TextManager.Field` 内部枚举** — 文本模板域标识符 `LANGUAGE("language")` / `GAME("game")`，含 `getValue()` 和 `fromValue()` 方法（commit 3a1819b）
- **`LogLevel.Name` 内部枚举** — 日志等级字符串常量 `DEBUG`/`INFO`/`ERROR`，含 `getValue()` 方法（commit 3a1819b）

### 重构

- **常量收编** — 将 Story/Game/Config/Version/User 域所有散落的 JSON 字段名和日志标签替换为 `StoryKey`、`ConfigKey`、`VersionKey`、`ThemeKey` 常量引用。波及 15 文件：`TreeStructureInfo`、`Role`、`GameStoryManager`、`GameLogicService`、`GameRoleManager`、`GameScriptManager`、`GameTemplateManager`、`GameUserConfigManager`、`UserConfigManager`、`FileUtils`、`LogUtils`、`UpdateChecker`、`Main`、`Init`、`FileName`（commit 3a1819b）
- **`UserConfigKey` → `ConfigKey.User`** — 将 `UserConfigKey`（含 SoundVolume 内部类）收编为 `ConfigKey.User`，删除旧文件（commit 3a1819b）
- **`FileName` 补充** — 新增 `UPDATE_CONFIG = "update_config.json"`（commit 3a1819b）
- **`TextManager.parseBraceText` 枚举派发** — switch 语句从字符串比较改为 `Field.fromValue()` + 枚举分支（commit 3a1819b）
- **`LogLevel` 映射常量化** — `STRING_PARSE_LEVEL_MAP`/`LEVEL_DISPLAY_STRING_MAP` 的键值从硬编码字符串改为 `Name` 枚举引用（commit 3a1819b）
- **`LogUtils` 默认配置常量化** — 默认日志等级值从 `"INFO"`/`"DEBUG"` 改为 `LogLevel.Name.INFO.getValue()`/`LogLevel.Name.DEBUG.getValue()`（commit 3a1819b）

### 修复

- **`GameStatePageInfo.GAME_STATE_CONFIG_MAP` 缺失子页面配置** — MENU 区块只注册了 `MENU_MAIN`，切换到 `MENU_LIST`(subState=1) 或 `MENU_LOAD`(subState=2) 时连锁报错"未定义的子页面配置"+"获取页面配置失败 null值"。已补上两项并设为 `true`（commit 3a1819b）

### 提交分组合并说明

> **注意**：以下同一日期条目的变更因依赖关系合并提交，非逐文件独立提交：
> - ConfigKey + UserConfigKey 收编 + 波及 10 文件 → 1 提交
> - ScriptKey 常量体系 + 波及 25 文件 → 1 提交
> - StoryKey 常量体系 + 波及 3 文件 → 1 提交
> - VersionKey + 波及 2 文件 → 1 提交
> - 其他 Key 类新增 + 波及 20 文件 → 1 提交
> - TextManager.Field 内部枚举 → 1 提交
> - LogLevel.Name + LogUtils 常量化 → 1 提交
> - GameStatePageInfo 修复 → 1 提交

---

## 2026-07-24 — UniversalKey/GameStateLayout 重命名 + 配置键修复 + MessageBox 调整

### 新增

- **3D 场景支持预想方案** — `develop/plans/2026-07-24-3d-scene-support.md`，通过 page 目录 3d.json 实现可选 3D 场景，最小架构入侵（commit 7e791e2）

### 变更

- **`MessageBox` UI 参数调整** — 标题高度占比从 `100/600`→`120/600`，标题内容间距从 `5/600`→`10/600`，优化视觉效果（commit 0444287）

### 重构

- **`UniversalKey`→`UniversalUIKey` 重命名** — 类名从 `UniversalKey` 统一为 `UniversalUIKey`，明确其通用 UI 按键常量的职责；新增私有构造器防止工具类实例化。波及 5 文件：`UniversalInputHandlerFunction`、`VirtualInputHandler`、`ConfigBasic`、`GameRole`、`RequirementUiKey` 的旧引用全部同步更新（commit 7e791e2）
- **`GameStateLayout`→`GameStatePageInfo` 重命名** — 类名从 `GameStateLayout` 改为 `GameStatePageInfo`，更准确反映其页面信息映射的职责；新增私有构造器。`SceneStack` 中的旧引用同步更新（commit 7e791e2）

### 修复

- **`RequirementConfigKey.MENU_LIST_PAGE_MAX_GAME` 键值对齐** — 从 snake_case 的 `page_max_game` 修正为 camelCase 的 `pageMaxGame`，与实际 JSON 格式保持一致（commit 0444287）

### 资产

- **`menu_list/config.json`** — 新增菜单列表页面配置，包含 `pageMaxGame: 8`（commit 0444287）

### 文档

- **`DOCUMENTATION_INDEX.md`** — 添加 3D 场景支持预想方案条目（commit 0444287）

---

## 2026-07-23 — SceneStack 重构 + 配置加载 + 文件夹化 + 语言配置合并

### 新增

- **`GAME_STATE_CONFIG_MAP`** — `GameStateLayout` 新增映射表 `Map<Integer, Map<Integer, Boolean>>`，标记需要页面配置的状态（MENU_MAIN=true），其余为 null/false（commit 983d0f7）
- **`loadGameConfig()`** — `SceneStack` 新增私有方法，遵循 `loadGameLayout()` 模式：查映射 → 拼路径 → 加载 `config.json`，文件不存在则返回空 `JsonEntity`（commit 983d0f7）
- **`FileName` 常量** — 新增 `PAGE_LAYOUT`/`PAGE_CONFIG`/`IN_GAME_PAGE_LAYOUT`/`IN_GAME_PAGE_CONFIG`（commit 983d0f7）

### 功能

- **`MenuList.pageMaxGame` 从页面配置读取** — 原硬编码 8 改为从 `configJson` 的 `GAME_LIST_PAGE_MAX_GAME` 键读取，布局配置可控制每页游戏数量。同时补充选中项为空时隐藏 profile 按钮的遗漏逻辑（commit 7e791e2）

### 变更

- **`PathName` 常量重命名** — `ASSET_S_LAYOUT`→`ASSET_S_PAGE`，`IN_GAME_ASSET_S_LAYOUT`→`IN_GAME_ASSET_S_PAGE`（commit 983d0f7）
- **`GameStateLayout` 布局映射** — 去掉 `.json` 后缀（`"menu_main"` 而非 `"menu_main.json"`），适配文件夹化路径（commit 983d0f7）

### 重构

- **`SceneStack` 更新流程拆分** — `updateGameState()` 从单一方法拆分为三阶段：`loadGameLayout()` → `loadGameConfig()` → `updateGameRender(layout, configJson)`。`loadGameLayout()` 返回类型从 `boolean` 改为 `Layout`，对无需布局的状态（INIT）返回空 `Layout` 而非 `null`，消除空布局误判崩溃（commit 7e791e2）
- **`GameStateDataContainer`** — 新增 `configJson` 构造参数及 `getConfigJson()` 方法，config 作为独立数据公民传递（commit 7e791e2）
- **`LayoutConfig`→`Layout` 类重命名** — 类声明、构造器、toString 同步更新。波及 11 文件：`Layout.java`、`LayoutManager.java`、`AudioManager.java`、`GraphicsManager.java`、`UiManager.java`、`GamePlay.java`、`GameRole.java`、`Role.java`、`Page.java`、`GameTemplateManager.java`。全部参数/变量/注释同步更名（commit 983d0f7）

### 修复

- **`safeCrash` 安全崩溃包装** — `Main.java` 新增私有静态方法，先尝试 `CrashUtils.crash`，失败时退化为 `System.err` + `RuntimeException`，避免 CrashUtils 类加载失败导致原始崩溃信息丢失。替换全部 5 处 `CrashUtils.crash` 调用（commit 983d0f7）
- **UI 管理器 JSON 字段空值防护** — `ButtonManager.loadButtonKind` 的 `fontColor`、`ImageManager.loadImageKind` 的 `color`、`LabelManager.loadLabelKind` 的 `fontColor`/`backgroundColor` 在 `getString()` 返回值 null 时不再 NPE，改为 ERROR 日志后 `return false`（commit 983d0f7）
- **`JsonTextParser.parseFontColor` 空值防护** — `fontColor` 字段值类型非字符串时打 ERROR 日志，不再 `Color.valueOf(null)` NPE（commit 983d0f7）
- **`Init.loadProcessColor` 空值防护** — `process_color` 字段值类型非字符串时打 ERROR 日志，不再 NPE（commit 983d0f7）
- **三级 OpenGL 降级仅对 GL/GLFW 异常生效** — `Lwjgl3Launcher` 新增 `isGlCompatibilityError()` 判断，NPE 等游戏逻辑异常直接抛出，不再被降级流程掩盖（commit 983d0f7）
- **虚拟鼠标光标路径 `external`→`internal`** — `ControllerInputHandler` 的虚拟鼠标图片文件句柄从 `Gdx.files.external` 改为 `Gdx.files.internal`，修复打包后光标图片找不到的问题（commit 983d0f7）

### 资产

- **布局文件文件夹化** — 四个页面从扁平 JSON 迁移至 `page/页面名/layout.json` 结构：（commit 7e791e2）
  - `layout/config_basic.json` → `page/config_basic/layout.json`
  - `layout/menu_list.json` → `page/menu_list/layout.json`
  - `layout/menu_load.json` → `page/menu_load/layout.json`
  - `layout/menu_main.json` → `page/menu_main/layout.json`
- **语言配置合并** — 将 `main.json` 中的 UI 文本键（menu/config 块）平面合并入 `requirement.json`，`language.json` 默认块从 `"main"` 改为 `"requirement"`，三语言已合并的 `main.json` 删除（commit 7e791e2）
- **`RequirementLanguageKey`** — 新增启动器菜单/配置页面的语言键常量定义（commit 7e791e2）
- **`RequirementUiKey`** — 新增 `MENU_LIST_BUTTON_PROFILE = "profile"`（commit 7e791e2）
- **`assets/THIRDPARTY_LICENSES.md` 按钮/标签图片更新** — `de.img.*.png`、`mb.img.background.png` 从待替换清单移至原创素材（自行绘制）（commit 7377576）
- **de 默认按钮纹理替换** — 三态 PNG 替换为自生成 UI 纹理，约 260px→680px，消除第三方素材依赖（commit 983d0f7）
- **mb 旧按钮纹理删除** — 三态 PNG 已无引用，对应 UI 配置已迁移至 de（commit 983d0f7）
- **mb 标签背景纹理更新** — `mb.img.background.png` 尺寸 630→1123 字节（commit 983d0f7）
- **`button/de.json` 字体颜色改白** — `#00008BFF`→`#FFFFFFFF`，适应深色纹理按钮（commit 983d0f7）
- **`button/mb.json` 删除** — 弹窗按钮样式已整合至 de（commit 983d0f7）
- **弹窗标签移除 `borderScale`** — `mb.content.json`、`mb.title.json` 移除不再需要的 borderScale 字段（commit 983d0f7）
- **`message_box.json` 按钮样式改 `default`** — 从已删除的 `message_box.button` 改为 `default`（commit 983d0f7）
- **`ui_config.json` 配置列表更新** — 按钮列表移除 `message_box.button`，标签列表新增 `default2`（commit 983d0f7）
- **新增 `default2` 按钮/标签样式** — 使用透明纹理 `black16.png`/`black32.png`，适用于纯文字 UI 元素（commit 983d0f7）
- **UI 纹理生成脚本入库** — `temp_ui_preview/generate.py`（8 配色）、`generate_styles.py`（6 样式变体）、六种风格预览图集（commit 983d0f7）

### 文档

- **`directory_structure.json`** — 同步更新为 `page/` 目录结构（commit 7e791e2）
- **`docs/THIRDPARTY_LICENSES.html`** — 新增官网素材版权声明页面，涵盖平台下载图标（Smashicons CC BY 4.0 + 自创混合）、Fugaz One 字体（SIL OFL 1.1）、原创素材清单（commit 7e791e2）

### 构建

- **`android/build.gradle` 签名验证加固** — 在 `projectsEvaluated` 外部捕获 `android` 扩展引用，避免 lint 等非 release 任务的同名属性遮蔽导致 doFirst 中 NPE（commit 983d0f7）

### 网站

- **页脚增加版权声明** — 三页面（index/community_share/history_versions）统一加入 `© HujiuGame` 和第三方素材版权声明链接（commit 983d0f7）
- **符号与 i18n 文本分离** — 所有 emoji（💡⚡🛠️🔧🎮📥📜💬✨）从 `data-i18n` 元素移出到 HTML 硬编码，语言文件只存纯文本，避免切换语言后符号丢失（commit 983d0f7）
- **9 语言 `community_description` 移除 ✨** — 符号移至 HTML，对齐符号分离策略（commit 983d0f7）
- **讨论区链接更新** — `docs/data/community.json` 更新为新的 Gitee/GitHub Issue 地址，仓库名从 `hujiugame-qingfeng` → `hujiugame.qingfeng`（commit 7377576）

### 类型

- **`RequirementConfigKey` 工具类填充** — 空类补全为 final utility class，新增 `MENU_LIST_PAGE_MAX_GAME = "page_max_game"` 配置键常量（commit 983d0f7）
- **`RequirementUiKey` 分隔符补充** — 按规范补充节分隔符注释，区分启动器/游戏中区域（commit 983d0f7）
- **`MenuList` 配置键引用更新** — `ConfigKey.GAME_LIST_PAGE_MAX_GAME` → `RequirementConfigKey.MENU_LIST_PAGE_MAX_GAME`（commit 983d0f7）

---

## 2026-07-22 — 主菜单标题图替换为 Fugaz One 字体

### 新增

- **`temp_ui_preview/generate_title.py`** — 标题图生成脚本（Font: Fugaz One, PIL 渲染）（commit 7e791e2）

### 变更

- **`menu.title.png`** — 主菜单标题字体从 Pacifico 替换为 **Fugaz One**（Google Fonts / SIL OFL 1.1），"Qing"=#3F48CCFF（蓝），"Feng"=#FDA1FFFF（粉）（commit 983d0f7）

### 文档

- **`assets/THIRDPARTY_LICENSES.md` 条目 #9** — 更新为 Fugaz One 字体声明（作者：LatinoType Limitada / Luciano Vergara）（commit 983d0f7）

---

## 2026-07-19 — loadPicture 错误占位 + JSON 标准 + 音效/图片版权清理

**loadPicture 首次加载失败用 errorTexture 占位 + UI 纹理生成工具 + 第三方素材版权清查**（commit 983d0f7）

### 新增

- **`temp_ui_preview/generate.py`** — UI 纹理 Python 生成工具，8 套配色方案（蓝紫/翠绿/暖橙红/暗黑透明/粉紫/极简黑白/深蓝海洋/琥珀怀旧），圆角渐变风格（commit 983d0f7）
- **`temp_ui_preview/generate_styles.py`** — UI 纹理样式变体生成器，支持 6 种样式（线框风格/新粗野主义 4px 2px 1px/极简细边框/磨砂质感），2x 输出分辨率 + 8x AA 超采样抗锯齿（commit 983d0f7）

### 变更

- **`assets/THIRDPARTY_LICENSES.md` 条目 #12 待替换素材** — 将 MC 摺纸材质包纹理列明替换计划（commit 7e791e2）

### 修复

- **`GraphicsManager.loadPicture` 首次加载失败无限重试** — 当 `getTexture` 返回 `errorTexture` 时，对首次加载的 tag 将其写入 `pictureMap` 占位，避免 `hasPicture` 永远返 false 导致每帧重试和日志刷屏（commit 7e791e2）

### 文档

- **`assets/THIRDPARTY_LICENSES.md`** — 完整补全第三方素材声明条目 #9~#12，涵盖 Pacifico 字体、豆包 AI 图像合集、原创素材、待替换素材（commit 983d0f7）
- **`C:\Users\11067\hujiugame\qingfeng\game\swxq\THIRDPARTY_LICENSES.md`** — 新增 swxq 游戏完整的第三方版权声明（commit 983d0f7）
- **`assets/asset/resource/image/error.png`** — 替换为自生成的 2×2 像素占位图，消除版权风险（commit 983d0f7）

---

**JSON 配置标准总览 + 文档体系清理 + enterGame 回滚修复**（commit 983d0f7）

### 新增

- **`develop/JSON_STANDARD.md`** — JSON 配置标准总览文档，覆盖全部 32 种 JSON 格式，含字段类型、默认值、解析类、新增标准流程（commit 983d0f7）

### 变更

- **`LabelManager` 打字速度常量重命名** — `LABEL_TEXT_TYPING_SPEED` → `DEFAULT_LABEL_TEXT_TYPING_SPEED`，与命名规范对齐（commit 7e791e2）

### 修复

- **`GameSessionManager.enterGame` 失败回滚** — `loadResource`/`loadData` 失败或 `enterGame` 异常时按 LIFO 顺序回滚已加载的资源、数据和用户配置，避免残留加载状态（commit 983d0f7）
- **`GameUserConfigLoader` 主题加载失败语言回滚** — 主题加载失败或异常时回滚已切换的语言管理器，避免 `textManager` 停留在游戏语言（commit 983d0f7）

### 文档

- **`MANUAL_TEST.md` 删除** — 经评估该文件从未作为实际检查清单使用，条目过于笼统或琐碎，予以删除（commit 983d0f7）
- **`DOCUMENTATION_INDEX.md` 更新** — 移除 MANUAL_TEST.md 引用，新增 JSON_STANDARD.md 条目，更新文档更新原则（commit 983d0f7）
- **`CHANGELOG.md` 增加更新检查提醒** — 顶部新增警示框，提醒每次更新日志后检查 `JSON_STANDARD.md`、`SCRIPT_INTERNAL_STANDARD.md`、`DOCUMENTATION_INDEX.md` 是否需要同步更新（commit 983d0f7）

---

**默认主题音效替换 + THIRDPARTY_LICENSES 补充 + JSON 标准文档增强**（commit 983d0f7）

### 变更

- **默认主题按钮/弹窗音效替换** — 按钮点击音效从 `de.aud.click.ogg` 替换为 `862694__cat-fox_alex__random-click-2.wav`（CC0）；弹窗提示音从 `de.aud.ogg` 替换为 `849886__wavewire__ui_textblip_08.wav`（CC BY 4.0）。涉及 `de.json`、`mb.json`、`message_box.json`、`directory_structure.json`（commit 983d0f7）

### 文档

- **`THIRDPARTY_LICENSES.md`** — 新增条目 #7（Wavewire / CC BY 4.0）和 #8（CAT-FOX_ALEX / CC0）（commit 983d0f7）
- **`develop/JSON_STANDARD.md`** — 故事树章节全面重写（Section 7 + 14），补充四种节点差异对比、page 解析流程、in/out 连接机制、跨块跳转和缓存策略；全文档补充上下文敏感字段说明（language 双作用域、textKey 解析上下文、theme/user_config 游戏覆盖关系）（commit 983d0f7）

---

**图片版权全面清查 + error.png 替换 + THIRDPARTY_LICENSES 补全**（commit 983d0f7）

### 变更

- **`THIRDPARTY_LICENSES.md`** — 全面清查项目所有图片素材版权：（commit 983d0f7）
  - 新增 #9：`menu.title.png`（Pacifico 字体 / SIL OFL 1.1）
  - 新增 #10：豆包AI 图像素材合集（`app_init.png`、`controller_*`、`keyboard_*`，均已二次修改）
  - 新增 #11：原创素材清单（`menu.masker.png`、透明图、虚拟输入框等）
  - 新增 #12：待替换素材清单（MC 第三方材质包纹理 3 项）
- **`error.png` 替换** — 原来源不明的 17KB 图标替换为程序自创的 2x2 四色 PNG（白/黑/深灰/浅灰）（commit 983d0f7）

### 文档

- **`CHANGELOG.md`** — 记录本次变更（commit 983d0f7）

---

## 2026-07-18 — 颜色配置修复 + 死代码清理

### 变更

- **`ThemeManager.java` 默认颜色** — 三个兜底色值从 `#FF000000`（全透明红）改为 `#000000FF`（纯黑不透明）（commit 7e791e2）
- **`Init.java` 默认进度条颜色** — 硬编码 int 移位改为 `Color.valueOf("#3F47B5FF")` 可读形式（commit 7e791e2）

### 修复

- **颜色读取 String→Int 不匹配** — `Init.java` 进度条颜色、`UiManager.java` 标签/按钮颜色通过 `getInt()` 读取 hex 字符串，静默返回 0 导致颜色不生效。统一改为 `getString()` + `Color.valueOf()`（commit 983d0f7）
- **`theme.json` 字体颜色透明** — `fontColor` 值为 `#00000100`（alpha=0），改为 `#000000FF`（纯黑不透明）（commit 983d0f7）
- **`app_config.json` 尾随逗号** — 删除 JSON 末尾多余逗号（commit 983d0f7）
- **`MessageBox.java` 颜色格式** — `Color.valueOf("#FFD700")` 缺少 alpha 位，补全为 `#FFD700FF`（commit 983d0f7）

### 移除

- **`UiManager.java` 废弃方法** — 移除重构遗留的死代码 `loadLabelKind(FileHandle, FileHandle)` 和 `loadButtonKind(FileHandle, FileHandle)`，原逻辑已由 `LabelManager` / `ButtonManager` 替代（commit 983d0f7）

  完整源码存档如下：

  ```java
  // ==================== 已移除：UiManager.loadLabelKind (line 1674-1745) ====================
  public boolean loadLabelKind (FileHandle file, FileHandle themePath)
  {
      try
      {
          JsonEntity labelKindJson = new JsonEntity(file);
          LogUtils.debug(UiManager.class, "loadLabelKind 读取标签配置: " + labelKindJson);

          String labelKindName = labelKindJson.getString("name");
          if (labelKindName == null)
          {
              LogUtils.error(UiManager.class, "loadLabelKind 缺少 name 字段: " + labelKindJson);
              return false;
          }

          String fontName = labelKindJson.getString("font");
          if (fontName == null)
          {
              LogUtils.error(UiManager.class, "loadLabelKind 缺少 font 字段: " + labelKindJson);
              return false;
          }

          Label.LabelStyle labelStyle = new Label.LabelStyle();
          labelStyle.font = getFont(fontName, 1.0f);
          labelStyle.fontColor = Color.valueOf(labelKindJson.getString("fontColor"));

          Pixmap bgPixmap = null;
          JsonEntity imageJson = labelKindJson.getJsonEntityByKey("image");
          FileHandle resImagePath = themePath.child(PathName.ASSET_S_RESOURCE_IMAGE);

          if (imageJson.isEmpty())
          {
              bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
              bgPixmap.setColor(Color.CLEAR);
              bgPixmap.fill();
          }
          else if (imageJson.containsKey("background"))
          {
              FileHandle bgFileHandle = resImagePath.child(imageJson.getString("background"));
              if (!bgFileHandle.exists())
              {
                  LogUtils.error(UiManager.class, "loadLabelKind 背景文件不存在: " + bgFileHandle.path());
                  return false;
              }
              bgPixmap = new Pixmap(bgFileHandle);
          }
          else if (labelKindJson.containsKey("backgroundColor"))
          {
              Color bgColor = Color.valueOf(labelKindJson.getString("backgroundColor"));
              bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
              bgPixmap.setColor(bgColor);
              bgPixmap.fill();
          }
          else
          {
              bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
              bgPixmap.setColor(Color.CLEAR);
              bgPixmap.fill();
          }

          pendingPixmapMap.put(PIXMAP_LABEL + labelKindName, bgPixmap);
          pendingLabelStyles.put(labelKindName, labelStyle);
          LogUtils.debug(UiManager.class, "暂存标签背景 pixmap: " + labelKindName);
          return true;
      }
      catch (Exception e)
      {
          LogUtils.error(UiManager.class, "loadLabelKind", e);
          return false;
      }
  }

  // ==================== 已移除：UiManager.loadButtonKind (line 2996-3054) ====================
  public boolean loadButtonKind (FileHandle file, FileHandle themePath)
  {
      try
      {
          JsonEntity buttonKindJson = new JsonEntity(file);
          LogUtils.debug(UiManager.class, "loadButtonKind 读取按钮配置: " + buttonKindJson);

          String buttonKindName = buttonKindJson.getString("name");
          if (buttonKindName == null)
          {
              LogUtils.error(UiManager.class, "loadButtonKind 缺少 name 字段: " + buttonKindJson);
              return false;
          }

          String fontName = buttonKindJson.getString("font");
          if (fontName == null)
          {
              LogUtils.error(UiManager.class, "loadButtonKind 缺少 font 字段: " + buttonKindJson);
              return false;
          }

          TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
          style.font = getFont(fontName, 1.0f);

          Color fontColor = Color.valueOf(buttonKindJson.getString("fontColor"));
          style.fontColor = new Color(fontColor);
          style.downFontColor = new Color(1f - fontColor.r, 1f - fontColor.g, 1f - fontColor.b, fontColor.a);
          style.disabledFontColor = fontColor.cpy().mul(0.5f);

          JsonEntity imageJson = buttonKindJson.getJsonEntityByKey("image");
          FileHandle resImgDir = themePath.child(PathName.ASSET_S_RESOURCE_IMAGE);
          Pixmap upPix = new Pixmap(resImgDir.child(imageJson.getString("up")));
          Pixmap downPix = new Pixmap(resImgDir.child(imageJson.getString("down")));
          Pixmap disabledPix = new Pixmap(resImgDir.child(imageJson.getString("disabled")));

          pendingPixmapMap.put(PIXMAP_BUTTON + buttonKindName + "_up", upPix);
          pendingPixmapMap.put(PIXMAP_BUTTON + buttonKindName + "_down", downPix);
          pendingPixmapMap.put(PIXMAP_BUTTON + buttonKindName + "_disabled", disabledPix);

          JsonEntity audioJson = buttonKindJson.getJsonEntityByKey("audio");
          FileHandle audioFileHandle = themePath.child(PathName.ASSET_S_RESOURCE_AUDIO)
              .child(audioJson.getString("click"));

          pendingButtonStyles.put(buttonKindName, style);
          pendingButtonAudios.put(buttonKindName, audioFileHandle);
          LogUtils.debug(UiManager.class, "暂存按钮 pixmap: " + buttonKindName);
          return true;
      }
      catch (Exception e)
      {
          LogUtils.error(UiManager.class, "loadButtonKind", e);
          return false;
      }
  }
  ```

---

## 2026-07-16 — VirtualInput 选中框保持 + 虚拟输入优化

### 新增

- **`tryToKeepSameSelectObject`** — `VirtualInputHandler` 新增引用搜索实现，交互对象集合刷新后恢复原选中对象行列位置，避免选中框跳跃。仅 O(n) 扫描变更时刻，侵入性最低（commit 983d0f7）

---

## 2026-07-15 — 新增两首背景音乐 + 布局层配置集成

### 新增

- **menu2.mp3（Campus）** / **menu3.mp3（Circulation）** — 乌鸦Producer 免费可商用音乐包，署名"音乐由乌鸦Producer提供"，`THIRDPARTY_LICENSES.md` 新增条目 #5/#6（commit 983d0f7）

### 变更

- **layout JSON 背景音乐扩展** — `menu_main.json`、`menu_list.json`、`config_basic.json` 三文件 `backgroundMusic` 从单曲/单元素组改为三曲数组 `["menu.mp3", "menu2.mp3", "menu3.mp3"]`，对应界面支持随机播放（commit 983d0f7）

---

## 2026-07-14 — 第三方素材版权声明框架

### 新增

- **`THIRDPARTY_LICENSES.md`** — `assets/` 下新建第三方素材著作权声明文件，随发行包分发，记录素材来源、作者、许可协议及署名要求（commit 983d0f7）
- **设计文档** — `develop/specs/2026-07-13-thirdparty-licenses-design.md`：版权声明方案选型（单文件 NOTICE）、格式规格、条目模板（commit 983d0f7）
- **实施计划** — `develop/plans/2026-07-13-thirdparty-licenses-plan.md`：分步实施计划（commit 983d0f7）

### 条目

- **menu.background.png** — 豆包AI 生成，视觉素材可商用（需二次设计）（commit 983d0f7）
- **menu.mp3（Scorching Sun）** — 乌鸦Producer 免费可商用音乐包，署名"音乐由乌鸦Producer提供"（commit 983d0f7）
- **app_repair.png** — Smashicons（icon-icons.com）CC BY 4.0，署名 Icons by Smashicons（commit 983d0f7）
- **icon.png（主题封面图标）** — Smashicons（icon-icons.com）CC BY 4.0，署名 Icons by Smashicons（commit 983d0f7）

---

## 2026-07-13 — 官网 HTML 结构优化 + 更新检测与打包版本管理

**官网 HTML 结构优化：语义化 + 内联样式提取 + 修复步骤面板**（commit 983d0f7）

### 新增

- **修复步骤面板** — 替换 repair 按钮的 `alert()` 为可折叠步骤面板，展示 repair1.png（维修标识位置）和 repair2.png（确定退出）两张步骤图片（commit 983d0f7）
- **按钮文字切换** — 点击"查看修复方法"/"收起修复步骤"切换，点击面板外部自动关闭（commit 983d0f7）
- **多语言支持** — 全部 9 个语言文件新增 7 个 i18n 键（步骤标题/说明/图片加载提示）（commit 983d0f7）

### 重构

- **HTML 结构语义化** — `div.hero-card` → `<header>`；内容区域包裹 `<main>`；`div.tip-card`/`div.card` → `<section>`；`div.footnote` → `<footer>`；品牌名称 `div` → `<h1>`（commit 983d0f7）
- **内联样式提取为 CSS 类** — 游戏介绍卡片的内联 `font-size/line-height/color/padding/margin` → `.intro-body`/`.intro-body p`/`.card-intro`；`download-header`/`share-header` 合并为 `.card-header`；模态框错误文本 → `.modal-error` 类（commit 983d0f7）
- **CSS 按组件分节** — 基础重置/头部/提示卡片/修复步骤/通用卡片/下载/更新日志/社区/页脚/模态框/响应式（commit 983d0f7）
- **卡片间距统一** — `.card + .card` 替代手动 `margin-bottom`（commit 983d0f7）

### 修复

- **`:last-child` / `:first-child` 选择器失效** — `.tip-card:last-child` 因 `<body>` 中后续元素（`div.card`、`div.modal`、`<script>` 等）存在而匹配不到任何元素，导致 `bindRepairButton()` 形同虚设。改为 ID 选择器（`#tipCardWatt`、`#tipCardRepair`）（commit 7e791e2）
- **`applyI18n` 中的 Watt/Repair 文本选择器** — 同上问题统一修复（commit 7e791e2）

### 文档

- **步骤文案调整** — 匹配自动修复流程：点击维修标识 → 自动修复 → 点击确定退出 → 重启程序（commit 983d0f7）

---

**更新检测三段式判断 + 打包脚本版本管理体系增强**（commit 983d0f7）

### 新增

- **JAR manifest `Implementation-Version`** — `lwjgl3/build.gradle` manifest 增加 `Implementation-Version` 属性，编译时自动从 `projectVersion` 注入，运行时可通过 `Package.getImplementationVersion()` 读取（commit 983d0f7）
- **打包工具文档** — `develop/output/README.md` 覆盖打包工具使用方法、7 步流水线详解、版本管理体系（三字段 × 六存储位置）、运行时更新检测机制（commit 983d0f7）

### 重构

- **`UpdateChecker` 版本检测三段式逻辑** — `doFileVersionDifferent()` 补充读取 `appVersion` 整型字段并存储为 `internalAppVersion`/`internalAppVersionType`；`checkWebVersion()` 重写为双段判断：正常时使用 `newest_version` 整型比较，整型字段不存在时回退字符串比较，字符串一致时进一步对比版本类型（beta→release 升级检测）（commit 7e791e2）
- **`appVersion` 整型字段完整链路** — 从 `app_version.json` 读取 → `UpdateChecker` 存储 → 远程 `newest_version` 整型对比，覆盖了此前只比较版本字符串的盲区（commit 7e791e2）

### 构建

- **`build_package.py` 版本管理增强** — 输入版本号、发行类型、整型编码时展示 `[上次: xxx]` 并默认沿用；新增整型编码独立输入（自动上次+1）；`step_update_version()` 补充写入 `appVersion` 整型字段；新增同步 `android/build.gradle` 的 `versionCode`/`versionName`；写入前增加版本确认步骤（展示新旧对比）（commit 983d0f7）
- **`DOCUMENTATION_INDEX.md`** 打包工具条目从 `build_package.py` 指向 `develop/output/README.md`（commit 983d0f7）

---

## 2026-07-11 — repairGame 保护文件还原 + NinePatch 边框视觉缩放

**repairGame 保护文件还原修复 + init 重入守卫**（commit 983d0f7）

### 新增

- **repairGame(Runnable) 优雅修复** — 不崩溃退出，在子线程中完成资源同步后通过 GL 线程回调通知修复结果（commit 983d0f7）
- **NativeDialogUtils 原生对话框** — 支持原生弹窗，Init 修复流程完成后弹窗提示"修复完成，请重启游戏"（commit 983d0f7）
- **mb.img.background.png 图片资源修正**（commit 983d0f7）

### 重构

- **Init 场景集成修复流程** — 修复中设置 `isRepairing` 标志跳过状态机，完成后弹出原生对话框并退出，不再直接 `CrashUtils.crash()`（commit 983d0f7）

### 修复

- **restoreProtectExternalFile 保护文件还原丢失** — 还原前先删除目标文件，避免 Windows `File.renameTo` 因目标已存在而静默失败；检查 `moveFile` 返回值，失败时中断流程而非继续清理 temp — 修复 `user_config.json` 等保护文件被内部默认文件覆盖的问题（commit 7e791e2）
- **init() 重入守卫** — 新增 `volatile initRunning` 标志位，`finally` 块确保所有退出路径复位；防止 `Main.threadUpdateVersion()` 和 `repairGame` 并发调用 `init()` 导致文件状态不一致（commit 7e791e2）

---

**NinePatch 边框视觉缩放：sourceBorder/renderBorder 分离**（commit 983d0f7）

### 重构

- **NinePatch 边框从单值改为 sourceBorder/renderBorder 分离** — 之前 borderScale 修改 NinePatch 裁切深度（`new NinePatch(r, border, border, border, border)`），但对于只有 1px 可见边框的纹理无效（多裁的部分与中心同色）。现在引入两张概念：（commit 983d0f7）
  - `sourceBorder`：固定比例 = 控件/16，决定从源纹理边缘取多少像素作为九宫格裁切位置
  - `renderBorder`：= sourceBorder × borderScale，决定裁出的像素在屏幕上绘制多大
  - 使用 `NinePatch.setLeftWidth/setRightWidth/setTopHeight/setBottomHeight` 实现视觉缩放，即使用户图片只有 1px 边线也能拉伸变粗
  - 涉及文件：`UiManager.java`、`LabelManager.java`、`ButtonManager.java`
- **纹理尺寸安全校验** — 对 sourceBorder * 2 超过纹理尺寸的图片（如 1×1 像素的 `black64.png`）跳过 NinePatch，回退到整图拉伸，防止九宫格无效导致控件不显示（commit 983d0f7）

---

## 2026-07-09 — 初次启动屏幕自适应分辨率 + UseViewport 视口工厂方法

### 新功能

- **初次启动自适应分辨率** — 首次运行检测屏幕尺寸，取 80% × 16:9 设窗口并写入配置文件：（commit 983d0f7）
  - 检测 `Gdx.graphics.getDisplayMode()` 获取屏幕分辨率
  - 以宽度为基准取 80% 后按 16:9 等比算高度，超出屏幕 80% 高度则反算
  - 检测失败回退 1024×576 兜底
  - 写入仅含 resolution 字段的配置到外部路径，由 UpdateChecker protect 自动合并为完整配置
  - 后续启动直接读取已存配置，不再检测

### 重构

- **UseViewport 提取视口创建** — 将 `Main.initLibGDX()` 中的 switch 视口创建逻辑内聚到 `UseViewport.getViewport()` 工厂方法，消除 import 重复和 ScreenSize 直接耦合（commit 983d0f7）

---

## 2026-07-08 — FileHandle 命名规范化 + PathType→FileHandle 全量迁移

**FileHandle 命名规范化 + FileUtils JavaDoc 完善 + .list() 安全替换**（commit 983d0f7）

### 重构

- **FileHandle 变量命名统一** — 消除所有 `xxxFile` 后缀的 FileHandle 变量，统一为 `xxxFileHandle`（文件）或 `xxxPathHandle`（目录）。涉及 15 个文件约 40+ 个变量：（commit 983d0f7）
  - `UiManager.java`（4 处）：`fontJsonFile`、`bitmapFontFile`、`bgFile`、`audioFile`
  - `VirtualInputHandler.java`（8 处）：`controllerConfirmPictureFile` 等按钮图片变量
  - `LayoutManager.java`（5 处）：`backgroundMusicFile`、`musicFile`、`backgroundPictureFile`、`pictureFile`、`gifFile`
  - `ImageManager.java`（2 处）：`imageFile`
  - `LabelManager.java`（3 处）：`bgFile`
  - `ButtonManager.java`（1 处）：`audioFile`
  - `GraphicsManager.java`（1 处）：`errorFile`
  - `MessageBox.java`（1 处）：字段 `boxAudio`
  - `ControllerInputHandler.java`（1 处）：字段 `virtualMousePictureFile`
  - `MenuList.java`（2 处）：`filehandle`
  - `LogUtils.java`（2 处）：`logFileHandle` 字段、`configFileHandle` 局部变量
  - `GameLogicService.java`（1 处）：`gameConfigFile`
  - `CrashUtils.java`（2 处）：`logFile`、`crashFile`
- **FileUtils `copyDirectoryRecursiveFix` 参数/局部变量重命名** — `oldFile`/`newFile` → `sourceDirectoryPath`/`destDirectory`，消除误导性命名；`old_DIRECTORY_STRUCTURE` 等局部变量同步精简（commit 983d0f7）

### 修复

- **`.list()` 安全替换** — 将 `FileUtils.deleteDirectoryRecursive` 中的原始 `file.list()` 替换为 `FileUtils.getList(file)`，确保 Android Internal 目录安全删除（commit 7e791e2）
- **`GameScriptManager` 循环变量修正** — `scriptFile` → `scriptFileHandle`，保持命名一致（commit 7e791e2）

### 文档

- **FileUtils.java 全量 JavaDoc 完善** — 为 14 个公开/私有方法补充详细的 @param 和 @return 描述：（commit 983d0f7）
  - 区分 `isExist`（文件或目录）/ `isFileExist`（仅文件）/ `isDirectoryExist`（仅目录）的语义差异
  - `getList` 说明 Internal 类型使用 `directory_structure.json` 替代 `File.list()` 的跨平台策略
  - `copyDirectory`/`deleteDirectory`/`moveDirectory`/`clearDirectory` 说明操作行为和注意事项
  - `createStringFileOfLog` 说明日志专用场景和 UTF-8 编码
  - `copyDirectoryRecursiveFix` 说明 Android Internal 专用遍历策略

---

**PathType→FileHandle 全量迁移 + QfFiles 包装 + 路径翻倍调试**（commit 7e791e2）

### 新增

- **QfFiles/QfFileHandle** — `util/system/` 下新增 Files 包装层，`toString()` 输出 `"type:path"` 格式，便于日志中区分 External/Internal 等文件类型。`Main.create()` 中通过 `Gdx.files = new QfFiles(Gdx.files)` 一行替换全局生效（commit 7e791e2）
- **LogUtils 日志目录预创建** — `updateFileByDayTime()` 创建日志文件句柄后主动 `file.parent().mkdirs()`，避免首次写入时因目录不存在而失败（commit 7e791e2）
- **FileUtils.createStringFileOfLog mkdirs** — 写入前调用 `file.parent().mkdirs()`，确保日志文件父目录已创建（commit 7e791e2）

### 重构

- **PathType 全量删除** — 从项目 35+ 个文件中移除 `PathType` 传参模式，统一使用 libGDX `FileHandle` 作为文件路径载体，共 -1083/+651 行变更。涉及 `FileUtils`、`ThemeManager`、`LanguageManager`、`UpdateChecker`、`UserConfigManager`、`GameTemplateManager`、`GameRoleManager`、`GameStoryManager`、`GameScriptManager`、`LayoutManager`、`SceneStack`、`MenuList`、`MenuMain`、`Init`、`Page`、`Role`、`Player`、`PlayLocalData`、`EventEnterGame`、`GamePlayDataLoader`、`GameUserConfigLoader` 等核心类（commit 983d0f7）
- **PathType.java 删除** — `type/file/PathType.java` 已无引用，从版本库移除（commit 983d0f7）
- **FileUtils 精简** — 删除所有 `PathType` 重载方法（`createStringFile`/`readStringFile`/`isExist` 等的 PathType 变体），保留纯 `FileHandle` API；`DIRECTORY_STRUCTURE` 重命名为 `INTERNAL_DIRECTORY_STRUCTURE`（commit 983d0f7）
- **Main.java 路径修复** — `rootPath` 不再通过 `Gdx.files.external("").file().getAbsolutePath()` 获取（QfFiles 包装后路径翻倍），改为直接 `System.getProperty("user.home")`（commit 983d0f7）

### 编码规范

- **JsonEntity** — 移除 `FileUtils`/`LogUtils` 导入（不再使用 `readStringFile` 和日志）（commit 7e791e2）
- **import 清理** — `PathType` 删除后波及文件同步移除已不再使用的 import（commit 7e791e2）

### 待修复

- **QfFiles 路径翻倍** — QfFiles 包装启用后，`Gdx.files.external()` 产生的路径出现翻倍（如 `C:\Users\11067\C:\Users\11067\hujiugame\...`），从第 2 次调用起持续累加。临时禁用 QfFiles 包装（Main.java:162 已注释），QfFiles/QfFileHandle 源文件保留供后续排查。根因推测为 QfFileHandle 包装链与 Lwjgl3Files.delegate 交互中的状态污染，具体待二次介入（commit 983d0f7）

---

## 2026-06-20 — 静默崩溃审计：异常分级 + CrashUtils/SafePostRunnable 提取 + 全路径修复

### 新增

- **CrashUtils.java** — 从 Main.java 提取崩溃处理逻辑到 `util/system/CrashUtils.java`，提供 `crash(Throwable)` 和 `crash(Exception)` 两个重载，自动生成独立崩溃日志 + 弹窗通知 + 阻塞退出（commit 983d0f7）
- **SafePostRunnable.java** — 安全的 GL 线程调度工具，包装 `Gdx.app.postRunnable`，异常时自动触发 CrashUtils.crash()（commit 983d0f7）

### 重构

- **EventDispatcher.java** — `handleEvent()` 外层 catch 改为 `throw new RuntimeException(e)`，7 个子 handler 移除冗余 try-catch，异常自然传播到 GameHost → Main → CrashUtils.crash()；保留 `handleEventOfLoadGameConfig` 内层 catch（L2 降级）（commit 7e791e2）
- **Main.java** — 移除 `crash()` 方法（已提取到 CrashUtils）；移除 `import com.hujiugame.qingfeng.Main` 的无效引用的传播（commit 7e791e2）
- **所有 postRunnable** — 关键路径（GraphicsManager/UiManager 的纹理销毁）保留 SafePostRunnable.crash；非关键路径（TextInputUtils/FileChooser/FileExplorer 的回调）降级为内部辅助方法 + 仅日志，避免对话框/文件选择器异常导致游戏崩溃（commit 7e791e2）

### 修复

- **Main.render()** — `catch (Exception)` 改为 `catch (Throwable)`，所有未捕获异常弹出崩溃对话框而非静默闪退（commit 983d0f7）
- **GameHost.run()** — catch 块追加 `throw e`，异常传播到 Main → crash，不再被静默吞噬（commit 983d0f7）
- **RenderPipeline.updateFrame/render** — catch 块追加 `throw e`，异常传播到 GameHost → crash（commit 983d0f7）
- **Init.java** — `initAudio`/`initGraphics`/`initUi` 三处 CrashUtils.crash() 后补加 `return`，消除"崩溃后仍执行后续代码"的逻辑错误（commit 983d0f7）
- **SceneStack.java** — `popGameState`/`setGameState`/`resetGameState` 外层 catch 追加 `CrashUtils.crash(e)`，意外异常不再静默吞掉（commit 983d0f7）

### 优化

- **updateVersionThread** — UncaughtExceptionHandler 降级为只 `LogUtils.error`，版本检查线程崩溃不影响游戏运行（commit 983d0f7）
- **UpdateChecker.repairGame** — 失败路径降级为只 `LogUtils.error`，删文件失败不触发崩溃（commit 983d0f7）
- **CrashUtils.java** — `InterruptedException` 处理从 `e1.printStackTrace()` 改为 `LogUtils.error`（commit 983d0f7）
- **AudioManager/GraphicsManager/UiManager** — dispose 子线程添加 UncaughtExceptionHandler（commit 983d0f7）

---

## 2026-06-18 — 文档全面审查：修复过时类名/包路径/主循环链路 + CHANGELOG

### 修复

- **CLAUDE.md** — 主循环链路修正为 `GameHost.run → renderPipeline.updateFrame → eventQueue/dispatcher → renderPipeline.render`；命名示例 `GameController`→`GameHost`；日志格式更新为 `ClassName.class` 参数（commit 983d0f7）
- **CONTRIBUTING.md** — 主循环链路同上修正；包结构表更新（`controller/`→`core/`、`GameStateService`→`SceneStack`、`EventManager→EventQueue→EventDispatcher`）；建议阅读顺序 `GameController.java`→`GameHost.java`；日志格式同步更新（commit 983d0f7）
- **COMMIT_STYLE.md** — scope 表删除废弃 `controller` 项，更新 `core`/`eventAction`/`ui`/`render` 描述；示例中过时类名全部替换（`GameController`→`GameHost`、`GameEventService`→`EventDispatcher`、`GameStateService`→`SceneStack`）（commit 983d0f7）
- **CODING_STYLE.md** — 示例代码中的 `GameControllerImp`/`gameController`/`gameStateService` 全部替换为当前类名（commit 983d0f7）
- **REVIEW.md** — 事件系统描述 `EventManager + GameEventService`→`EventQueue + EventDispatcher`；`GameController`→`GameHost`；脚本引擎状态更新为 ScriptExecutor + Page/GamePlay 集成已完成（commit 983d0f7）

### 文档

- **MANUAL_TEST.md** — `GameController`→`GameHost`（commit 983d0f7）

---

### 新增

- **个人代码成长分析** — 在 `develop/grow/` 下新增四代项目（SGL/PGL/Qingfeng/Java）全量分析合集 `MERGED_ANALYSIS_20260618.md`（386KB），含 SGL 代码评价、14 维度演化轨迹、注释考古报告（情绪曲线），已基于源码交叉验证修正多处错误（commit 983d0f7）
- **分析法则框架** — `GROW_UP_ANALYSE_RULE.md` 定义溯源原则、五维模型、五阶段分析流（commit 983d0f7）

### 修复

- **DOCUMENTATION_INDEX.md 路径修复** — 第四层引用的文件路径从根目录修正为 `develop/grow/`，`SGL_MYCODE.md`/`GA2026061801.md`/`GA2026061802.md` 三份独立文档已合并为 `MERGED_ANALYSIS_20260618.md`（commit 983d0f7）

### 文档

- **VirtualInputHandler.java 方法注释** — 为 `moveVirtualConfirmSelect`、`refreshInteractableObjectMap`、`refreshSelectObject` 等关键方法补充中文注释（约 30 处），说明边界处理、空集合跳过、行分组刷新等行为的"为什么"（commit 983d0f7）
- **assets/asset_trash/trash_config.json** — 新增空资源回收配置框架（commit 983d0f7）

---

## 2026-06-17 — Button/Label NinePatch 自适应 border + 预加载流程精简 + Debug 日志增强

### 新增

- **按钮/标签自适应 NinePatch** — Button 和 Label 的背景纹理不再用固定 border 值，改为根据**控件实际宽高**动态计算：`border = min(w, h) / 16`。控件放大时四角等比放大，不再出现"角小脸大"的不协调，接近圆/椭圆的纹理也适用（commit 983d0f7）
- **所有 UI 背景统一 NinePatch** — 所有按钮（up/down/disabled 三态）和标签背景纹理自动应用自适应 NinePatch，无须手动配置纹理 assets（commit 983d0f7）
- **Debug 日志打点** — 在 NinePatch 启用/跳过、纹理合并过程、kind 绑定等关键路径添加 LogUtils.debug，崩溃后可从日志反查 border 值、控件尺寸、纹理尺寸（commit 983d0f7）

### 重构

- **`UiManager.buildDrawable` 简化** — 去掉预存的 `pendingPixmapBorders` 查找，仅返回 `TextureRegionDrawable`；border 计算推迟到控件创建时刻（commit 983d0f7）
- **`UiManager.buildLabelDrawable` 简化** — 同样去掉 border 查找，仅返回 `TextureRegionDrawable`（commit 983d0f7）
- **`UiManager.buildLabelBackground` 重构** — 改为接收控件宽高参数，在此动态计算 border 并创建 NinePatch（commit 983d0f7）
- **`LabelManager.createLabelBackground` 重构** — 同上，去掉 `getLabelKindBorders` 查询，改为接收控件宽高（commit 983d0f7）
- **`ButtonManager.createButton` 增强** — 在按钮创建时根据控件宽高计算 border，通过 `adaptDrawable` 工具方法将 up/down/disabled 三个 Drawable 包装为自适应 NinePatchDrawable（commit 983d0f7）
- **删除 `pendingPixmapBorders` / `labelKindBorders`** — 移除两张 border 缓存 Map，不再需要预存 border 值（commit 983d0f7）
- **删除 `NinePatchHelper`** — 自动 border 检测方案已废弃，对应辅助类移除（commit 983d0f7）

### 修复

- **半透明纯色小纹理不显示** — 自适应的 border 算法自带兜底：`border*2 >= 控件短边` 时跳过 NinePatch，回退到普通 TextureRegionDrawable，1×1 等极小纹理正常渲染（commit 983d0f7）

### 编码规范

- **`ButtonManager` imports** — 补全 `NinePatch`/`TextureRegion`/`Drawable`/`NinePatchDrawable`/`TextureRegionDrawable` 导入（commit 983d0f7）
- **`UiManager` imports** — 清理不再使用的 `NinePatch`/`NinePatchDrawable` 导入；随后因 `buildLabelBackground` 使用 NinePatch 又重新加入（commit 983d0f7）
- **`LabelManager` imports** — 移除废弃的 `TextureRegionDrawable`/`NinePatchHelper` 导入，补充 `TextureRegion` 导入（commit 983d0f7）

---

## 2026-06-16 — P0/P1 逻辑漏洞修复 + 脚本引擎包迁移

**P0/P1 逻辑漏洞修复 + 页面进入机制 + 编码规范对齐 + 树结构修复**（commit 983d0f7）

### 新增

- **页面进入机制** — `Player` 新增 `nextPage` + `setNextPage`/`enterNextPage`/`getNextPage` 双缓冲页面切换；`GamePlay.localHostUpdate` 新增完整页面进入流程：初始脚本（start）→ 循环任务（loop）→ 触发器任务（trigger）+ `ScriptExecutor.update` 每帧驱动（commit 983d0f7）
- **TriggerTask 完整实现** — 从空占位类变为完整 `Task` 接口实现，支持每帧轮询触发条件并在满足时推入子任务（commit 983d0f7）
- **PlayLocalData.scriptExecutor** — 新增脚本执行器字段，替代通过 DI 容器获取（commit 983d0f7）

### 重构

- **命名规范对齐** — 全局重命名 `eventManager`→`eventQueue`、`gameController`→`gameHost`、`gameStateService`→`sceneStack`，涉及 17 个文件（GamePlay/GameMenu/GameRole/Init/MenuList/MenuLoad/MenuMain/ConfigBasic/ControllerInputHandler/KeyboardInputHandler/UniversalInputHandlerFunction/VirtualInputHandler/TextManager 等）（commit 983d0f7）
- **PlayLocalData 字段精简** — 移除 `gamePath`/`gamePathType` 字段，改为由 `gamePathDirectory` 推导（commit 983d0f7）
- **GameInfoKey** — 新增 `PLAY_NEXT_PAGE_ID`（commit 983d0f7）
- **Name** — 新增 `GAME_LOOP_TASK_NAME`、`GAME_START_TASK_NAME`、`GAME_TRIGGER_TASK_NAME`（commit 983d0f7）

### 修复

- **ScriptExecutor P0 死循环** — `executeCallAtomicValueCommand` 缺少脚本时返回 0 不推进指令，导致 `executeValueTask` while 循环无限执行同一条指令。改为推入默认值 0 并 `nextCommand()`（commit 7e791e2）
- **ScriptExecutor P0 参数错位** — `executeCallAtomicValueCommand` 创建子任务时 `new HashMap<>()` 误传入 `defaultReturnValue` 位置（应为 0 或脚本声明的默认值），导致脚本返回值被替换为空 HashMap，参与后续计算时 ClassCastException 崩溃（commit 7e791e2）
- **ScriptExecutor P1 CONST 参数 key=null** — `parseArguments` 中 CONST 类型参数的 `getName()` 返回 null，导致所有 CONST 参数以 null 为 key 存入 Map，无法被调用脚本访问。新增 `ArgumentInfo.argumentName` 字段区分参数名与变量名（commit 7e791e2）
- **ScriptExecutor P1 并发修改** — `removeTriggerTask` 在 for-each 遍历 `triggerTaskList` 的同时调用 `removeTask` 间接修改同一列表，修复为遍历快照副本 `new ArrayList<>(triggerTaskList)`（commit 7e791e2）
- **ScriptExecutor 低版本 JDK 兼容** — `String.repeat()` 替换为 `StringBuilder` 循环拼接，兼容 JDK 8（commit 7e791e2）
- **GameVariableManager.setVariable 静默创建** — 对未定义变量赋值时先打 ERROR 日志再继续创建，行为矛盾。改为直接 `put`，不再误报（commit 7e791e2）
- **GameScriptManager 扫描排除自身** — `loadScriptData` 在无配置清单时扫描目录全部 `.json`，将 `script_config.json` 自身也当作 Script 加载，导致 `缺少 commands 字段` 误报。新增文件名过滤排除配置清单自身（commit 7e791e2）
- **TreeStructure setNowPageId 硬编码返回 false** — `RootStructure`/`NodeStructure`/`LeafStructure` 的 `setNowPageId` 之前一律返回 false，导致 `storyGotoPage` 对非 Branch 类型永远跳转失败。改为与各自单页 ID 比对（commit 7e791e2）
- **TreeStructure getPageIdList 返回 null** — 三个树结构实现类返回 null 改为 `Collections.singletonList`，消除 `storyGotoPage` 中 NPE 隐患（commit 7e791e2）

---

**脚本引擎包迁移 + 帧驱动执行器 + 任务系统 + 编码规范修复**（commit 983d0f7）

### 新增

- **ScriptExecutor** — 帧驱动脚本执行引擎，每帧 MAX_COMMAND_COUNT_PER_FRAME=50 条指令上限，支持多任务栈并发（HashMap 打乱遍历）（commit 983d0f7）
- **Task 任务系统** — `Task` 接口 + `ScriptTask`（命令序列执行）/ `ValueTask`（前缀表达式求值）双实现，基于 `TaskStack`（Stack<Task>）管理父子任务委派（commit 983d0f7）
- **TaskType** — `COMMAND_NORMAL` / `COMMAND_WHILE` / `COMMAND_CALL` / `VALUE_MATH` / `VALUE_LOGIC` 五种任务类型（commit 983d0f7）
- **ScriptContent** — 脚本执行上下文，聚合 UiManager / GameSessionManager / GameVariableManager / GameInfoManager / GameScriptManager（commit 983d0f7）
- **GameVariableManager** — 游戏变量管理器，支持 has/get/set/remove 操作（commit 983d0f7）

### 重构

- **包迁移** — `data/script` → `script`（`com.hujiugame.qingfeng.script`），消除深层 data 包层级（commit 983d0f7）
- **Task 类重命名** — `ScriptTask` → `Task`（接口）、`ScriptTaskStack` → `TaskStack`、`ScriptTaskType` → `TaskType`，新增 `ValueTask` 值求值任务（commit 983d0f7）

### 修复

- **GameSessionManager.storyGotoPage** — setNowPageId 失败分支补充 `return false;`（commit 983d0f7）
- **ScriptCommandParser** — `ReturnControlScriptCommandParam` 使用无参构造改为 `new ReturnControlScriptCommandParam(paramJson)`（commit 983d0f7）
- **CallAtomicValueCommandParam** — 序列化 bug 修复：`json.put("arguments", arguments)` 直接存 List<ArgumentInfo> 改为 `stream().map(ArgumentInfo::getJson).collect()`（commit 983d0f7）
- **LabelClickTriggerParam** — 移除过时 `script` 字段及其 JSON 校验（commit 983d0f7）
- **ValueCommandParser** — `parseAtomic` switch 补充 `case CALL:` 分支（commit 983d0f7）

### 编码规范

- **TaskType** — 移除枚举尾部逗号（Java 不允许）（commit 983d0f7）
- **Task 接口** — 统一方法声明空格（`method ()` 而非 `method()`）（commit 983d0f7）
- **ScriptTask / ValueTask** — 修复 `getCurrentCommand` 双空格，统一方法声明空格（commit 983d0f7）
- **TaskStack** — 方法声明添加空格（Allman 风格）（commit 983d0f7）
- **LabelClickTriggerParam** — `tag` 字段添加 `private` 修饰符（commit 983d0f7）
- **导入顺序** — 修复 TaskStack / ScriptTask / ValueTask / ScriptExecutor / GameVariableManager / PageBehavior / GameStoryManager / Page 共 8 个文件的 import 分组顺序（Java 标准库 → libGDX → 项目内部）（commit 983d0f7）
- **Page** — 项目内部 import 按字母重排（commit 983d0f7）

---

## 2026-06-15 — ScriptExecutor 栈安全修复 + PageBehavior 纯内联化

### 重构

- **PageBehavior** — 移除 reference 脚本引用模式，仅保留 inline 内联。删除字段 `isStartScriptInline`/`isLoopScriptInline`/`startScriptName`/`loopScriptName`/`startScript`/`loopScript`；删除三个引用构造器（预加载 Script、动态加载 .script、路径参数）；删除 `buildScriptFromCommands()`/`parseScriptByName()` 及关联 Getter；JSON 解析构造器简化去掉 `scriptPath`/`pathType` 参数（commit 983d0f7）
- **Page** — `new PageBehavior(pageBehaviorJson, scriptPath, scriptPathType)` 同步改为 `new PageBehavior(pageBehaviorJson)`（commit 983d0f7）

### 修复

- **ScriptExecutor.executeScriptTask** — IF/WHILE 指令 push 子任务后，while 循环仍用局部缓存的 `task` 引用消费指令，导致子任务未执行时父任务已 advance 到后续指令。新增栈大小对比检测（`stackSize != taskStack.size()` 时 break），每帧只处理单层栈顶，下一帧 `executeTaskStack` re-peek 后自动消费子任务。同步修复 `executeBreakControlScriptCommand` 缺少 `forceFinish()` 和 `return 1` 的问题（commit 7e791e2）

### 编码规范

- **PageBehavior** — 删除不再需要的 `Script`/`PathType`/`FileUtils` 导入（commit 983d0f7）

---

## 2026-06-10 — 主题字体预缓存 + LogUtils 重构 + 脚本引擎解析器 + 框架重构

**主题字体预缓存 + Page 包迁移 + PageBehavior 骨架**（commit 983d0f7）

### 新增

- **主题字体预缓存配置** — `theme.json` 新增 `fontUseSize` 字段，指定启动时需预缓存的字体缩放尺寸；`ThemeManager` 新增解析逻辑，缺失时回退至 `Numeric.getFontNormalScaleList()` 默认值；`UiManager.CustomFont` 改为使用主题配置的尺寸列表（commit 983d0f7）
- **两个主题已配置** — `default_theme` 按 layout 实际使用配置 `[0.8, 1.2, 1.6]`，`swxq` 游戏主题配置 `[0.8, 1.2, 1.3, 1.6]`（commit 983d0f7）

### 重构

- **Page 类包迁移** — `data.story.Page` → `data.story.page.Page`，对齐 story 包内按功能划分子包的规范（page/tree）；同步更新 `Player`、`GameStoryManager`、`GamePlay` 中的导入路径（commit 983d0f7）

### 基建

- **PageBehavior 骨架** — 在 `story.page` 包下新建 `PageBehavior.java`，为后续 start/loop/trigger 三区行为模型做准备（commit 983d0f7）

---

**LogUtils 重构：字符串标签改为 Class<?> 传参**（commit 983d0f7）

### 重构

- **LogUtils 接口扩展** — 新增 `debug/info/error` 的 `Class<?> clazz` 重载，内部通过 `clazz.getSimpleName()` 获取标签名，消除字符串标签在类重命名时不同步的风险；旧 `String tag` 方法保留向后兼容（commit 983d0f7）
- **全部 71 个源文件调用点迁移** — 1528 处 `LogUtils.xxx("ClassName", ...)` 统一替换为 `LogUtils.xxx(ClassName.class, ...)`，充分利用编译期类型安全（commit 983d0f7）
- **修复 5 处历史错误日志标签** — `ButtonManager` 误用 `"UiManager"`、`LabelManager` 误用 `"UiManager"`、`ImageManager` 误用 `"UiManager"`、`UniversalInputHandlerFunction` 误用 `"UniversalFunction"`、`UpdateChecker` 误用 `"Init"` 等，均修正为所在类自身的 `.class` 引用（commit 983d0f7）

### 编码规范

- **日志标签标准化** — 消除所有与类名不匹配的字符串标签，类重命名后日志标签自动跟随（commit 983d0f7）

---

**脚本引擎解析器实现 + 工具包结构重组 + 值对象重构**（commit 983d0f7）

### 新增

- **命令/值解析器** — `ScriptCommandParser.parse()` / `ValueCommandParser.parse()` 完整实现，支持 control/variable/story 三类命令和 atomic/math/compare/logic 四类值命令的分发解析（含校验/日志/异常处理）（commit 983d0f7）
- **TypeMapper 类型系统** — 新增 `TypeMapper.java`，提供 Java 类与类型字符串的双向映射（int/float/boolean/String），支持 Lenient 宽松解析（commit 983d0f7）
- **Variable/Story 命令支持** — 新增 `VariableScriptCommand` + `CreateVariableScriptCommandParam` / `AssignmentVariableScriptCommandParam`；`ForwardPageStoryScriptCommandParam` / `GotoPageStoryScriptCommandParam`（commit 983d0f7）
- **Script JSON 构造** — `Script` 类新增 `Script(JsonEntity)` 构造函数，支持从 JSON 反序列化完整脚本（含参数、命令列表、返回值）（commit 983d0f7）
- **序列化支持** — 全部 ScriptCommand / ValueCommand 实现 `getJson()`，支持命令对象序列化为 JSON（commit 983d0f7）
- **值对象重构** — `ValueObject` 从接口改为基类，`LogicValue`/`MathValue` 继承并增加指令类型合法性校验（commit 983d0f7）

### 重构

- **工具包结构重组** — `util/parser/` → `util/json/parser/`；`util/interfaces/` → `util/interact/interfaces/`；`LogUtils/FileUtils/PlatformUtils/FilePathConfig` → `util/system/`；`TextInputUtils` 包路径修正（commit 983d0f7）
- **Control 命令参数重命名** — 7 个类去掉 `Command` 后缀统一为 `*CommandParam` 命名规范（commit 983d0f7）
- **IfControl/WhileControl 参数增强** — 子命令 JSON 构建改为流式映射；getter 重命名（`getTrueScript/getFalseScript` → `getThenCommands/getElseCommands`）（commit 983d0f7）

### 编码规范

- **泛型修复** — 多处 `Map<..., Class>` 原始类型 → `Map<..., Class<?>>`（commit 983d0f7）
- **间距规范化** — `ScriptCommandParam`/`ValueCommandParam` 接口方法空格对齐（commit 983d0f7）
- **导入清理** — 移除未使用的引用（commit 983d0f7）

---

**框架重构：controller→core 类名变更 + 事件系统迁移**（commit 983d0f7）

### 重构

- **包结构重组** — `controller/GameController`→`core/GameHost`、`GameStateService`→`core/SceneStack`、`GameRenderService`→`core/RenderPipeline`、`GameConfigLoader`→`core/GameResolver`、`UpdateController`→`core/UpdateChecker`（commit 983d0f7）
- **事件系统迁移** — `controller/GameEventService`→`eventAction/EventDispatcher`、`manager/EventManager`→`eventAction/EventQueue`（commit 983d0f7）
- **引用全面更新** — 同步更新 15 个引用文件的 import、类型声明、getter 调用（commit 983d0f7）
- **旧文件清理** — 删除 `controller/` 下 6 个文件和 `manager/EventManager.java`（commit 983d0f7）

---

## 2026-06-07 — 脚本引擎基础框架 + 布局字段级融合

**脚本引擎基础框架：Script + ScriptCommand + 解析器**（commit 983d0f7）

### 新增

- **脚本数据模型** — 新增 `data/play/script/Script` 和 `ScriptCommand` 类，支持从 `.script` 文件（JSON 数组格式）、`FileHandle`、`JsonEntity`、命令列表多种构造方式；包含有效性校验（`isValid()`）、复制构造、深拷贝、`equals/hashCode/toString` 完整覆写（commit 983d0f7）
- **脚本解析器** — 新增 `utils/json/parser/JsonScriptParser`，提供 `parseType/parseAction/parseScript` 等静态方法，遵循项目 Parser 模式（try-catch + 存在/不存在/异常三级调试反馈）（commit 983d0f7）

### 文档

- **`develop/REVIEW.md`** — 第 72 项"脚本引擎"标记为 🚧 进行中（commit 983d0f7）

---

**布局字段级融合 + Label 全方向对齐修复**（commit 983d0f7）

### 新增

- **Linux `.deb` 文件关联** — `build_package.py` 的 `.desktop` 添加 `MimeType` 和 `%f` 参数；新增 freedesktop MIME XML 注册 `.qfg` → `application/x-qingfeng-game`；`postinst`/`postrm` 添加 `update-mime-database` 刷新（commit 7e791e2）

### 修复

- **`FileChooser.java`** — `EXT_GAME` 常量从 `.qgf` 修正为 `.qfg`，与 README 文档及用户流程一致（commit 983d0f7）
- **`AndroidManifest.xml` / `AndroidLauncher.java`** — `pathPattern`、注释、临时文件名同步修正 `.qgf` → `.qfg`（commit 983d0f7）
- **README / docs / locales** — 三语言及网站文案中 `.qgf` 全部替换为 `.qfg`（commit 983d0f7）

### 文档

- **`develop/REVIEW.md`** — 第 4 项 `.qfg` 文件关联标记完成 ✅（commit 983d0f7）

---

## 2026-06-06 — 代码命名优化 + 命名对齐与主机模式

**代码命名优化 + 启动器控制台修复 + dispose 调试信息补充**（commit 983d0f7）

### 命名优化与包结构调整

- **`GameController`及相关类重构** — 重命名 `GameRenderer`→`GameRenderService`、`GameLogic`→`GameLogicService`、`GamePlayDataContent`→`PlayDataContent`；部分类移动至 `loader/`、`play/` 子包归类（13 文件，含 InstanceContent/多个 Render 实现适配）（commit 983d0f7）
- **Event eventActionName 构造函数赋值** — 8 个 Event 类（EventEnterGame、EventLoadGameConfig、EventPlayGame、EventPopGameState、EventPushGameState、EventQuitGame、EventResetGameState、EventSetGameState）将 `eventActionName` 字段赋值统一移至构造函数中，消除外部手动 set（commit 983d0f7）
- **`RequirementUiKey`** — 新增 GameMenu 所需的 UI 标签 Key（commit 983d0f7）

### Bug 修复

- **`launcher.c`** — 修复 GUI 子系统下 `console:true` 无控制台窗口的问题：新增 `show_console()` 调用 `AllocConsole()` 显式创建控制台并重定向 stdout/stderr；控制台模式下 Java 进程直接继承启动器控制台（不创建管道、不使用 `CREATE_NO_WINDOW`），实现游戏日志实时输出（commit 983d0f7）
- **`build_package.py`** — jlink 模块列表追加 `jdk.crypto.ec`，修复因缺少 EC 加密提供者导致 Let's Encrypt（ECDSA 证书）SSL 握手失败的问题（commit 983d0f7）

### 优化

- **`UpdateController.java`** — 官网更新检测重试间隔从 2 秒延长至 5 秒（`RETRY_DELAY_MS`: 2000→5000），最大尝试次数从 2 次增加至 3 次（`MAX_RETRY`: 2→3）（commit 983d0f7）
- **dispose 调试信息** — `AudioManager`、`MessageBox`、`UpdateController`、`GraphicsManager` 统一添加 `dispose()` 完成情况的调试日志（commit 983d0f7）
- **`LogUtils`** — 日志加载配置输出从原来仅显示等级数字，额外附加等级字符串信息，使日志更直观（commit 983d0f7）
- **`menu.mp3`** — 当前默认主题的菜单音乐换回 v0.0.0-beta 版本（commit 983d0f7）

### 代码清理

- **`PlayLocalData`** — 字段与方法命名对齐：`getPlayerData`/`setPlayerData` 统一为 `getPlayer`/`setPlayer`；getter/setter 按字段声明顺序重排（commit 983d0f7）
- **`GameController`** — 构造函数中 `GameUserConfigLoader`/`GameResourceLoader`/`GamePlayDataLoader` 三个中间局部变量内联至 `sessionManager` 赋值语句，消除冗余局部变量（commit 983d0f7）

### 文档

- **`develop/COMMIT_STYLE.md`** — 修复部分英文提交头的残留信息，全部替换为中文格式（commit 983d0f7）

---

**命名对齐 + GamePlay 主机模式 + 布局安全增强 + 数据结构扩展**（commit 983d0f7）

### 新增

- **`PlayRuntimeData`** — 新增 `playerList` 字段及 CRUD 方法（`getPlayerList`/`setPlayerList`/`addPlayer`/`removePlayer`），支持多人玩家列表管理（commit 7e791e2）
- **`Player`** — 新增 `ipp` 网络地址字段，`setIpp` 同步写入 `GameInfoManager`（commit 7e791e2）
- **`GameInfoKey`** — 新增 `PLAY_IPP` 常量并注册到 keys 列表（commit 7e791e2）

### 命名优化

- **`GameController.getGameDataContent` → `getPlayLocalData`** — 与 `playLocalData` 字段名对齐，消除歧义，统一 6 个文件 24 处调用点（GameMenu、GameRole、3 个 InputHandler）（commit 983d0f7）

### 重构

- **`GamePlay` 主机模式布局系统** — 新增 `generateLayout()` / `localHostUpdate()` / `remoteHostUpdate()` 方法；`update()` 按 `Hoster` 类型分发（LOCAL_HOST 从 Page 获取真实布局，REMOTE_HOST 预留）；`doInit()` 移除直接赋值 `layout`，由主机更新逻辑负责（commit 983d0f7）

### 修复

- **`inno_setup.iss`** — `.qfg` 文件关联的 `DefaultIcon` 从 `{app}\launcher.exe,0`（console.ico）改为 `{app}\icon.ico`，修复 .qfg 文件图标显示控制台图标的问题（commit 983d0f7）

### 优化

- **`GameStateService.updateGameLayout` 布局安全增强** — 子状态映射值为 null 时跳过布局加载；获取布局文件失败时重置为空 `LayoutConfig`，避免残留前一状态的布局数据（commit 7e791e2）

---

## 2026-06-04 — 文档体系重构 + 启动器错误捕获增强 + 崩溃日志独立输出

### 文档体系重构

- **新增根文档索引** — `DOCUMENTATION_INDEX.md` 统一统领所有 .md 文件，按读者角色分层（所有读者 / 贡献者 / 工具链维护者），README.md 三语言段均添加入口链接（commit 983d0f7）
- **新增启动器说明文档** — `lwjgl3/setup/README.md` 详细说明启动器设计目标、工作流程、编译方法、Win7 实验性支持及排查指引（commit 983d0f7）
- **项目文档更新** — `CONTRIBUTING.md` 补充 MinGW-w64 前置要求、launcher.c 位置说明、打包流程细节（commit 983d0f7）
- **Claude 助手指令** — `CLAUDE.md` 新增「文档维护」章节，规定新增/重命名文档文件后必须同步更新 `DOCUMENTATION_INDEX.md`（commit 983d0f7）
- **记忆文件全面刷新** — 10 个记忆文件同步更新至当前项目状态（JDK 21、launcher.c 迁移完成、develop/ 文件合并）（commit 983d0f7）

### 启动器错误捕获增强

- **`launcher.c`** — Java 启动段改为管道捕获：创建 `CreatePipe` 绑定 Java 进程 stdout/stderr，`CreateProcessW` 失败时显示错误码，Java 非零退出时弹窗显示 stderr 错误输出 + 退出码（commit 983d0f7）
- **`launcher.c` Win7 弹窗润色** — 实验性支持提示改写为更周详的排查指引，明确"当前系统环境不满足"是可能原因（commit 983d0f7）

### 崩溃日志独立输出

- **`Main.java` `crash()` 重写** — 崩溃时自动生成独立崩溃日志文件 `hujiugame/qingfeng/log/crash-{yyyyMMdd-HHmmss}.txt`，包含异常类名、消息、完整堆栈跟踪、日志文件引用（commit 983d0f7）
- **`FileName.java`** — 新增 `CRASH_LOG` 常量（`"crash-"`）（commit 983d0f7）
- **崩溃弹窗优化** — 弹窗直接显示崩溃日志的绝对路径，引导用户将此文件发送给开发者（commit 983d0f7）

### JDK 21 升级

- **构建工具链升级至 JDK 21** — `gradlew.bat` 自动下载从 JDK 17→21（清华镜像 `21.0.11_10`），`build_package.py` 打包检测同步升级（commit 983d0f7）
- **construo 跨平台 JDK 同步升级** — `lwjgl3/build.gradle` 中 Linux/macOS/Windows 四个平台 JDK 下载全部更新至 21.0.11_10（commit 983d0f7）
- **jlink `--compress` 参数适配 JDK 21** — `"2"` → `"zip-2"`（JDK 21 废弃旧语法）（commit 983d0f7）

### Windows 7 兼容性修复

- **`build_package.py`** — jlink 生成 JRE 后从 [adang1345/api-ms-win-core-path](https://github.com/adang1345/api-ms-win-core-path) 自动下载开源 shim DLL（~114KB，MIT 协议），复制到 `jre/bin/` 目录。`java.exe` 在 Win7 上启动时将优先加载同目录下的 shim，解决 "api-ms-win-core-path-l1-1-0.dll 缺失" 报错（commit 983d0f7）
- **`launcher.py`** — Windows 7 从"阻断错误"改为"实验性支持警告"，提示用户保留 shim DLL 或安装 KB2533623+UCRT（commit 983d0f7）

### 跨平台零配置构建

- **`gradlew`（Unix）新增 JDK 21 自动下载** — Mac/Linux 用户首次运行自动从清华镜像下载对应平台 JDK（支持 Linux x64、macOS x64、macOS ARM），无需手动安装（commit 983d0f7）
- **`.java-version`** — 新建文件，IntelliJ IDEA 2024.1+ 自动识别项目需要 JDK 21 并提供下载（commit 983d0f7）
- **`build_package.py` 流程解耦** — 跨平台包（construo）构建失败不再阻断 Windows 安装包生成，网络超时等不影响主平台（commit 983d0f7）

### 构建脚本改进

- **`build_package.py`** — ISS 版本号同步移入 step1，消除 step6 重复修改；提取 `restore_backups` 为独立方法；修复 `check_jdk17`→`check_jdk21` 等方法命名（commit 983d0f7）
- **`build_package_server.py`** — 启动时自动切换到脚本所在目录，确保双击运行时正确提供打包产物（commit 983d0f7）
- **`build_package_server.py`** — IP 检测补充 `172.16.0.0/12` 私有网段，过滤 IPv6 地址（commit 983d0f7）

### 代码清理

- **`Lwjgl3Launcher.java`** — 简化窗口聚焦代码，移除冗余类型转换（commit 983d0f7）

---

## 2026-06-02 — Linux 安装包改为自解压 .sh 一键安装

### 文档更新

- **`CONTRIBUTING.md`** — 输出成品表更新：Linux `.tar.gz` / `.deb` 列替换为 `.sh` 一键安装包（commit 7e791e2）

### 打包优化

- **`build_package.py`** — Linux 打包产物从 `.deb` + `.tar.gz` 改为单个自解压 `.sh` 文件（`.deb` 内嵌于脚本末尾），用户双击即可通过 `pkexec` 图形化安装，无需手动输入终端命令（commit 983d0f7）
- **保留 .deb** — 因蓝奏云不支持 `.sh` 分发，保留 `.deb` 作为蓝奏云等平台的分发格式，`.sh` 自解压安装包用于官网直链下载（commit 983d0f7）
- **`.gitignore`** — 新增 `/develop/output/*.sh` 忽略规则（commit 983d0f7）

### 脚本改进

- **`build_package.py`** — 控制台窗口异常关闭修复：入口 `__main__` 改为 try/finally 确保任何情况下（成功/异常）最后都会暂停等待用户按 Enter 退出；移除 `run()` 和 `main()` 中的重复暂停代码；异常时打印完整堆栈后再暂停（commit 983d0f7）
- **`develop/output/build_package_server.py`** — 新增局域网文件分享服务器脚本，双击即可运行，自动显示本机 IP 地址和端口，无需手动输入 `python -m http.server`（commit 983d0f7）
- **移除 `server.py`** — 重命名为 `build_package_server.py`，与 `build_package.py` 命名风格统一（commit 983d0f7）

---

## 2026-06-01 — 官网下载区支持 Linux + Linux 桌面修复

**官网下载区支持 Linux + 打包脚本瘦身优化**（commit 7e791e2）

### 打包优化

- **JAR 瘦身** — 构建 Windows 安装包时自动移除 `.so`/`.dylib` 等非 Windows 原生库，JAR 体积减少 20-30MB（commit 7e791e2）
- **镜像源加速** — JDK 下载地址从 GitHub 切换至南京大学镜像（`mirror.nju.edu.cn/adoptium/`）（commit 7e791e2）
- **Inno Setup 压缩增强** — 改为 `lzma2/ultra64` 提升安装包压缩率（commit 7e791e2）
- **修复 ISS 冗余引用** — 移除 `inno_setup.iss` 中重复的 JAR 引用，避免瘦身结果被覆盖（commit 7e791e2）
- **Linux 打包默认开启** — `build_package.py` 默认同时打包 Linux，不再需要 `--linux` 参数（commit 7e791e2）
- **jlink 补充 `java.desktop` 模块** — 修复 Linux 端文件选择器（Swing/JFileChooser）闪退问题（commit 7e791e2）

### 官网下载

- **`docs/index.html` / `docs/html/history_versions.html`** — 新增 Linux 下载面板，网格布局从 2 列扩为 3 列，旧版本缺少某平台字段时优雅降级提示（commit 983d0f7）
- **`docs/data/locales/*.json`（9 种语言）** — 新增 `linux_button` 字段（commit 983d0f7）
- **`docs/data/image.json`** — 新增 `download-linux` 路径配置（后因 CDN 加载问题回退，改用本地文件 + SVG fallback）（commit 983d0f7）
- **`docs/data/versions.json`** — v1.0.0-beta 新增 linux 下载入口；版本日志增加英文国际化（commit 983d0f7）
- **`docs/resource/image/download-linux.png`** — Linux 下载图标（commit 983d0f7）
- **README 介绍同步至官网** — 游戏介绍板块更新为 README 平台生态文案（commit 983d0f7）

### 提交规范

- **`develop/COMMIT_STYLE.md`** — 提交类型改为中文（新增/修复/优化/重构/测试/文档/构建），BREAKING 格式调整，移除重复行（commit 983d0f7）

---

**Linux 桌面修复：文件选择器改用 zenity、.deb 目录修复**（commit 983d0f7）

### Bug 修复

- **文件选择器 Ubuntu Wayland 崩溃/卡死** — Swing JFileChooser 在 Ubuntu 22.04 Wayland 下无论 GTK2/GTK3 均无法正常工作（GTK2 断言卡死、GTK3 段错误）。改用 `zenity --file-selection`（GNOME 原生文件选择器）完全绕过 Swing/GTK 栈，新增 `ZenityFileChooser.java` 实现 `NativeFileChooser` 接口。Linux 端在 `Lwjgl3Launcher` 中自动选择 ZenityFileChooser，其他平台仍用原有 DesktopFileChooser。（commit 983d0f7）
- **.deb 安装时 `/usr/lib/qingfeng/` 目录不存在** — `_make_tar()` 只写文件不写目录条目，dpkg 在解压时因父目录缺失而失败。改为在 tar 包中显式写入目录条目，按排序顺序先写目录再写文件。（commit 983d0f7）
- **官网 Linux 下载链接改为 .deb** — `versions.json` 中 GitHub/Gitee 下载路径从 `.tar.gz` 改为 `.deb`（蓝奏云暂不支持 .deb 托管，URL 暂时留空）。（commit 983d0f7）
- **Inno Setup 版本号修正** — `inno_setup.iss` 版本号由 `1.0.0` 改为 `1.0.0-beta`，与 gradle.properties 保持一致。（commit 983d0f7）

### 构建配置

- **`build.gradle`** — 移除 construo `roast` 块中的 `-Djdk.gtk.version=2` 参数（不再需要，文件选择器不依赖 Swing GTK）；更新 jlink 注释说明 `java.desktop` 模块实际用途（资源管理器 + 崩溃弹窗）。（commit 983d0f7）

### 打包脚本完善

- **`_make_tar()` 目录条目修复** — tar 包生成时自动收集所有父目录路径，按排序写入 DIRTYPE 条目。（commit 983d0f7）
- **`input("按下回车...")` 暂停** — `main()` 末尾增加暂停，防止非交互环境下控制台窗口在打包完成前自动关闭。（commit 983d0f7）

---

## 2026-05-31 — 架构审查 + UiManager 拆分 + 委托消除 + 日志 Javadoc 补全 + UpdateController 重构 + UI 样式增强

**架构审查报告：Python→Java 翻新综合评价**（commit 983d0f7）

### 新增文档

- **`develop/REVIEW.md`** — 系统性架构审查与翻新进度追踪（合并原三份 review 文档）：（commit 983d0f7）
    - 架构评分 **7/10**（Python 版基线 3/10），10 大进步 + 11 项待改进
    - Python→Java 翻新路线图（P0~P4 优先排序）
    - 状态码对照表与资源路径映射

---

**UiManager 拆分为三子管理器 + 桌面打包脚本便携化**（commit 983d0f7）

### ⚠️ 新人必看：构建环境配置

具体可以看项目根目录文件:MANUAL_TEST.md 和 CONTRIBUTING.md\
本项目使用 **Gradle** 构建，桌面端打包需要额外工具。克隆后请按以下流程操作：

**快速开始（开发运行）：**
```
./gradlew lwjgl3:run        # 运行桌面端
./gradlew android:run        # 运行 Android 端（需连接设备）
```

**打包分发的完整流程**（`develop/output/build_package.py`），支持自动检测环境：

1. JDK 17 — 检测顺序：`JAVA_HOME` → `PATH` → `C:\Program Files\Java\` → 弹窗手动选择
2. Inno Setup 6 — 检测顺序：`ISCC` 环境变量 → `Program Files` → 弹窗手动选择
3. Android SDK — 从 `local.properties` 读取 `sdk.dir`
4. PyInstaller — 可选，用于构建 `launcher.exe`（也可直接用已有的）

首次运行时自动检测以上工具路径并保存到 `develop/output/build_config.env`（已 gitignore）。运行 `python develop/output/build_package.py` 后依次：更新版本号 → 编译 JAR（Windows 专用，排除其他平台） → 编译 APK → 组装启动器（含自动 jlink 生成最小 JRE）→ Inno Setup 打包 → 输出到 `develop/output/`。

> **注意**：Android SDK 路径在项目根目录 `local.properties` 中配置（格式：`sdk.dir=D\:/Android/Sdk`），此文件已 gitignore，新成员需自行创建。

---

### UiManager 拆分（5600 行 → 3 个子管理器）

- **ImageManager**：提取图片相关的全部逻辑（loadImageKind、createImage、addImage、updateImage、show/hide、delete、位置/大小操作），通过 `imageMap`/`imageKindMap`/`imageKindNameMap`/`imageStateMap` 管理（commit 983d0f7）
- **LabelManager**：提取标签相关逻辑（loadLabelKind、createLabel、addLabel、updateLabel 等），通过 `labelMap`/`labelKindMap`/`labelKindNameMap`/`labelStateMap`/`labelBaseTextMap` 管理（commit 983d0f7）
- **ButtonManager**：提取按钮相关逻辑（loadButtonKind、createButton、addButton、updateButton、点击回调等），通过 `buttonMap`/`buttonKindMap`/`buttonKindNameMap`/`buttonStateMap`/`buttonBaseTextMap`/`buttonClickCallbackMap` 管理（commit 983d0f7）
- **兼容性**：三个管理器通过 `compatibilityMap` 保留旧的外部直接访问 `imageMap`/`labelMap`/`buttonMap` 的路径，拆分解耦后零调用点改动（commit 983d0f7）
- 内部类 `CustomImage`/`CustomLabel`/`CustomTextButton` 改为 `static final class`，使外部类在包内可访问（commit 983d0f7）

### 桌面端打包脚本入库

- **`lwjgl3/setup/` 目录纳入版本控制**：包含了 `inno_setup.iss`、`launcher.py`、`launcher.spec`、`setup.ico`、`console.ico`（commit 983d0f7）
- **`inno_setup.iss` 便携化**：移除 `D:\File\idea\...` 硬编码路径，全部改为相对路径；`OutputDir=.\dist`、`SetupIconFile=.\setup.ico`、`PrivilegesRequired=lowest`（commit 983d0f7）
- **`.gitignore` 新增规则**：排除 `lwjgl3/setup/build/`、`lwjgl3/setup/dist/`、`lwjgl3/setup/qingfeng_setup_windows.exe` 等构建产物（commit 983d0f7）

### 便携打包脚本 `develop/output/build_package.bat`

- **环境自动检测 + 持久化**：`build_config.env` 缓存 JDK 和 Inno Setup 路径，支持自动检测 + 手动选择弹窗（commit 983d0f7）
- **运行时目录自举**：自动创建 `dist/launcher/lib/jar/`、自动生成 `set.json`、自动 `jlink` 生成最小 JRE（约 40MB，包含 java.base/java.desktop/java.logging 等模块）（commit 983d0f7）
- **6 步构建流程**：① 更新版本号 → ② `lwjgl3:jar` → ③ `android:assembleRelease` → ④ 组装启动器 → ⑤ Inno Setup 打包 → ⑥ 复制成品到 `develop/output/`（commit 983d0f7）
- **成品命名**：`qing-feng_setup_android_v{ver}-{type}.apk` + `qing-feng_setup_windows_v{ver}-{type}.exe`（commit 983d0f7）
- **配置文件模板**：`develop/output/build_config.env.template` 已入库，供参考（commit 983d0f7）

### Python 版打包脚本（替代 .bat）

`develop/output/build_package.py` 是同功能 Python 版本，完全避免 cmd.exe 编码问题：

- **直接运行**：`python develop/output/build_package.py`（commit 983d0f7）
- **打包为 .exe**：`pyinstaller --onefile --console develop/output/build_package.py`（commit 983d0f7）
- 自动检测 JDK/Inno Setup/Android SDK/PyInstaller，结果持久化到 `build_config.env`（commit 983d0f7）
- 支持 `--config-only` 参数仅检测环境不打包（commit 983d0f7）

> 建议优先使用 Python 版。`.bat` 版保留但限于 Windows cmd.exe 在 UTF-8 BOM + 中文环境下有已知解析 bug。

#### 修复：gradlew.bat 尾部文本损坏导致 Windows 构建崩溃

`gradlew.bat` 末尾 `:omega` 标签后残留了多余文本 `\r`（字面反斜杠 + r），导致 cmd.exe 将其解析为命令执行，报错 `文件名、目录名或卷标语法不正确`。任何通过 `gradlew.bat` 的 Gradle 构建（包括 Python 脚本的 `subprocess` 调用）均受影响。已删除尾部垃圾字符。

#### 修复：build_package.py 非交互模式 EOFError

打包成功后的 `input("按 Enter 键退出...")` 在 CI/后台等非 TTY 环境抛 `EOFError`，导致脚本以非零退出码结束。已加 `try/except (EOFError, OSError)` 保护。

#### 修复：发布类型提示在部分 Windows 终端显示异常

提示文字中使用 `/` 分隔选项（`beta / alpha / release`）在部分 Windows 终端中渲染为 "betaherelease"。已改为中文顿号分隔。

#### 新增：LICENSE 加入 Android APK 打包

LICENSE 复制到 `assets/` 目录，Android 构建时自动打包进 APK，桌面端同步可用。

#### 移除：淘汰 build_package.bat

Python 版已稳定，`.bat` 版因 Windows cmd.exe 在 UTF-8 BOM + 中文环境下的解析 bug 不再维护，已删除。

---

**GameController 委托方法消除，直调 GameSessionManager**（commit 983d0f7）

- **消除 GameController 的 5 个委托包装方法**：移除 `loadGame`、`enterGame`、`quitGame`、`isInGame`、`playNewStory`，改为直接暴露 `getGameSessionManager()` getter（commit 983d0f7）
- **全部调用点更新**（8 文件）：`GameRole.java`、`GameMenu.java`、`MenuLoad.java`、`MenuList.java`、`ControllerInputHandler.java`、`UniversalInputHandlerFunction.java`、`VirtualInputHandler.java` 统一改为 `gameController.getGameSessionManager().xxx()` 模式（commit 983d0f7）
- **清理未使用导入**：`GameController.java` 移除 `FileHandle`、`Role`、`Hoster` 三个不再需要的 import（commit 983d0f7）

### UI 架构改进

- **Layout Group 支持（a）**：`addLayout` 现在将同一布局的所有 Actor 归入一个 scene2d `Group`，通过 `layoutGroupMap` 跟踪。`showLayout`/`hideLayout` 直接调用 `group.setVisible()`（O(1)），不再逐元素迭代。`deleteLayout`/`deleteAllObject`/`dispose` 同步清理 Group。未通过 `addLayout` 添加的元素仍然兼容旧逐元素路径（commit 7e791e2）
- **按钮点击回调（b）**：新增 `setButtonClickCallback(tag, Runnable)` 方法，在按钮点击时同时触发回调 + 保留 `isButtonClicked()` 状态标记。`createButton` 的 clickRunnable 增加回调调用，`deleteButton` 同步清理回调映射。现有轮询代码无需改动，逐步迁移即可（commit 7e791e2）

### 编码修复

- **修复 LWJGL3 窗口标题中文乱码**：`lwjgl3/build.gradle` 缺少 UTF-8 编译编码配置，`setTitle("氢风")` 在 Windows 默认 GBK 编码下编译产生乱码。将 `compileJava.options.encoding = 'UTF-8'` 提升到根 `build.gradle` 的 `configure(subprojects...)` 块中，对所有非 Android 子项目生效；同步添加（commit 983d0f7）
  `compileTestJava.options.encoding = 'UTF-8'`；移除 `core/build.gradle` 中重复的局部配置

### 窗口聚焦优化

- **文件选择器关闭后自动聚焦游戏窗口**：`FileChooser` 新增 `setWindowFocusRequester` 注入回调，在 `onFileChosen`/`onCancellation`/`onError` 三种结束路径均通过 `Gdx.app.postRunnable` 触发窗口聚焦（commit 983d0f7）
- **LWJGL3 端注入 GLFW 聚焦**：`Lwjgl3Launcher.getDefaultConfiguration()` 中注入实现，通过 `((Lwjgl3Application) Gdx.app).getWindow().focusWindow()` 将游戏窗口调到前台（commit 983d0f7）

---

**日志标签统一、全量 Javadoc 与方法注释补全**（commit 983d0f7）

### 日志标签统一（25+ 文件）

- 消除所有带 `Imp` 后缀的日志标签：`UserConfigManagerImp`、`ThemeManagerImp`、`TextManagerImp`、`LanguageManagerImp`、`GameInfoManagerImp`、`GameTemplateManagerImp`、`GameStoryManagerImp`、`GameRoleManagerImp` 等 → 对应类名（commit 983d0f7）
- 修复错误类名标签：`UserGameConfigManagerImp` → `GameUserConfigManager`；`JsonServiceImp` → `JsonUtils`；`gameRoleManagerImp`（小写）→ `GameRoleManager`（commit 983d0f7）
- 修复跨类误用标签：`UiManager.java` 中 `LogUtils.error("GameStateServiceImp", ...)` → `"UiManager"`（commit 983d0f7）
- 修复 `GraphicsManager.java` 中 `LogUtils.error` 参数顺序颠倒（tag/message 互换）的 bug（commit 983d0f7）

### Bug 修复

- **GamePlay.java 空指针修复**：`layout` 字段在 `init()` 中从未初始化，`render()` 和 `dispose()` 使用时始终为 null。改为从 `gameStateDataContainer.getLayoutConfig()` 获取（commit 7e791e2）
- **硬编码字符串 → 常量引用**：`ConfigBasic.java` 中的 `"back"` → `UniversalKey.BUTTON_BACK`；`GameMenu.java` 中的 `"start"/"quit"` → `RequirementUiKey.MENU_MAIN_BUTTON_START` / `UniversalKey.BUTTON_QUIT`；`GameRole.java` 中的 `"back"` → `UniversalKey.BUTTON_BACK`（commit 7e791e2）

### 全量 Javadoc 与方法注释补全

- 为全部 ~92 个 Java 源文件的公开 API 方法添加 `/** */` Javadoc（含 `@param`、`@return`）（commit 983d0f7）
- 为全部私有方法添加 `/** */` 功能描述注释（commit 983d0f7）
- 覆盖范围：8 个 GameRender 实现、6 个核心 Controller、7 个 Manager、4 个 GameManager、5 个 GameLogic 类、AudioManager（992 行）、GraphicsManager（836 行）、UiManager（4221 行 / ~180 方法）、MessageBox、20 个 Event/Handler/Interact 类、36 个 Data/Define/Parser 类（commit 983d0f7）

### 命名规范

- `FileUtils.java`：`directoryStructure` → `DIRECTORY_STRUCTURE`（static final 常量 UPPER_SNAKE）（commit 983d0f7）
- `LogUtils.java`：`fileDateFormat` → `FILE_DATE_FORMAT`（commit 983d0f7）

---

**UpdateController 重构与 deltaTime 统一**（commit 983d0f7）

### UpdateController 重构

- **消除重复代码**：移除与 `parseVersion()` 逻辑重复的 `parseVersionOrThrow()`，`compare()` 改为直接调用 `parseVersion()`（commit 983d0f7）
- **网络版本检测归位**：将散落在 `Init.java` 中的 HTTP 版本检测逻辑（270+ 行，含递归重试）移入 `UpdateController`，新增 `checkWebVersion()` / `requestWebVersion()` 方法。`Init.java` 的 `initStop()` 简化为一行 `updateController.checkWebVersion()`（commit 983d0f7）
- **命名与日志修正**：`InternalVersionFilePath` → `internalVersionFilePath`；日志标签 `"UpdateControllerImp"` → `"UpdateController"`；`dispose()` 去除无意义的 try/catch 空壳（commit 983d0f7）

### deltaTime 传递链路统一

- **GameRenderer**：`updateFrame()` 和 `render()` 改为从参数接收 `float deltaTime`，不再各自内部调用 `Gdx.graphics.getDeltaTime()`（commit 983d0f7）
- **GameController**：`run()` 改为 `run(float deltaTime)`，向下游传递（commit 983d0f7）
- **Main**：`render()` 中在 `mainRender(deltaTime)` 入口处取一次 `Gdx.graphics.getDeltaTime()`，逐层传入 `gameController.run(deltaTime)` 和 `stage.act(deltaTime)`。delta 源头统一为一处，为后续帧率控制/暂停时间缩放做准备（commit 983d0f7）

### 文档增强

- 大幅增强 `develop/REVIEW.md`：P0-P4 每条展开为完整实现指引表格，新增架构审查、代码质量问题、资源映射、技术对比等章节（commit 983d0f7）

---

**UI 样式系统增强与多 bug 修复**（commit 983d0f7）

### UiManager 样式运行时更新支持

- `updateImage`、`updateLabel`、`updateButton` 由 `private` 提升为 `public`，支持在 UI 创建后动态切换样式（kind）、位置、大小（commit 983d0f7）
- 新增 `imageKindNameMap`、`labelKindNameMap`、`buttonKindNameMap` 追踪各元素的当前样式名，确保 `deleteAllObject` 时完整清理（commit 983d0f7）
- `updateImage` 新增 `TextureRegionDrawable` 热切换：修改 kind 后即时更新显示的纹理区域（commit 983d0f7）
- `updateLabel` 新增对齐标志（`fontFlag`: W/E/N/S 及 _TYPING 变体）、内边距支持（`padX/padY/pad`）、字体颜色、背景图切换（commit 983d0f7）
- `updateButton` 新增样式（`up/down/disabled/over`）、文字、字体、颜色热切换（commit 983d0f7）
- `CustomTextButton` 新增 `getButtonStyle()` 公开方法，支持按钮样式运行时修改（commit 983d0f7）

### addLayout 逻辑重构

- 图片/标签/按钮的添加逻辑改为：**无条件创建全部元素**，再根据 `getShow()` 隐藏不需要显示的。旧逻辑仅在 `show=true` 时才创建元素，导致反复 `addLayout/deleteLayout` 时元素注册不一致（commit 983d0f7）
- 修复由此引发的 `deleteLayout` 报 `"标签不存在 (tag): path"` 的误报（因之前跳过的元素从未被注册到 UiManager）（commit 983d0f7）

### Map 顺序一致性保障

- **MergeUtils**: `mergedMap`、`deepCopyMapGeneric` 的返回值由 `HashMap` 改为 `LinkedHashMap`，保证合并后的 Map 按插入顺序迭代（commit 983d0f7）
- **JsonUtils**: `jsonStringToObject` 添加 `Feature.OrderedField`，使 JSON 反序列化保持字段声明顺序（commit 983d0f7）
- **JsonEntity**: `deepCopy` 改为 `LinkedHashMap`，保持深拷贝后的字段顺序（commit 983d0f7）
- **LayoutManager**: `loadLayoutUiImage` 中的 `imageMap` 改为 `LinkedHashMap`，确保 UI 图像的 z-order 按配置顺序渲染（commit 983d0f7）

### MessageBox 遮盖层残留修复

- **修复 MessageBox 遮盖层（mask）在游戏→主菜单状态切换后残留的问题。** 根本原因：`handleAsk` 中 `onYes.run()`（触发 `quitGame` → `disposeResource` → `messageBox.dispose()` 清空 `askMap`）在 `hideAsk` 之前执行，导致 `hideAsk` 因 `askMap` 为空而跳过 `removeMaskLayer`，遮盖层永久留在 Stage 上（commit 7e791e2）
- `MessageBox.dispose()` 增加遮盖层显式移除逻辑：遍历 `showingBoxTypeStack` 并调用 `uiManager.getMaskLayer().remove()`，同时清空 `showingBoxTypeStack`、`enterButtonTagStack`、`escapeButtonTagStack`（commit 7e791e2）
- 日志验证：`removeMaskLayer 移除遮盖` → `hideAsk 移除弹窗` → `disposeResource messageBox销毁成功`，状态切换后遮盖层正确移除（commit 7e791e2）

### MenuList 游戏封面渲染优化

- `refreshGameCover()` 改为 `updateImage` + `showImage/hideImage` 模式，替代旧的 `deleteImage + addImage` 模式（commit 7e791e2）
- 消除页面切换时的封面闪烁问题，提升翻页流畅度（commit 7e791e2）

---

## 2026-05-30 — 构建配置调整

- Gradle wrapper 镜像源切换：`services.gradle.org` → `mirrors.cloud.tencent.com`，解决国内网络 SSL 握手失败问题（commit 983d0f7）
- 构建环境改用 JDK 17（系统默认 JDK 8 证书库过旧，无法验证境外 HTTPS 证书）（commit 983d0f7）
- 删除 `gradle/gradle-daemon-jvm.properties`（内含硬编码的 foojay JDK 21 下载链接），根除成员首次构建时自动连接 `api.foojay.io` 导致超时的问题（commit 983d0f7）
- 注释 `settings.gradle` 中的 `foojay-resolver-convention` 插件，彻底禁用 Gradle 自动下载 JDK 机制。原因：国内网络无法访问 `api.foojay.io`，团队成员在首次构建时卡在 JDK 下载阶段，出现 `Connection timed out: getsockopt` 错误（commit 983d0f7）
- 在 `build.gradle` 的 `subprojects.repositories` 块中新增阿里云镜像 `https://maven.aliyun.com/repository/public/`，解决子项目依赖（gdx、gdx-platform、gdx-controllers 等）下载超时问题。此前阿里云镜像仅配置在 `buildscript.repositories` 中，只对 Gradle 插件生效，子项目依赖仍走 `mavenCentral()` 境外源（commit 983d0f7）
- 修改 `gradlew.bat`，新增 JDK 17+ 自动检测与下载功能：运行 Gradle 前检测系统 JDK 版本，如果低于 17 则自动从阿里云 Adoptium 镜像下载 JDK 17 到 `.jdk/` 目录并设置 `JAVA_HOME`。新成员克隆后直接 `./gradlew lwjgl3:run` 即可，零手动配置（commit 983d0f7）
- `.gitignore` 添加 `.jdk/` 忽略规则，防止自动下载的 JDK 被提交到仓库（commit 983d0f7）

---

## 2026-05-29 — 代码规范统一与架构优化

- 拆解上帝类 `GameController`，将数据加载、资源加载、会话管理、用户配置加载拆分为独立类（commit 983d0f7）
- 新增 `GameDataLoader`、`GameResourceLoader`、`GameSessionManager`、`GameUserConfigLoader`（commit 983d0f7）
- 移除 `RenderInstanceContent`，将渲染注册逻辑整合到 `InstanceContent`（commit 983d0f7）
- 优化类名与常量命名规范（`GameSonState` → `GameSubState` 等）（commit 983d0f7）
- 事件数据处理优化：简化 `EventPopGameState`、`EventResetGameState`，规范 `EventPushGameState`、`EventSetGameState`（commit 983d0f7）
- 代码风格统一优化（3 轮）：对齐 Allman 风格、修饰符顺序、导入顺序、日志格式等（commit 983d0f7）
- 完善 `CODING_STYLE.md` 代码规范文档（commit 983d0f7）
- 新增 `README.md` 项目说明文档（commit 983d0f7）

---

## 2026-05-25 — 代码质量改进

- 修复多处潜在漏洞（commit 983d0f7）
- 添加 `@Nullable`、`@Override` 等注解（commit 983d0f7）
- 增加 `final` 修饰词，强化不可变性（commit 983d0f7）
- 新增 `ColorConfig`、`PictureInfo`、`GifInfo`、`ButtonInfo`、`ImageInfo`、`LabelInfo` 数据类（commit 983d0f7）
- 调整包结构：`eventAction`、`game` 相关类移动到 `data` 包；`box` 相关类移动到 `engine` 包（commit 983d0f7）
- 优化 `GameInfoKey` 常量定义（54 处变更）（commit 983d0f7）

---

## 2026-05-24 — 功能完善与工具升级

- 修正 `playNewStory` 流程，增加 `TreeStructure` 和 `Page` 的 setter 校验返回值（commit 983d0f7）
- Android Gradle 插件升级 8.12.0 → 8.13.0（commit 983d0f7）
- Gradle wrapper 工具版本升级（commit 983d0f7）

---

## 2026-05-16 — 构建配置调整

- JDK toolchain 版本修改（commit 983d0f7）
- 新增 `gradle-daemon-jvm.properties`（commit 983d0f7）

---

## 2026-05-12 — 故事系统完善

- 完善 `GameStoryManagerImp`，重构故事管理器实现（178 行变更）（commit 983d0f7）
- 新增 `Page`、`TextObject` 数据类（commit 983d0f7）
- 完善 `TreeStructureInfo`，增强故事树节点信息（commit 983d0f7）
- 新增 `GameInfoManager` 接口与实现（commit 983d0f7）
- `Main` 主函数增加崩溃弹窗显示，方便用户求助（`CrashDialogShower`）（commit 983d0f7）
- 优化 `MessageBoxImp`、`UiManagerImp`，重构消息框与 UI 管理器（commit 983d0f7）
- 重构 `Init` 渲染器（112 行变更）（commit 983d0f7）
- 新增 Android 崩溃对话框支持（`AndroidLauncher`）（commit 983d0f7）
- 扩展 `FileName`、`PathName`、`PathType` 路径常量定义（commit 983d0f7）

---

## 2026-05-10 — 剧情树与多人游戏准备

- 完成剧情树的区块加载方法（`RootStructure` / `BranchStructure` / `NodeStructure` / `LeafStructure`）（commit 983d0f7）
- 提取 `PlayerData`、`GameDataContent` 为多人游戏做准备（commit 983d0f7）
- 新增文件选择器（`FileChooser`）和资源管理器打开工具（`FileExplorer`）（commit 983d0f7）
- 重构故事树：删除旧的 `Root` / `Node` / `Branch` 类，替换为 `imp` 包下的结构化实现（commit 983d0f7）
- 新增 `GameInfoManager`、`FilePathConfig`（commit 983d0f7）
- 配置文件统一命名：`theme_config.json`、`language_config.json`、`game_config.json` 等（commit 983d0f7）
- Android 端新增 `AndroidExplorerOpener` 实现（commit 983d0f7）
- 桌面端新增 `DesktopExplorerOpener` 实现（commit 983d0f7）

---

## 2026-05-07 — 渲染性能优化

- **GraphicsManager**：大纹理优化，重构纹理管理（475 行变更，+360/-115）（commit 983d0f7）
- **UiManager**：废弃旧隔离纹理模式，大纹理优化显著提升渲染性能（528 行变更，+370/-210）（commit 983d0f7）
- 修正官网下载链接，添加发行版本下载入口（commit 983d0f7）
- 新增多语言下载提示字段（commit 983d0f7）

---

## 2026-05-06 — 仓库合并

- **源码仓库与官网仓库合并**，统一管理（commit 983d0f7）
- 移动网站部署目录（commit 983d0f7）

---

## 2026-04-23 — 网站功能完善

- 修复历史版本列表无法下载的 bug（commit 983d0f7）
- 添加讨论区链接（commit 983d0f7）
- 增加更多下载途径：GitHub、蓝奏云（commit 983d0f7）

---

## 2026-04-22 — 多语言支持

- 添加官网多语言支持（中/英/日/韩/俄/德/法/葡）（commit 983d0f7）
- 修正 README.md（commit 983d0f7）

---

## 2026-04-21 — 官网重构

- 优化 HTML 代码，图片与下载链接 JSON 配置化（commit 983d0f7）
- 整理仓库结构（commit 983d0f7）

---

## 2026-03 — 网站初期建设

- 新增字符型版本字段（2026-03-31）（commit 983d0f7）
- 网站小更新（2026-03-16）（commit 983d0f7）
- 网站图标更新（2026-03-05）（commit 983d0f7）

---

## 2026-02-28 — 初始提交

- 项目初始提交，基于 libGDX 框架搭建跨平台工程结构（commit 983d0f7）
