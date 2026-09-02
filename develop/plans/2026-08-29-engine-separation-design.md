# 引擎分离架构设计方案（方案 C: Pragmatic Layered）

> **文档定位**：引擎分离的完整设计方案，把 qingfeng 代码拆成可复用的 engine 层和业务 platform 层。
>
> **文档结构**：8 个设计节（含 ActiveContext + Widget 体系 + 13→4 事件映射）+ 6 阶段迁移策略 + 3D 扩展路径
>
> **更新规范**：
> 1. 设计变更需同步更新对应章节
> 2. 实现过程中发现的偏差记录在末尾「实现偏差」节

---

## 方案选型

三种思路对比：

| 维度 | A 薄适配 | B 领域模型 | C 务实分层 |
|------|---------|-----------|-----------|
| 初始工作量 | 小（~500 行） | 大（~3000+ 行） | 中（~1200 行） |
| libGDX 解耦度 | 低 | 高 | 中高 |
| 3D 扩展难度 | 高（接口要重设计） | 低（已有扩展点） | 中（加方法即可） |
| 后端替换难度 | 中 | 低 | 低 |
| 过度设计风险 | 低 | 高 | 低 |
| 与现有代码适配 | 容易（概念一致） | 困难（要大改） | 中等（渐进迁移） |

**选定方案 C**，理由：
1. qingfeng 已有 SceneStack、EventQueue、EngineContext 等基础设施，分层改造是自然演进
2. 渲染和音频最可能变化（3D 扩展、音效升级），值得抽象；输入和文件系统已足够好
3. 引擎服务层（SceneManager、ScriptExecutor、AnimationManager）本身平台无关，只需移包
4. 工作量可控，可分阶段实施

核心原则：

| 类别 | 策略 | 理由 |
|------|------|------|
| 渲染绘制 | 抽象接口 | 将来 3D 要换渲染后端 |
| 音频播放 | 抽象接口 | 空间音频、混音需求 |
| 场景管理 | 抽象接口 | 2D/3D 场景生命周期一致 |
| 输入系统 | 直接暴露 | libGDX 输入系统已经跨平台 |
| 文件系统 | 直接暴露 | FileHandle 已经抽象得很好 |
| 数学类型 | 直接暴露 | 重写 Vector2/Matrix4 纯属浪费 |
| 资产类型 | 直接暴露 | Texture/Font 换后端时再包装 |

---

## 第一节：包结构与模块边界

```
com.hujiugame.qingfeng
│
├── engine/                              ← 引擎层（不依赖任何 platform 类）
│   │
│   ├── Application                      ← 主循环驱动（类比 Godot SceneTree / Cocos Director）
│   │   create → update(delta) → render(delta) → dispose
│   │   持有所有子系统引用，驱动帧循环
│   │
│   ├── core/                            ← 核心基础设施
│   │   ├── EngineContext                 ← 子系统容器（类比 Cocos Director 持有的子系统引用）
│   │   │   持有 ActiveContext（可切换） + Input / Asset / Script / Event（固定）
│   │   │   提供 getActiveContext() / getInput() / ... 统一访问
│   │   │   dispose() 按依赖顺序释放
│   │   │
│   │   ├── ActiveContext                ← 可切换 Manager 集合（Animation/Graphics/Audio/Ui）
│   │   │   启动器和游戏各一份，SM 通过 EC 访问无感知
│   │   │
│   │   ├── Event                        ← 事件基类
│   │   ├── EventBus                     ← 事件总线（发布/订阅 + 帧同步队列）
│   │   │   publish(event) / subscribe(type, handler)
│   │   │   queue(event) / flush() — 帧末统一派发
│   │   │   替代当前 EventQueue + EventDispatcher 的引擎部分
│   │   │
│   │   └── EngineConfig                 ← 引擎配置（分辨率、帧率、音频采样率等）
│   │
│   ├── graphics/                        ← 渲染子系统
│   │   ├── Graphics                     ← 渲染上下文接口（类比 libGDX Graphics / Godot RenderingServer）
│   │   │   beginFrame() / endFrame()
│   │   │   draw(Texture, x, y, w, h) / drawRegion(TextureRegion, ...)
│   │   │   drawText(Font, String, x, y)
│   │   │   setColor() / setTransform() / clear()
│   │   │
│   │   ├── Camera                       ← 相机接口（2D: Orthographic; 3D: Perspective 可扩展）
│   │   ├── Texture / TextureRegion      ← 纹理抽象
│   │   ├── SpriteBatch                  ← 精灵批处理
│   │   └── impl/libgdx/                 ← libGDX 实现
│   │       ├── LibGDXGraphics
│   │       ├── LibGDXCamera
│   │       └── LibGDXSpriteBatch
│   │
│   ├── audio/                           ← 音频子系统
│   │   ├── Audio                        ← 音频上下文接口（类比 Godot AudioServer）
│   │   │   play(Sound, volume, loop) / playMusic(Music, fadeIn)
│   │   │   stopAll() / pause() / resume()
│   │   │   setMasterVolume()
│   │   │
│   │   ├── Sound / Music                ← 音频资源抽象
│   │   └── impl/libgdx/
│   │
│   ├── input/                           ← 输入子系统
│   │   ├── Input                        ← 输入状态接口（类比 libGDX Input）
│   │   │   isKeyPressed(Key) / isTouched()
│   │   │   getTouchX/Y() / getDeltaX/Y()
│   │   │
│   │   ├── InputProcessor               ← 输入事件回调接口
│   │   └── impl/libgdx/
│   │
│   ├── asset/                           ← 资产管理子系统（类比 libGDX Assets / Bevy bevy_asset）
│   │   ├── AssetManager                 ← 异步加载 + 缓存 + 引用计数
│   │   │   load(path, type) / get(path) / unload(path)
│   │   │   finishLoading() / update() — 增量加载
│   │   │
│   │   └── AssetDescriptor              ← 资产描述符
│   │
│   ├── scene/                           ← 场景管理子系统
│   │   ├── Scene                        ← 场景接口（类比 libGDX Screen / Godot Node 生命周期）
│   │   │   create() / update(delta) / render(delta) / dispose()
│   │   │   pause() / resume() — 可选
│   │   │
│   │   ├── SceneManager                 ← 场景栈（原 SceneStack，加过渡协调）
│   │   │   push(Scene) / pop() / set(Scene) / reset()
│   │   │   getCurrentScene() / getPreviousScene()
│   │   │   持有 AnimationManager，通过 AM 驱动过渡
│   │   │
│   │   └── SceneRegistry               ← 场景工厂注册（原 GameRenderRegistry 泛化）
│   │
│   ├── animation/                       ← 动画子系统
│   │   ├── AnimationManager              ← 动画执行引擎（TaskStack + 帧预算 + 指令解析）
│   │   │   └── TransitionManager        ← 场景过渡（AM 内部持有，双事件模型：空壳→执行）
│   │   ├── Tween                        ← 补间动画（位移/缩放/旋转/透明度/颜色）
│   │   └── command/                     ← 动画指令（AnimationCommand 系列）
│   │
│   ├── script/                          ← 脚本子系统
│   │   ├── ScriptExecutor                 ← 执行引擎（TaskStack + 帧预算 + 控制流 + 变量）
│   │   ├── CommandRegistry              ← 命令注册表（平台层注册具体命令）
│   │   ├── ScriptTask / TriggerTask     ← 任务模型
│   │   └── ScriptContent                ← 脚本上下文接口（由平台层实现注入）
│   │
│   ├── ui/                              ← UI 框架
│   │   ├── UiManager                    ← 控件管理器（组合 WidgetManager）
│   │   ├── WidgetManager<W>             ← 泛型控件管理器（查找/渲染/生命周期）
│   │   ├── kind/                        ← 控件类型
│   │   │   ├── Widget                   ← 控件接口基类
│   │   │   ├── InteractableWidget       ← 可交互控件接口
│   │   │   ├── TextWidget               ← 文本控件接口
│   │   │   ├── Button / Label / Image / Text / Graphics / MessageBox
│   │   ├── VirtualInputHandler          ← 虚拟输入映射（键鼠→控件选择）
│   │   └── layout/                      ← 布局加载（JSON→控件树）
│   │       ├── LayoutLoader             ← 布局解析器
│   │       └── Layout                   ← 布局数据模型
│   │
│   ├── data/                            ← 引擎通用数据
│   │   ├── JsonEntity                   ← JSON 读取封装（通用工具，不属于 qingfeng 业务）
│   │   └── KeyRegistry                  ← 键名注册表（UiKey/ConfigKey 统一管理）
│   │
│   └── util/                            ← 引擎工具
│       ├── LogUtils
│       ├── FileUtils / SafePostRunnable
│       └── math/                        ← 数学工具（直接封装 libGDX，不重新发明）
│
└── platform/                            ← 平台层（qingfeng 业务逻辑）
    │
    ├── launcher/                        ← 启动器
    │   ├── Main                         ← 入口（实现 Application 或注册 Application）
    │   ├── InstanceContent              ← 服务定位器（逐步瘦身为 EngineContext + GameHost）
    │   ├── InitService                  ← 分帧初始化
    │   ├── ConfigService                ← 配置链（UserConfig → Theme → Language）
    │   └── UpdateChecker
    │
    ├── game/                            ← 游戏运行时
    │   ├── GameHost                     ← 游戏宿主（持有 EngineContext + PlayLocalData）
    │   ├── GameSessionManager           ← 游戏会话管理
    │   ├── scene/                       ← 平台场景实现
    │   │   ├── Init / MenuMain / MenuList / MenuLoad
    │   │   ├── GameMenu / GameRole / GamePlay
    │   │   ├── ConfigBasic / ConfigDisplay
    │   │   └── AbstractGameRender       ← 平台场景基类（持有 EngineContext + UiManager）
    │   ├── data/                        ← 游戏数据
    │   │   ├── PlayLocalData
    │   │   ├── Player
    │   │   ├── story/                   ← Story / Role / Page / PageBehavior
    │   │   └── game/                    ← GameStateDataContainer / Layout 扩展
    │   ├── manager/                     ← 游戏管理器
    │   │   ├── GameRoleManager / GameStoryManager / GameTemplateManager
    │   │   ├── GameVariableManager / GameScriptManager
    │   │   └── loader/                  ← GameResourceLoader / GamePlayDataLoader
    │   └── command/                     ← 平台脚本命令（注册到 CommandRegistry）
    │       ├── ForwardPageCommand
    │       ├── GotoPageCommand
    │       ├── ShowLayoutCommand
    │       └── ...
    │
    ├── config/                          ← 配置管理
    │   ├── GameUserConfigManager
    │   ├── LanguageManager              ← 文本解析（{language$block#key} / {game$key}）
    │   └── ThemeManager
    │
    └── type/                            ← 平台类型定义
        ├── game/                        ← GameState / GameStateEventAction / GameRenderPipeLineState
        ├── key/                         ← UiKey / ConfigKey / AnimationKey
        └── file/                        ← FileName / PathName
```

关键边界规则：

| 规则 | 说明 |
|------|------|
| engine 不依赖 platform | 引擎层不能 import 任何 platform/ 包的类 |
| platform 依赖 engine | 平台层通过接口与引擎交互 |
| util 双向可用 | 工具类不依赖业务，两层都能用 |
| libGDX 只在 engine/core 实现包 | impl/libgdx/ 子包是唯一允许 import libGDX 的地方 |
| 命令通过注册表扩展 | platform/command 通过 CommandRegistry.register() 注入，engine 不知道具体命令 |

---

## 第二节：Application 生命周期

当前缺主循环驱动。对比 Godot SceneTree 和 Cocos Director，引擎需要统一的生命周期管理：

```java
public interface Application
{
    /** 引擎启动，初始化所有子系统 */
    void create ();

    /** 每帧逻辑更新（输入处理 + 场景 update + 动画推进 + 脚本执行） */
    void update (float delta);

    /** 每帧渲染（场景 render + UI render + 过渡动画） */
    void render (float delta);

    /** 窗口尺寸变化 */
    void resize (int width, int height);

    /** 引擎关闭，按依赖逆序释放 */
    void dispose ();
}
```

帧循环由 Application 统一驱动。启动时 Main 先进入闪屏阶段（显示工作室图标 N 帧，同时让窗口管理器识别窗口），闪屏结束后才创建 Application：

> **闪屏为新增功能（二次审查 Q7）**：现状 Main 无闪屏（`slash()` 仅置标志延迟一帧，解决任务栏图标丢失）。引入 SPLASH 后：启动慢 N 帧（60fps 下 90 帧 ≈1.5s）、SPLASH 期间不响应输入、依赖 `ui/studio_logo.png`（当前**不存在**）。决策：本次做闪屏，logo 资源**先以 `//TODO` 占位**（Phase 3b 实现时补资源或留空屏），SPLASH 期间忽略输入。

```
Main.render() 启动流程：
  SPLASH 阶段（N 帧）→ 居中绘制工作室图标，窗口管理器识别窗口
  INIT 阶段（1 帧）  → lazyInit()，初始化所有子系统
  RUNNING 阶段       → Application 接管主循环

Application.create()
  ├── EventBus.init()
  ├── Graphics.init() / Audio.init()
  ├── Input.init(platformInput)   ← Q9：Input 接口由 platform 实现（封装 controller/virtual handler）并注入，engine 不硬编码实现
  ├── AssetManager.init(Graphics)
  ├── AnimationManager.init(EventBus)
  ├── ScriptExecutor.init(EventBus, CommandRegistry)
  ├── UiManager.init(Graphics, Input, EventBus)
  └── SceneManager.init(AnimationManager, EventBus)

**Init 进 SM 栈（Q10）**：初始化场景（原 Init 渲染机）作为栈底场景，在 Application.create() 时压入；InitService 由 platform 工厂创建并注入 Init 场景（保持现状对 gameHost/configService/updateChecker 的依赖），`stepInit()` 由 Init 场景 update 每帧驱动；完成后 `immediatelyTo(MENU_MAIN, PUSH)`。

每帧（帧尾 flush：场景 update 入队的切换事件当帧处理，render 前统一执行）:
  update(delta)
  ├── SceneManager.update(delta)          ← 场景逻辑（含过渡状态机推进）
  │   ├── TransitionManager.update(delta) ← 如果在过渡中，推进动画
  │   └── currentScene.update(delta)      ← 正常场景更新
  ├── Input.update(delta)                 ← 输入状态刷新（委托 platform 注入的 Input，驱动 controller/virtual handler）
  └── EventBus.flush()                    ← 帧尾统一处理积压事件（场景切换等；SM 公开 API 只在此调起，不在场景 update 栈内同步执行）

  render(delta)
  ├── Graphics.beginFrame()
  ├── SceneManager.render(delta)          ← 场景绘制
  │   ├── currentScene.render(delta)      ← 正常渲染
  │   └── TransitionManager.render(delta) ← 过渡叠加层
  ├── UiManager.render(delta)             ← UI 绘制（在场景之上）
  └── Graphics.endFrame()

dispose()
  ├── SceneManager.dispose()   ← 先释放当前场景
  ├── ScriptExecutor.dispose()
  ├── AnimationManager.dispose()
  ├── UiManager.dispose()
  ├── AssetManager.dispose()
  ├── Graphics.dispose() / Audio.dispose()
  └── EventBus.dispose()
```

当前 Main.render() 把事件处理、逻辑更新、渲染全混在一起。重构后 Application 统一调度，职责清晰。

---

## 第三节：EventBus 统一

当前事件系统有两个问题：
1. EventQueue 只管排队，EventDispatcher 只管分发，两者职责割裂
2. EventDispatcher 混杂了引擎事件（场景切换）和游戏事件（进入/退出游戏）

统一后的 EventBus 设计：

```java
public final class EventBus
{
    // ========== 发布/订阅 ==========
    /** 注册事件处理器 */
    public <T extends Event> void subscribe (EventType<T> type, EventHandler<T> handler);

    /** 取消注册 */
    public <T extends Event> void unsubscribe (EventType<T> type, EventHandler<T> handler);

    /** 立即派发（同步派发，仅限必须立即生效的事件，如帧内回调链） */
    public <T extends Event> void publish (T event);

    /** 入队（当帧帧尾 flush 统一派发，场景切换等请求统一走 queue） */
    public <T extends Event> void queue (T event);

    /** 帧尾冲刷（处理积压事件，统一执行场景切换等帧间操作） */
    public void flush ();
}
```

事件分层：

| 层 | 事件类型 | 处理者 |
|----|---------|--------|
| 引擎层 | SceneTransitionEvent（PUSH/POP/SET/RESET） | SceneManager |
| 引擎层 | RecoverNormalPipelineEvent | SceneManager |
| 引擎层 | RefreshUiEvent | UiManager |
| 平台层 | EnterGameEvent | GameSessionManager |
| 平台层 | QuitGameEvent | GameSessionManager |
| 平台层 | PlayGameEvent | GameSessionManager |

引擎层事件由 SceneManager 内部订阅处理，不再暴露给外部。平台层事件由平台代码自行订阅。

### 当前事件→重构后映射（13→4）

当前 EventDispatcher 处理 13 种事件，重构后归为 4 类：

| # | 当前事件 | 处理方式 | 重构后归类 |
|---|---------|---------|-----------|
| 1 | PushGameState | 空壳事件→SM 方法调用 | SceneManager.push() |
| 2 | PopGameState | 空壳事件→SM 方法调用 | SceneManager.pop() |
| 3 | SetGameState | 空壳事件→SM 方法调用 | SceneManager.set() |
| 4 | ResetGameState | 空壳事件→SM 方法调用 | SceneManager.reset() |
| 5 | RecoverNormalPipeline | 删除（SM 状态机自动恢复） | 不再需要 |
| 6 | StartSceneTransition（空壳） | → SM 方法调用 | SceneManager.handleTransition() |
| 7 | ExecuteSceneTransition（执行） | → SceneTransitionAction | SceneManager.onTransitionComplete() |
| 8 | FadeInComplete | → SM 内部回调 | TransitionManager 内部处理 |
| 9 | FadeOutComplete | → SM 内部回调 | TransitionManager 内部处理 |
| 10 | EnterGame | 业务事件→保留 | GameSessionManager.subscribe() |
| 11 | QuitGame | 业务事件→保留 | GameSessionManager.subscribe() |
| 12 | PlayGame | 业务事件→保留 | GameSessionManager.subscribe() |
| 13 | RefreshUi | 业务事件→保留 | UiManager.subscribe() |

映射规则：
- **空壳事件（1-4, 6）**→ 场景**仍走 `eventBus.queue()` 入队**（帧尾 flush 统一执行），**SM 方法只在帧尾 flush 中被调用**，不直接在场景 update 栈内同步调用（避免"场景自 dispose 后继续运行"的 use-after-free，Q1）。SM 双 API 二选一：UI 触发走 `transitionTo`（带过渡），业务事件/初始化走 `immediatelyTo`（立即切换，强制打断进行中的过渡）
- **执行事件（7-9）**→ 内部化为 TransitionManager 回调
- **RecoverNormalPipeline（5）**→ 删除，SM 状态机自动从 FADE_IN 恢复到 NORMAL
- **业务事件（10-13）**→ 保留，由平台层自行 subscribe；**处理逻辑统一走 `eventBus.queue()` 入队，帧尾 flush 统一执行**（保持"场景切换在帧间"红线，避免帧内中途执行栈操作）

> **帧尾 flush 说明**：flush 放在 update 之后、render 之前（见第二节帧循环）。场景 update 阶段入队的事件**当帧**被处理，render 前统一执行场景切换（dispose/register）。这与现状"同帧 while 循环处理"等效，保留"切换本帧生效"语义；区别是集中到帧尾一个点执行，避免场景切换逻辑散落在帧中各处。

---

## 第四节：SceneManager + TransitionManager 协调

当前问题：TransitionManager 状态机分散在 EventDispatcher（启动）和场景文件（推进）之间，职责不清。

重构后：SceneManager 统一管理场景栈 + 过渡。**SM 持 ActiveContext**（启动器/游戏各一份），AM/TM 通过当前 ActiveContext **动态获取**，不在构造时固定持有（解决双 AM 矛盾：游戏内过渡必须用游戏的 AM/TM）。AM 内部持有 TM，SM 通过 AM 驱动过渡动画。

```java
public final class SceneManager
{
    private final Deque<GameState> sceneStack = new ArrayDeque<>();  // 枚举栈（对齐现状 SceneStack；场景对象不常驻，Q2）
    private Scene currentScene;                                      // 当前活跃场景（仅栈顶存活，对齐现状 RenderPipeline.gameRender，Q2）
    private final EngineContext engineContext;   // 通过 EC 动态取当前 ActiveContext（不固定持有某个 AM）
    private final EventBus eventBus;
    private final SceneRegistry sceneRegistry;

    // ========== 场景操作（公开 API，双 API，均带 action）==========

    /** 带过渡切换（供 UI 触发：淡出 → 切换 → 淡入）
     *  action：PUSH/POP/SET/RESET，与现状空壳事件一一对应 */
    public void transitionTo (GameState state, SceneTransitionAction action);

    /** 立即切换（供业务事件/初始化：EnterGame/QuitGame/PlayGame/InitService）
     *  硬切换无过渡，强制打断进行中的过渡（等效现状直接入队 *_EXECUTE）
     *  action：EnterGame/PlayGame → PUSH；QuitGame → RESET（清栈回主菜单，Q4/Q5） */
    public void immediatelyTo (GameState state, SceneTransitionAction action);

    // ========== 帧循环（由 Application 调用）==========

    public void update (float delta);
    public void render (float delta);

    // ========== 内部逻辑 ==========

    /** 处理带过渡的切换请求 */
    private void handleTransition (TransitionIntent intent)
    {
        TransitionManager tm = engineContext.getActiveContext().getAnimationManager().getTransitionManager();

        // 1. 记录意图
        tm.setIntent(intent);

        // 2. 启动过渡（内部驱动淡出动画）
        tm.startOutTransition(currentScene, delta);
    }

    /** 立即切换：不驱动过渡动画，直接执行栈操作（帧间安全路径内） */
    private void executeImmediately (TransitionIntent intent)
    {
        // 若有进行中的过渡，先打断（等效现状"绕过 TM 直接入队执行事件"）
        TransitionManager tm = engineContext.getActiveContext().getAnimationManager().getTransitionManager();
        tm.abortTransition();

        // 直接执行切换，不再走淡出/淡入
        executeSceneSwitch(intent);
    }

    /** 过渡完成回调 */
    void onTransitionComplete ()
    {
        TransitionManager tm = engineContext.getActiveContext().getAnimationManager().getTransitionManager();
        TransitionIntent intent = tm.getIntent();

        executeSceneSwitch(intent);

        // 启动淡入动画
        tm.startInTransition(currentScene);
    }

    /** 创建并激活场景（Q3：完整页面加载链，对齐现状 SceneStack.updateGameState，不能只 sceneRegistry.create） */
    private Scene createScene (GameState state)
    {
        loadGameLayout(state);   // isInGame 判断 + themeManager 选择 + 缓存 + firstInGame 强制重读
        loadGameConfig(state);   // 游戏配置加载
        Scene newScene = sceneRegistry.create(state);
        newScene.create();       // init_(container)：注入当前 ActiveContext 的 UiManager 等（动态取，Q8）
        currentScene = newScene;
        return newScene;
    }

    /** 销毁当前场景（场景对象不常驻，仅栈顶存活，Q2） */
    private void disposeCurrent ()
    {
        if (currentScene != null) currentScene.dispose();
        currentScene = null;
    }

    /** 执行真正的场景切换（栈操作，必须在帧间执行，不允许帧内渲染中途） */
    private void executeSceneSwitch (TransitionIntent intent)
    {
        switch (intent.getAction())
        {
            case PUSH:
                sceneStack.push(intent.getTargetState());
                disposeCurrent();
                createScene(intent.getTargetState());
                break;
            case POP:
                sceneStack.pop();              // 弹出当前 state
                disposeCurrent();
                if (!sceneStack.isEmpty())
                {
                    createScene(sceneStack.peek());   // 弹栈重建：回到的页面重新构建（现状语义，Q2）
                }
                break;
            case SET:
                disposeCurrent();
                sceneStack.pop();
                sceneStack.push(intent.getTargetState());
                createScene(intent.getTargetState());
                break;
            case RESET:
                // Q5：清空全部 + 压 MENU_MAIN（对齐现状 resetGameState = stateStack.clear() + push(MENU_MAIN)）
                while (!sceneStack.isEmpty()) sceneStack.pop();
                disposeCurrent();
                sceneStack.push(GameState.MENU_MAIN);
                createScene(GameState.MENU_MAIN);
                break;
        }
    }
}
```

TransitionManager 状态机（内部驱动，不再依赖场景文件推进）：

```
IDLE
  ↓ 收到 TransitionIntent
OUT_TRANSITIONING
  ↓ 淡出完成（由 AnimationManager 驱动）
EXECUTING
  ↓ 调用 SceneManager.onTransitionComplete()
IN_TRANSITIONING
  ↓ 淡入完成
IDLE
```

> **当前空壳阶段**：TransitionManager 无动画逻辑，OUT_TRANSITIONING/IN_TRANSITIONING 单帧立即完成，状态机退化为"IDLE → EXECUTING → IDLE"，行为与现状完全一致。以上多帧状态机为将来动画引擎接入后的形态。
>
> **空壳阶段 transitionTo 不互相拒绝（Q11）**：空壳阶段 transitionTo 立即回 IDLE，同一帧 flush 内连续切换事件**会全部执行**（与现状双事件模型一致，不崩溃）。"非 IDLE 拒绝"的过渡保护只在真动画阶段（OUT/IN 持续多帧）才实际生效，文档表述勿理解为空壳阶段也拒绝。

关键改进：
1. 过渡动画完全由 TransitionManager 内部驱动，不再要求场景文件调 initFadeOut/fadingOut
2. 场景只需实现 update/render，过渡是 SceneManager 的事
3. **过渡保护（仅限带过渡切换）**：非 IDLE 状态下，`transitionTo` 被拒绝（WARN 日志 + 静默丢弃），防止连续过渡叠加导致状态混乱
4. **立即切换不触发保护**：`immediatelyTo` 强制打断进行中的过渡后直接硬切换，保证 EnterGame/QuitGame/PlayGame 永远可执行（杜绝"过渡中被拒导致游戏无法退出"的卡死风险）
5. **帧间红线**：`executeSceneSwitch`（栈 dispose/register）只能在帧尾 flush 阶段执行，禁止在帧内渲染中途调用

---

## 第五节：AssetManager 资产管理

当前问题：资源加载分散在各 Manager 的 init() 方法中，同步阻塞，无缓存管理。

```java
public final class AssetManager
{
    /** 同步加载（阻塞直到完成） */
    public <T> T load (String path, Class<T> type);

    /** 异步加载（非阻塞，需要轮询或回调） */
    public <T> void loadAsync (String path, Class<T> type, AssetCallback<T> callback);

    /** 获取已加载的资产（未加载返回 null） */
    public <T> T get (String path, Class<T> type);

    /** 检查是否已加载 */
    public boolean isLoaded (String path);

    /** 释放资产（引用计数归零时真正释放） */
    public void unload (String path);

    /** 推进异步加载（每帧调用） */
    public boolean update ();

    /** 加载完成比例（进度条用） */
    public float getProgress ();

    /** 释放所有资产 */
    public void dispose ();
}
```

与现有代码的整合：

| 当前加载方式 | 重构后 |
|-------------|--------|
| `GraphicsManager.loadPicture(path)` 直接 new Texture | `assetManager.load(path, Texture.class)` |
| `AudioManager.loadBackgroundMusic(path)` 直接 new Music | `assetManager.load(path, Music.class)` |
| 各 Manager 的 init() 里手动加载 | 统一通过 AssetManager |

---

## 第六节：UiManager 拆分

当前 UiManager 5000+ 行，混杂了控件管理、输入处理、布局加载、文本解析。拆分方案：

```java
// 引擎层：控件类型体系
public interface Widget                    // 控件基类
{
    String getTag ();
    void render (float delta);
    void dispose ();
}

public interface InteractableWidget extends Widget  // 可交互控件
{
    boolean isFocused ();
    void setFocused (boolean focused);
    boolean isClicked ();
}

public interface TextWidget extends Widget          // 文本控件
{
    void setText (String text);
    String getText ();
}

// 引擎层：泛型控件管理器
public final class WidgetManager<W extends Widget>
{
    private final Map<String, W> widgets;

    public void add (W widget);
    public void remove (String tag);
    public W get (String tag);
    public void clear ();
    public void render (float delta);
    public void dispose ();
}

// 引擎层：UI 总管理器（组合各 WidgetManager）
public final class UiManager
{
    private final WidgetManager<Widget> allWidgets;
    private final WidgetManager<InteractableWidget> interactables;

    // 控件生命周期
    public void addWidget (Widget widget);
    public void removeWidget (String tag);
    public Widget getWidget (String tag);

    // 交互管理
    public void focus (InteractableWidget obj);
    public InteractableWidget getFocused ();

    // 按钮快捷方法（保留，因为太常用）
    public boolean isButtonClicked (String tag);
    public void showButton (String tag);
    public void hideButton (String tag);

    // 渲染
    public void render (float delta);

    // 布局操作（委托给 LayoutLoader）
    public void addLayout (Layout layout);
    public void deleteLayout (Layout layout);

    public void dispose ();
}

// 引擎层：布局加载器
public final class LayoutLoader
{
    /** 从 JSON 文件加载布局 */
    public Layout load (FileHandle file);

    /** 合并模板布局和页面布局 */
    public Layout merge (Layout template, Layout page);

    /** 将布局实例化为控件树并注册到 UiManager */
    public void instantiate (Layout layout, UiManager uiManager);
}

// 引擎层：虚拟输入处理
public final class VirtualInputHandler
{
    /** 键盘/手柄 → 控件选择映射 */
    public void update (float delta);
    public void setPrioritySelect (String tag);
}
```

平台层职责（不进 engine）：
- requirement.json 解析（i18n 文本解析）→ LanguageManager
- `{language$block#key}` 变量替换 → LanguageManager
- 主题资源路径解析 → ThemeManager

---

## 第七节：依赖管理与初始化顺序

子系统依赖关系图（箭头 = 依赖）：

```
EventBus ←─────────────────────────────────────┐
    ↑                                          │
Graphics ←── AssetManager                      │
    ↑         ↑                                │
Audio    Input                                 │
    ↑      ↑                                   │
    ├──┬───┘                                   │
    │  ↓                                       │
    │ UiManager ←── VirtualInputHandler        │
    │                                          │
AnimationManager ←── Graphics + EventBus        │
    │   └── TransitionManager（AM 内部持有）     │
    ↑                                          │
ScriptExecutor ←── EventBus + CommandRegistry   │
    ↑                                          │
SceneManager ←── EngineContext + EventBus ──────┘
    ↑         （从 EC 动态取当前 ActiveContext 的 AM/TM）
```

无循环依赖。初始化顺序：EventBus → Graphics/Audio/Input → AssetManager → AnimationManager → ScriptExecutor → UiManager → SceneManager。

### ActiveContext：可切换 Manager 集合

启动器和游戏场景可能需要不同的 Graphics/Ui 配置（如分辨率、UI 缩放）。ActiveContext 封装可切换的 Manager 子集：

```java
public final class ActiveContext
{
    private final AnimationManager animationManager;
    private final Graphics graphics;
    private final Audio audio;
    private final UiManager uiManager;

    // getter 省略
}
```

- 启动器有自己的 ActiveContext（低分辨率、简单 UI）
- 游戏有自己的 ActiveContext（游戏分辨率、完整 UI）
- **SceneManager 持 EngineContext，不固定持有某个 AM**：过渡驱动时每次动态 `engineContext.getActiveContext().getAnimationManager().getTransitionManager()`（保证游戏内过渡用游戏自己的动画数据）
- EngineContext 持有当前激活的 ActiveContext；切换时只需 `engineContext.setActiveContext(gameContext)`。SM 不缓存 AM/TM 引用，每次从 EC 动态取当前上下文，切换后自然感知
- **场景获取 UiManager（Q8）**：场景不固定持有 UiManager 字段引用（ActiveContext 切换后旧引用失效），每次从 `engineContext.getActiveContext().getUiManager()` 动态获取；配合"弹栈重建"，切换后永远拿到当前上下文的实例

### EngineContext

EngineContext 持有所有子系统引用 + ActiveContext，构造时按序初始化：

```java
public final class EngineContext
{
    private final EventBus eventBus;
    private final Input input;
    private final AssetManager assetManager;
    private final ScriptExecutor scriptExecutor;
    private final SceneManager sceneManager;

    // ActiveContext 可切换部分
    private ActiveContext activeContext;

    public EngineContext (EngineConfig config, Input platformInput)
    {
        // 按依赖顺序初始化
        this.eventBus = new EventBus();
        this.input = platformInput;   // Q9：Input 由 platform 实现注入（封装 controller/virtual handler）
        this.assetManager = new AssetManager();

        // 创建默认 ActiveContext（启动器用）
        Graphics graphics = new LibGDXGraphics(config);
        Audio audio = new LibGDXAudio();
        AnimationManager am = new AnimationManager(eventBus);
        UiManager uiManager = new UiManager(graphics, input, eventBus);
        this.activeContext = new ActiveContext(am, graphics, audio, uiManager);

        this.scriptExecutor = new ScriptExecutor(eventBus);
        // SM 持 EngineContext，过渡时动态取当前 ActiveContext 的 AM/TM（双 AM 架构：启动器/游戏各一份）
        this.sceneManager = new SceneManager(this, eventBus);
    }

    /** 切换活跃上下文（启动器→游戏时调用） */
    public void setActiveContext (ActiveContext context);

    public ActiveContext getActiveContext ();

    public void dispose ()
    {
        // 按依赖逆序释放
        sceneManager.dispose();
        scriptExecutor.dispose();
        activeContext.dispose();
        assetManager.dispose();
        eventBus.dispose();
    }
}

### dispose 责任矩阵（Q12）

引擎分离后新增了多个生命周期持有者，dispose 责任必须显式划分，避免 double-dispose / use-after-free：

| 持有者 | 释放内容 | 释放时机 | 说明 |
|--------|---------|---------|------|
| Main | Stage / SpriteBatch / studioLogo / Application | dispose() | libGDX 基础设施，Main 独占持有（Q16：topRender 共用同一 batch，不二次 dispose） |
| InstanceContent | GameHost / UpdateChecker / ConfigService 等 platform 对象 | dispose() | 现状责任不变 |
| GameHost | RenderPipeline / SceneStack / EventDispatcher / ConfigService / GameLogic / PlayLocalData / PlayRuntimeData | dispose() | 现状责任不变（Phase 3b 后 RenderPipeline/SceneStack/EventDispatcher 废弃，对应职责移 SM/EventBus） |
| EngineContext | SceneManager / ScriptExecutor / ActiveContext / AssetManager / EventBus / Input | dispose() | 按依赖逆序；Input 仅释放 platform 注入对象中它自建的部分（platform handler 不 double-dispose） |
| SceneManager | 栈内全部场景对象（currentScene，及各 state 重建时的临时对象） | dispose() | 场景对象 dispose 归 SM（SceneRegistry 只负责创建，不负责释放） |
| ActiveContext | 该上下文内的 AnimationManager / Graphics / Audio / UiManager | 上下文被替换时 + 引擎关闭时 | **游戏 ActiveContext**：随 QuitGame 清栈/退出游戏时释放（PlayLocalData 释放后 AM 无动画数据可放） |
| UiManager | 其持有的控件 / 布局 | dispose() | 每个 ActiveContext 各有一份，随所属 ActiveContext 释放 |
| EventBus | 订阅者列表 | dispose() | 清空 handler 引用，防泄漏 |

**释放顺序红线**：父容器先 dispose 依赖它的子对象，再 dispose 被依赖对象（例：SceneManager.dispose → 释放场景 → 再释放 UiManager）。**场景对象永远由 SM dispose，不由调用方 dispose**；**Input/EventBus 归 Main/EngineContext 释放，platform handler 不自行 dispose**。
```

---

## 第八节：InstanceContent 瘦身路径

当前 InstanceContent 是 God Object（30+ 字段）。重构后逐步瘦身：

| 阶段 | InstanceContent 持有 | 说明 |
|------|---------------------|------|
| 现在 | EngineContext + GameHost + UpdateChecker + 所有 Manager | God Object |
| Phase 1-2 | EngineContext + GameHost + UpdateChecker + ConfigService | 引擎子系统移入 EngineContext |
| Phase 3-4 | EngineContext + GameHost + UpdateChecker | ConfigService 移入 GameHost |
| Phase 5 | 只做启动引导，运行时无状态 | GameHost 持有 EngineContext（含 ActiveContext） |

最终 InstanceContent 只负责：

```java
public final class InstanceContent
{
    /** 启动时调用一次，创建引擎和启动器 */
    public static void init ()
    {
        EngineContext engine = new EngineContext(defaultConfig);
        GameHost gameHost = new GameHost(engine, configService);
        Application app = new Application(engine, gameHost);
        app.create();
    }
}
```

---

## 3D 扩展路径

当前设计为 2D 优化，3D 扩展只需：

| 子系统 | 2D 实现 | 3D 扩展 |
|--------|---------|---------|
| Graphics | SpriteBatch + OrthographicCamera | 加 ModelBatch + PerspectiveCamera |
| Scene | 2D 场景 | 3D 场景，Scene 接口不变 |
| Animation | 2D Tween | 加骨骼动画 / 模型动画 |
| Input | 触屏/键鼠 | 加手柄 / VR 控制器 |

Graphics 接口加 `drawModel(Model, Matrix4)` 方法，2D 实现抛 UnsupportedOperationException，3D 实现正常处理。

---

## 渐进迁移策略（6 阶段）

| 阶段 | 内容 | 风险 | 预计改动量 |
|------|------|------|-----------|
| Phase 1 | 创建 engine 包骨架，定义接口（Scene/EventBus/Graphics/Audio/Input），不改现有代码 | 低 | 新增 ~15 文件 |
| Phase 2 | 实现 libGDX 适配层（LibGDXGraphics 等），封装现有 Manager 为引擎实现 | 中 | 改动 ~10 文件 |
| Phase 3a | SceneManager 双 API 重构（只改内部实现，不改外部接口）；**TM 保持空壳不动，动画引擎解耦单独立项**（P1-4） | 中 | 改动 ~10 文件 |
| Phase 3b | Application 主循环替换 Main.render，SM 接管场景栈 | 高 | 改动 ~10 文件 |
| Phase 4 | UiManager 拆分 + AssetManager 引入 + ScriptExecutor 迁移 | 中 | 改动 ~15 文件 |
| Phase 5 | InstanceContent 瘦身 + ActiveContext 引入 + 平台层重组 | 低 | 改动 ~10 文件 |

每个 Phase 独立可运行，不破坏现有功能。

Phase 3a/3b 拆分理由：Phase 3 原来是"内部重构+主循环替换"一步到位，风险过高。拆分后 3a 只改 SM 内部（外部接口不变），3b 才替换主循环。3a 完成后现有代码仍然正常运行（TM 保持空壳，行为与现状完全一致），3b 是真正的"开关切换"。

**动画引擎单独立项**：真正淡出淡入动画执行引擎、过渡视觉（P1-8）、过渡期间场景行为（P2-15）不在本 6 阶段内，待架构重构完成后单独设计（等架构做完再设计 AM 以及 AM.TM）。

---

## 主流引擎对比参考

| 引擎 | 主循环驱动 | 子系统抽象 | 引擎/游戏分离方式 |
|------|-----------|-----------|------------------|
| libGDX | ApplicationListener | 5 个核心接口（App/Graphics/Audio/Input/Files） | 游戏实现 ApplicationListener |
| Godot | SceneTree | Server 单例（RenderingServer/AudioServer） | 游戏代码 = 脚本挂 Node |
| MonoGame | Game 类 | GraphicsDevice + ContentManager | 游戏继承 Game，重写 Init/Update/Draw |
| Cocos2d-x | Director 单例 | Renderer + Scheduler + EventDispatcher | 游戏继承 Scene/Layer |
| Bevy | App + Plugin | ECS World，一切皆 Plugin | 游戏 = 注册 Systems + Plugins |

共同模式：主循环驱动 + 子系统通过接口/单例隔离 + 游戏代码只做生命周期回调。
