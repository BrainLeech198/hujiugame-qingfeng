# 引擎分离架构设计方案（方案 C: Pragmatic Layered）

> **文档定位**：引擎分离的完整设计方案，把 qingfeng 代码拆成可复用的 engine 层和业务 platform 层。
>
> **文档结构**：8 个设计节 + 迁移策略 + 3D 扩展路径
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
3. 引擎服务层（SceneManager、ScriptEngine、AnimationManager）本身平台无关，只需移包
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
│   │   │   持有 Graphics / Audio / Input / Asset / Animation / Script / UI / Event
│   │   │   提供 getGraphics() / getAudio() / ... 统一访问
│   │   │   dispose() 按依赖顺序释放
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
│   │   │   内部协调 TransitionManager
│   │   │
│   │   └── SceneRegistry               ← 场景工厂注册（原 GameRenderRegistry 泛化）
│   │
│   ├── animation/                       ← 动画子系统
│   │   ├── AnimationEngine              ← 动画执行引擎（TaskStack + 帧预算 + 指令解析）
│   │   ├── TransitionManager            ← 场景过渡（双事件模型：空壳→执行）
│   │   ├── Tween                        ← 补间动画（位移/缩放/旋转/透明度/颜色）
│   │   └── command/                     ← 动画指令（AnimationCommand 系列）
│   │
│   ├── script/                          ← 脚本子系统
│   │   ├── ScriptEngine                 ← 执行引擎（TaskStack + 帧预算 + 控制流 + 变量）
│   │   ├── CommandRegistry              ← 命令注册表（平台层注册具体命令）
│   │   ├── ScriptTask / TriggerTask     ← 任务模型
│   │   └── ScriptContent                ← 脚本上下文接口（由平台层实现注入）
│   │
│   ├── ui/                              ← UI 框架
│   │   ├── UiManager                    ← 控件管理器（查找/聚焦/事件分发）
│   │   ├── kind/                        ← 控件类型
│   │   │   ├── UiObject                ← 控件基类
│   │   │   ├── Button / Label / Image / Text / Graphics / MessageBox
│   │   │   └── InteractableObject      ← 可交互控件基类
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

帧循环由 Application 统一驱动：

```
create()
  ├── EventBus.init()
  ├── Graphics.init() / Audio.init() / Input.init()
  ├── AssetManager.init(Graphics)
  ├── AnimationEngine.init(EventBus)
  ├── ScriptEngine.init(EventBus, CommandRegistry)
  ├── UiManager.init(Graphics, Input, EventBus)
  └── SceneManager.init(AnimationEngine, EventBus)

每帧:
  update(delta)
  ├── EventBus.flush()                    ← 先处理上帧积压事件
  ├── SceneManager.update(delta)          ← 场景逻辑（含过渡状态机推进）
  │   ├── TransitionManager.update(delta) ← 如果在过渡中，推进动画
  │   └── currentScene.update(delta)      ← 正常场景更新
  └── Input.update(delta)                 ← 输入状态刷新

  render(delta)
  ├── Graphics.beginFrame()
  ├── SceneManager.render(delta)          ← 场景绘制
  │   ├── currentScene.render(delta)      ← 正常渲染
  │   └── TransitionManager.render(delta) ← 过渡叠加层
  ├── UiManager.render(delta)             ← UI 绘制（在场景之上）
  └── Graphics.endFrame()

dispose()
  ├── SceneManager.dispose()   ← 先释放当前场景
  ├── ScriptEngine.dispose()
  ├── AnimationEngine.dispose()
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

    /** 立即派发（同步，当前帧处理） */
    public <T extends Event> void publish (T event);

    /** 延迟派发（入队，下一帧 update 开头 flush） */
    public <T extends Event> void queue (T event);

    /** 处理所有延迟事件 */
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

与当前代码的映射：

| 当前 | 重构后 |
|------|--------|
| `eventQueue.addEvent(new PushGameState(...))` | `eventBus.publish(new SceneTransitionEvent(PUSH, state))` |
| `EventDispatcher.handleEventOfPushGameState()` | `SceneManager.onTransitionRequest(event)` |
| `EventDispatcher.handleEventOfEnterGame()` | `GameSessionManager 自行 subscribe` |

---

## 第四节：SceneManager + TransitionManager 协调

当前问题：TransitionManager 状态机分散在 EventDispatcher（启动）和场景文件（推进）之间，职责不清。

重构后：SceneManager 统一管理场景栈 + 过渡

```java
public final class SceneManager
{
    private final Deque<Scene> sceneStack = new ArrayDeque<>();
    private final TransitionManager transitionManager;
    private final EventBus eventBus;
    private final SceneRegistry sceneRegistry;

    // ========== 场景操作（公开 API）==========

    /** 压入新场景 */
    public void push (GameState state);

    /** 弹出当前场景 */
    public void pop ();

    /** 替换当前场景 */
    public void set (GameState state);

    /** 重置到栈底 */
    public void reset ();

    // ========== 帧循环（由 Application 调用）==========

    public void update (float delta);
    public void render (float delta);

    // ========== 内部逻辑 ==========

    /** 处理场景切换请求 */
    private void handleTransition (TransitionIntent intent)
    {
        // 1. 记录意图
        transitionManager.setIntent(intent);

        // 2. 启动过渡（内部驱动淡出动画）
        transitionManager.startOutTransition(currentScene, delta);
    }

    /** 过渡完成回调 */
    void onTransitionComplete ()
    {
        TransitionIntent intent = transitionManager.getIntent();

        // 3. 执行真正的场景切换
        switch (intent.getAction())
        {
            case PUSH:
                Scene newScene = sceneRegistry.create(intent.getTargetState());
                sceneStack.push(newScene);
                newScene.create();
                break;
            case POP:
                sceneStack.pop().dispose();
                break;
            case SET:
                sceneStack.pop().dispose();
                Scene replacement = sceneRegistry.create(intent.getTargetState());
                sceneStack.push(replacement);
                replacement.create();
                break;
            case RESET:
                while (sceneStack.size() > 1) sceneStack.pop().dispose();
                break;
        }

        // 4. 启动淡入动画
        transitionManager.startInTransition(currentScene);
    }
}
```

TransitionManager 状态机（内部驱动，不再依赖场景文件推进）：

```
IDLE
  ↓ 收到 TransitionIntent
OUT_TRANSITIONING
  ↓ 淡出完成（由 AnimationEngine 驱动）
EXECUTING
  ↓ 调用 SceneManager.onTransitionComplete()
IN_TRANSITIONING
  ↓ 淡入完成
IDLE
```

关键改进：过渡动画完全由 TransitionManager 内部驱动，不再要求场景文件调 initFadeOut/fadingOut。场景只需实现 update/render，过渡是 SceneManager 的事。

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
// 引擎层：通用控件管理
public final class UiManager
{
    private final Map<String, UiObject> widgets;     // 控件存储
    private final List<InteractableObject> interactables; // 可交互控件

    // 控件生命周期
    public void addWidget (UiObject widget);
    public void removeWidget (String tag);
    public UiObject getWidget (String tag);

    // 交互管理
    public void focus (InteractableObject obj);
    public InteractableObject getFocused ();

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
AnimationEngine ←── Graphics + EventBus        │
    ↑                                          │
ScriptEngine ←── EventBus + CommandRegistry    │
    ↑                                          │
SceneManager ←── AnimationEngine + EventBus ───┘
```

无循环依赖。初始化顺序：EventBus → Graphics/Audio/Input → AssetManager → AnimationEngine → ScriptEngine → UiManager → SceneManager。

EngineContext 持有所有子系统引用，构造时按序初始化：

```java
public final class EngineContext
{
    private final EventBus eventBus;
    private final Graphics graphics;
    private final Audio audio;
    private final Input input;
    private final AssetManager assetManager;
    private final AnimationEngine animationEngine;
    private final ScriptEngine scriptEngine;
    private final UiManager uiManager;
    private final SceneManager sceneManager;

    public EngineContext (EngineConfig config)
    {
        // 按依赖顺序初始化
        this.eventBus = new EventBus();
        this.graphics = new LibGDXGraphics(config);
        this.audio = new LibGDXAudio();
        this.input = new LibGDXInput();
        this.assetManager = new AssetManager(graphics);
        this.animationEngine = new AnimationEngine(eventBus);
        this.scriptEngine = new ScriptEngine(eventBus);
        this.uiManager = new UiManager(graphics, input, eventBus);
        this.sceneManager = new SceneManager(animationEngine, eventBus);
    }

    public void dispose ()
    {
        // 按依赖逆序释放
        sceneManager.dispose();
        uiManager.dispose();
        scriptEngine.dispose();
        animationEngine.dispose();
        assetManager.dispose();
        graphics.dispose();
        audio.dispose();
        eventBus.dispose();
    }
}
```

---

## 第八节：InstanceContent 瘦身路径

当前 InstanceContent 是 God Object（30+ 字段）。重构后逐步瘦身：

| 阶段 | InstanceContent 持有 | 说明 |
|------|---------------------|------|
| 现在 | EngineContext + GameHost + UpdateChecker + 所有 Manager | God Object |
| Phase 1 | EngineContext + GameHost + UpdateChecker + ConfigService | 引擎子系统移入 EngineContext |
| Phase 2 | EngineContext + GameHost + UpdateChecker | ConfigService 移入 GameHost |
| Phase 3 | 只做启动引导，运行时无状态 | GameHost 持有 EngineContext |

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

## 渐进迁移策略（5 阶段）

| 阶段 | 内容 | 风险 | 预计改动量 |
|------|------|------|-----------|
| Phase 1 | 创建 engine 包骨架，定义接口（Scene/EventBus/Graphics/Audio/Input），不改现有代码 | 低 | 新增 ~15 文件 |
| Phase 2 | 实现 libGDX 适配层（LibGDXGraphics 等），封装现有 Manager 为引擎实现 | 中 | 改动 ~10 文件 |
| Phase 3 | Application 主循环替换 Main.render，SceneManager 替换 SceneStack | 高 | 改动 ~20 文件 |
| Phase 4 | UiManager 拆分 + AssetManager 引入 + ScriptEngine 迁移 | 中 | 改动 ~15 文件 |
| Phase 5 | InstanceContent 瘦身 + 平台层重组 | 低 | 改动 ~10 文件 |

每个 Phase 独立可运行，不破坏现有功能。

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
