# 引擎分离实施计划

> **文档定位**：引擎分离架构改造的完整实施计划，供人工审查后逐阶段执行。
>
> **文档结构**：全局总览 → 6 阶段逐阶段详述（痛点/改动/收益/风险/文件清单/步骤）
>
> **前置文档**：`develop/plans/2026-08-29-engine-separation-design.md`（设计方案）
>
> **更新规范**：
> 1. 实施过程中发现偏差时更新对应阶段
> 2. 每阶段完成后勾选检查清单

---

## 全局总览

### 当前架构痛点

| # | 痛点 | 具体表现 | 影响范围 |
|---|------|---------|---------|
| 1 | **主循环职责混杂** | `Main.render()` 把清屏/视口/懒初始化/渲染全混在一起；`GameHost.run()` 把事件处理/逻辑更新/渲染串行拼接 | Main.java 684 行，GameHost.java 373 行 |
| 2 | **事件系统割裂** | `EventQueue` 只管排队（39 行），`EventDispatcher` 只管分发（410 行），两者职责分离但耦合紧密；13 种事件类型混杂引擎事件和业务事件 | event/ 包 2 文件 |
| 3 | **场景管理分散** | `SceneStack`（569 行）管栈，`TransitionManager`（168 行）管过渡，`EventDispatcher` 管启动过渡——三处分散，职责不清 | 3 个文件交叉依赖 |
| 4 | **InstanceContent God Object** | 30+ 字段，持有从 SpriteBatch 到 LanguageManager 的一切，任何子系统都要通过它访问 | InstanceContent.java 584 行 |
| 5 | **UiManager 巨型类** | 5924 行，混杂控件管理/输入处理/布局加载/文本解析/主题切换 | 单文件 5924 行 |
| 6 | **无统一生命周期** | 缺少 `create→update→render→dispose` 的标准生命周期，各子系统初始化顺序隐含在代码中 | 全局 |
| 7 | **引擎/业务不分离** | `AnimationManager`、`SceneStack` 等平台无关代码和 `GameState`、`LanguageManager` 等业务代码混在同一个包层级 | 全局包结构 |
| 8 | **资源加载分散** | `GraphicsManager.loadPicture()`、`AudioManager.loadBackgroundMusic()` 各自直接 new 对象，无统一缓存/引用计数 | 各 Manager |

### 改动全景

```
Phase 1 ─── engine 包骨架 + 接口定义（新增 ~15 文件，不改现有代码）
   │
Phase 2 ─── libGDX 适配层（改动 ~10 文件，封装现有 Manager）
   │
Phase 3a ── SM/TM 内部重构（改动 ~10 文件，外部接口不变）
   │
Phase 3b ── Application 主循环替换 Main.render（改动 ~10 文件，高风险切换点）
   │
Phase 4 ─── UiManager 拆分 + AssetManager + ScriptExecutor 迁移（改动 ~15 文件）
   │
Phase 5 ─── InstanceContent 瘦身 + ActiveContext + 平台层重组（改动 ~10 文件）
```

### 风险等级说明

| 等级 | 含义 | 回滚策略 |
|------|------|---------|
| 低 | 只新增文件，不改现有代码 | 删除新增文件即可 |
| 中 | 改动现有文件内部实现，不改外部接口 | git revert 单个 commit |
| 高 | 替换核心流程，外部行为可能变化 | 需要 feature branch + 逐步验证 |

---

## Phase 1：engine 包骨架 + 接口定义

### 痛点

当前所有代码混在 `com.hujiugame.qingfeng` 一个包层级下，`AnimationManager`（平台无关）和 `LanguageManager`（业务相关）做邻居。没有 engine/platform 的边界，任何新功能都要面对整个包的依赖关系。

### 改动内容

**只新增文件，不改任何现有代码。** 新建 `engine/` 包，定义纯接口和数据类。

#### 新增文件清单

| # | 文件 | 包路径 | 职责 | 行数估算 |
|---|------|--------|------|---------|
| 1 | `Event.java` | `engine.core` | 事件基类，所有事件的父类型 | ~20 |
| 2 | `EventType.java` | `engine.core` | 事件类型标识（泛型 key） | ~15 |
| 3 | `EventHandler.java` | `engine.core` | 事件处理器函数式接口 | ~10 |
| 4 | `EventBus.java` | `engine.core` | 事件总线（publish/subscribe/queue/flush） | ~120 |
| 5 | `EngineConfig.java` | `engine.core` | 引擎配置（分辨率、帧率等） | ~40 |
| 6 | `EngineContext.java` | `engine.core` | 子系统容器（持有所有子系统引用） | ~100 |
| 7 | `ActiveContext.java` | `engine.core` | 可切换 Manager 集合（Animation/Graphics/Audio/Ui） | ~50 |
| 8 | `Scene.java` | `engine.scene` | 场景接口（create/update/render/dispose） | ~30 |
| 9 | `SceneManager.java` | `engine.scene` | 场景栈 + 过渡协调（接口+骨架实现） | ~80 |
| 10 | `SceneRegistry.java` | `engine.scene` | 场景工厂注册表 | ~40 |
| 11 | `TransitionIntent.java` | `engine.scene` | 过渡意图（action + targetState） | ~30 |
| 12 | `SceneTransitionAction.java` | `engine.scene` | 过渡动作枚举（PUSH/POP/SET/RESET） | ~10 |
| 13 | `Graphics.java` | `engine.graphics` | 渲染上下文接口 | ~50 |
| 14 | `Audio.java` | `engine.audio` | 音频上下文接口 | ~40 |
| 15 | `Input.java` | `engine.input` | 输入状态接口 | ~30 |

#### 接口设计要点

```java
// Scene 接口 — 生命周期标准
public interface Scene
{
    void create ();              // 场景创建时调用一次
    void update (float delta);   // 每帧逻辑更新
    void render (float delta);   // 每帧渲染
    void dispose ();             // 场景销毁时调用一次
}

// Graphics 接口 — 渲染抽象
public interface Graphics
{
    void beginFrame ();
    void endFrame ();
    void draw (Object texture, float x, float y, float w, float h);
    void drawRegion (Object region, float x, float y, float w, float h);
    void drawText (Object font, String text, float x, float y);
    void setColor (float r, float g, float b, float a);
    void clear (float r, float g, float b, float a);
}

// EventBus 接口 — 事件系统统一
public final class EventBus
{
    public <T extends Event> void subscribe (EventType<T> type, EventHandler<T> handler);
    public <T extends Event> void unsubscribe (EventType<T> type, EventHandler<T> handler);
    public <T extends Event> void publish (T event);      // 同步派发
    public <T extends Event> void queue (T event);        // 延迟到下帧 flush
    public void flush ();                                  // 处理所有延迟事件
}
```

### 收益

| 收益 | 说明 |
|------|------|
| 零风险验证 | 不改任何现有代码，纯新增，可随时删除回滚 |
| 接口先行 | 定义清楚 engine 层的 API 契约，后续阶段有据可依 |
| 编译验证 | 接口之间的依赖关系在编译期就能发现循环依赖 |
| 文档价值 | 接口本身就是 engine 层的活文档 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 接口设计不完整，后续阶段需要改 | 低 | 接口是增量添加的，改接口不影响现有代码 |
| 包路径选错，后续要调 | 低 | `engine.core/scene/graphics/audio/input` 是业界通用结构 |
| 与现有 EngineContext.java 冲突 | 中 | 现有 `engine/EngineContext.java`（122 行）只有3个字段，新版本完全替代它 |

### 检查清单

- [ ] 所有接口编译通过（无实现体也可编译）
- [ ] engine 包不 import 任何 `platform/` 或业务类
- [ ] 接口之间无循环依赖
- [ ] Scene 生命周期方法签名完整
- [ ] EventBus 的泛型约束正确
- [ ] 现有代码编译不受影响（新包是独立的）

---

## Phase 2：libGDX 适配层

### 痛点

当前 `GraphicsManager`（渲染）、`AudioManager`（音频）直接使用 libGDX API，没有抽象层。如果将来要换渲染后端（3D 扩展、Vulkan 等），需要改动所有调用点。

### 改动内容

在 `engine/*/impl/libgdx/` 下实现 Phase 1 定义的接口，封装现有 Manager。

#### 新增文件

| # | 文件 | 包路径 | 职责 | 对应现有类 |
|---|------|--------|------|-----------|
| 1 | `LibGDXGraphics.java` | `engine.graphics.impl.libgdx` | Graphics 接口的 libGDX 实现 | 封装 `GraphicsManager` |
| 2 | `LibGDXAudio.java` | `engine.audio.impl.libgdx` | Audio 接口的 libGDX 实现 | 封装 `AudioManager` |
| 3 | `LibGDXInput.java` | `engine.input.impl.libgdx` | Input 接口的 libGDX 实现 | 封装 `Gdx.input` |

#### 改动文件

| # | 文件 | 改动内容 | 风险 |
|---|------|---------|------|
| 1 | `EngineContext.java`（新版） | 实现完整的构造函数，按依赖顺序初始化所有子系统 | 低 |
| 2 | `GraphicsManager.java` | 不改，LibGDXGraphics 内部委托给它 | 无 |
| 3 | `AudioManager.java` | 不改，LibGDXAudio 内部委托给它 | 无 |

#### 适配模式

```java
// LibGDXGraphics 示例 — 委托模式，不重写逻辑
public final class LibGDXGraphics implements Graphics
{
    private final SpriteBatch batch;
    private final Stage stage;

    public LibGDXGraphics (SpriteBatch batch, Stage stage)
    {
        this.batch = batch;
        this.stage = stage;
    }

    @Override
    public void beginFrame ()
    {
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();
    }

    @Override
    public void endFrame ()
    {
        batch.end();
        stage.draw();
    }

    // ... 其他方法委托给 batch/viewport
}
```

### 收益

| 收益 | 说明 |
|------|------|
| 渲染后端可替换 | 将来 3D 扩展只需新增 `LibGDX3DGraphics implements Graphics` |
| 测试友好 | 可以用 Mock Graphics 做单元测试 |
| 现有代码不改 | 适配层是新增代码，现有 Manager 保持不变 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 委托层增加一帧调用开销 | 低 | 方法调用开销在纳秒级，不影响帧率 |
| SpriteBatch/Stage 生命周期由谁管 | 中 | 明确：LibGDXGraphics 持有引用但不 dispose（由 Main 管） |
| Graphics 接口用 Object 类型参数 | 中 | Phase 2 先用 Object，Phase 4 引入 Texture/Font 抽象后再泛化 |

### 检查清单

- [ ] LibGDXGraphics 能正确调用 SpriteBatch 绘制
- [ ] LibGDXAudio 能正确播放音效
- [ ] LibGDXInput 能正确读取输入状态
- [ ] EngineContext 能按序初始化所有子系统
- [ ] 现有 Main.render() 流程不受影响（适配层未接入主循环）

---

## Phase 3a：SceneManager + TransitionManager 内部重构

### 痛点

当前场景切换流程分散在三处：

```
EventDispatcher.handleEventOfPushGameState()   → 启动过渡，设置 RenderPipeLineState
SceneStack 内部                                → 维护场景栈
TransitionManager                              → 空壳（commit 3952fb5 已删动画逻辑，仅保留双事件骨架）
```

问题：
1. **EventDispatcher 同时管事件分发和场景切换逻辑**，违反单一职责
2. **TransitionManager 已是空壳**（168 行）：`startTransition()` 立即执行 + 恢复渲染管线，无任何动画。本阶段**不重写动画执行引擎**，动画引擎解耦为独立阶段（见下）
3. **RenderPipeLineState 枚举**在 `GameRenderPipeLineState.java` 中定义，是平台类型，但控制的是引擎行为
4. **RecoverNormalPipelineEvent** 是个补丁事件，用来在场景切换后恢复渲染管线，说明状态管理有缺口
5. **场景切换双路径并存**：空壳路径（UI 触发 PushGameState，带过渡意图但实际无动画）+ 直接执行路径（EnterGame/QuitGame/PlayGame/InitService 用 `*_EXECUTE`，无过渡硬切换）。重构后必须**保留两种语义**（见下方"双 API"设计）

### 改动内容

**只改内部实现，不改外部接口。** 调用方仍然通过 `eventQueue.addEvent(new PushGameState(...))` 触发场景切换，但内部执行路径完全重写。

**关键决策：Phase 3a 只做架构重构，不实现动画**。TransitionManager 保持空壳（无淡出/淡入动画），动画执行引擎（真正的过渡视觉、TM 的 updateOut/updateIn）**解耦为独立阶段，待架构完成后单独设计**（关联设计文档第四节/P1-8）。因此本阶段过渡保护、双 API 是围绕"空壳 TM + 立即执行"设计的。

#### 改动文件

| # | 文件 | 改动内容 | 风险 |
|---|------|---------|------|
| 1 | `SceneManager.java`（新实现） | 实现场景栈 + 双 API（transitionTo/immediatelyTo，均带 action），**持 ActiveContext**（动态取 AM/TM，不固定持有启动器 AM）；executeSceneSwitch 内保留 loadGameLayout/loadGameConfig/init_(container) 完整加载链（Q3）；栈模型 = 枚举栈 + 仅栈顶活跃场景（弹栈重建，Q2） | 中 |
| 2 | `TransitionManager.java` | **保持空壳不动**（本阶段不重写动画引擎），仅补充 `abortTransition()` 供 immediatelyTo 打断过渡 | 低 |
| 3 | `AnimationManager.java` | 内部持有 TransitionManager，提供 `getTransitionManager()` | 低 |
| 4 | `SceneStack.java` | 保留场景栈功能，移除过渡相关逻辑 | 中 |
| 5 | `EventDispatcher.java` | Push/Pop/Set/Reset handler 改为调用 SceneManager 双 API（UI 触发 → transitionTo；业务事件 → immediatelyTo） | 中 |
| 6 | `GameRenderPipeLineState.java` | 逐步废弃，由 SceneManager.State 枚举替代 | 低 |

#### 状态机重设计

**当前**：状态分散在 EventDispatcher（启动）+ RenderPipeLineState（管线）+ TransitionManager（动画）

**重构后**：SceneManager 单一状态源

```
IDLE（NORMAL）
  ↓ 收到场景切换请求
FADE_OUT
  ↓ TM.updateOut() 返回 true（淡出完成）
WAITING_EXECUTE
  ↓ SceneManager.executeTransition()（帧间安全执行栈操作）
FADE_IN
  ↓ TM.updateIn() 返回 true（淡入完成）
IDLE（NORMAL）
```

#### 13→4 事件映射（实施细节）

**双 API 语义**：UI 触发的空壳事件 → `transitionTo`（带过渡，本阶段空壳 TM 立即执行）；业务事件/初始化（EnterGame/QuitGame/PlayGame/InitService）→ `immediatelyTo`（无过渡硬切换，强制打断进行中的过渡，等效现状直接入队 `*_EXECUTE`）。

| 当前事件 | 当前处理位置 | 重构后处理 |
|---------|------------|-----------|
| PushGameState | EventDispatcher: 设置待处理任务 + 启动 RenderPipeLineState + 启动过渡 | SceneManager.transitionTo(): 启动 TM（空壳） |
| PopGameState | EventDispatcher: 同上 | SceneManager.transitionTo(): 同上 |
| SetGameState | EventDispatcher: 同上 | SceneManager.transitionTo(): 同上 |
| ResetGameState | EventDispatcher: 同上 | SceneManager.transitionTo(): 同上 |
| RecoverNormalPipeline | EventDispatcher: 恢复 RenderPipeLineState 为 NORMAL | 删除：SM 状态机自动从 FADE_IN→IDLE |
| PushGameStateExecute | EventDispatcher: 执行真正的栈操作 | SceneManager.onTransitionComplete(): 内部方法（transitionTo 路径） |
| PopGameStateExecute | EventDispatcher: 同上 | 同上 |
| SetGameStateExecute | EventDispatcher: 同上 | 同上 |
| ResetGameStateExecute | EventDispatcher: 同上 | 同上 |
| FadeInComplete/FadeOutComplete | 各场景文件中推进 | TransitionManager 内部回调（空壳阶段无动画，此事件不再出现） |
| EnterGame | EventDispatcher: 业务逻辑 → PushGameStateExecute(GAME_MENU) | GameSessionManager.subscribe() → **immediatelyTo(GAME_MENU, PUSH)**，入队 queue 帧尾执行 |
| QuitGame | EventDispatcher: 业务逻辑 → ResetGameStateExecute | GameSessionManager.subscribe() → **immediatelyTo(MENU_MAIN, RESET)**（清栈回主菜单，等效现状 ResetGameStateExecute；Q4/Q5），入队 queue 帧尾执行 |
| PlayGame | EventDispatcher: 业务逻辑 → PushGameStateExecute(GAME_PLAY) | GameSessionManager.subscribe() → **immediatelyTo(GAME_PLAY, PUSH)**，入队 queue 帧尾执行 |
| RefreshUi | EventDispatcher: 刷新 UI + SafePostRunnable 帧间释放 | 保留，后续移给 UiManager；**必须保留 SafePostRunnable 的"下一帧释放"机制**（flush 当帧派发替代不了它） |

#### 并发场景切换请求处理（过渡进行中的新事件）

**问题**：若实现真动画后淡出/淡入持续多帧，过渡进行中（FADE_OUT 或 FADE_IN）事件队列里又来了新的切换事件，怎么处理？

**双 API 的两种行为**：
1. **`transitionTo`（带过渡，UI 触发）**：非 IDLE 状态拒绝，WARN 日志 + 静默丢弃
2. **`immediatelyTo`（立即切换，业务事件）**：**强制打断**进行中的过渡（`tm.abortTransition()`），直接执行硬切换

理由：
1. **用户操作不可能合法地在过渡中触发新切换**：UI 按钮在过渡中不可见/不可点击，键盘输入在过渡中被屏蔽 → 用 transitionTo 保护
2. **业务事件（QuitGame 等）必须永远可执行**：若 QuitGame 在过渡中被拒，游戏无法退出 → 卡死。immediatelyTo 强制打断保证退出/进入游戏路径永不失效（等效现状"绕过 TM 直接入队 *_EXECUTE"）
3. **立即切换必须同帧完成**（P0-2）：帧尾 flush 处理 QuitGame 时，immediatelyTo 直接切到 MENU_MAIN，与 quitGame() 同步释放的游戏数据同帧安全

```java
// SceneManager 中的场景切换方法（双 API，均带 action；Q4）
public void transitionTo (GameState state, SceneTransitionAction action)
{
    if (this.state != State.IDLE)
    {
        LogUtils.warn(SceneManager.class, "transitionTo 忽略：当前正在过渡中，状态=" + this.state);
        return;  // 静默丢弃（带过渡保护）
    }
    handleTransition(new TransitionIntent(action, state));
}

public void immediatelyTo (GameState state, SceneTransitionAction action)
{
    // 业务事件硬切换：强制打断过渡，不受保护
    TransitionManager tm = activeContext.getAnimationManager().getTransitionManager();
    tm.abortTransition();
    this.state = State.IDLE;
    executeSceneSwitch(new TransitionIntent(action, state));
}

// 调用方：UI 空壳事件 → transitionTo(state, PUSH/POP/SET/RESET)；业务事件 → immediatelyTo(state, PUSH/RESET)
```

**事件队列中的连续切换事件处理流程**（当前空壳 TM 无动画，FADE_OUT/FADE_IN 单帧完成）：

```
帧 N：EventBus.flush()（帧尾）处理第一个 PushGameState
  → SceneManager.transitionTo() → 空壳 TM 立即执行 → executeSceneSwitch
  → state 回到 IDLE

帧 N：flush() 继续处理队列中第二个 QuitGame（同一帧内）
  → GameSessionManager → SceneManager.immediatelyTo(MENU_MAIN)
  → state != IDLE（若上一个 transitionTo 还在 FADE_OUT）→ abortTransition 强制打断
  → 硬切换到 MENU_MAIN
```

**关键点**：`EventBus.flush()` 是一次把队列里所有事件都处理完的。**当前空壳阶段 transitionTo 之间不互相拒绝**（transitionTo 立即回 IDLE，同一帧 flush 内连续切换事件会全部执行，与现状双事件模型一致、不崩溃；"非 IDLE 拒绝"的过渡保护只在真动画阶段 OUT/IN 持续多帧时才实际生效，Q11）；但 immediatelyTo 总是能打断执行。这保证业务事件的退出路径永不失效。

#### 帧间约束（关键红线）

场景栈操作（push/pop/set/reset 的 dispose 和 create）必须在帧间执行，不能在帧内任意时刻调用。

**当前实现**：通过 `SafePostRunnable` 延迟到下一帧（RefreshUi 等场景仍保留此机制，P1-7）

**重构后**：通过 `EventBus.flush()` 在**每帧帧尾**（update 之后、render 之前）统一执行。场景 update 阶段入队的事件**当帧**被处理，render 前统一执行场景切换——与现状"同帧 while 循环处理"等效，保留"切换本帧生效"语义。

```java
// 每帧帧尾 flush（update 末尾，render 之前）
public void update (float delta)
{
    // 场景逻辑（含过渡状态机推进）
    if (state == FADE_OUT)
    {
        if (animationManager.getTransitionManager().updateOut(delta))
        {
            // 淡出完成，执行真正的场景切换
            executeTransition();
            state = FADE_IN;
        }
    }
    else if (state == FADE_IN)
    {
        if (animationManager.getTransitionManager().updateIn(delta))
        {
            // 淡入完成，恢复正常
            state = IDLE;
        }
    }
    else
    {
        currentScene.update(delta);
    }

    // 帧尾 flush：统一处理本帧入队的切换事件（场景栈操作集中在此执行）
    eventBus.flush();
}
```

> 注意：当前空壳 TM 无动画，FADE_OUT/FADE_IN 分支单帧完成，`updateOut/updateIn` 立即返回 true。此分支结构为将来动画引擎预留。**flush 位置与设计文档第二节一致（帧尾，不是帧首）**，避免"场景 update 入队的事件拖到下一帧"的时序变化。

### 收益

| 收益 | 说明 |
|------|------|
| 场景切换逻辑归一 | 从 3 处分散变为 SceneManager 单点管理 |
| 过渡与场景解耦 | 场景不再需要调 initFadeOut/fadingOut，过渡是 SM 的事 |
| 消除 RecoverNormalPipeline | 状态机自动恢复，不再需要补丁事件 |
| 为 Phase 3b 铺路 | SM 已经能独立管理场景栈和过渡，切换主循环只需替换调用入口 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 帧间约束违反导致崩溃 | **高** | 逐帧推演所有 dispose/create 调用时机，必须在帧尾 flush() 中执行 |
| **双 API 遗漏**：业务事件误走 transitionTo，进入游戏/退出游戏变带过渡或可能被拒 | **高** | 事件映射表逐条核对：EnterGame/QuitGame/PlayGame/InitService 一律 immediatelyTo，代码 review 时重点检查 |
| **immediatelyTo 打断逻辑错误**：abortTransition 未重置 TM 状态导致后续过渡脏 | 中 | abortTransition 必须复位 TM 状态到 IDLE，单测覆盖"过渡中打断→再过渡"路径 |
| 同一帧 flush 多个事件顺序 | 中 | flush 按入队顺序派发；transitionTo 互相拒绝（第一个执行），immediatelyTo 打断执行 |
| RenderPipeLineState 枚举被外部引用 | 中 | 保留枚举但标记废弃，新代码用 SceneManager.State |

### 检查清单

- [ ] 场景切换：Push/Pop/Set/Reset 四种操作都能正常工作（transitionTo 路径）
- [ ] **立即切换：EnterGame/QuitGame/PlayGame/InitService 四条业务路径走 immediatelyTo，无过渡、不被拒绝**（P0-1 验证）
- [ ] **退出去重（Q6）**：删除 GameMenu 退出按钮里多余的 `addEvent(new ResetGameState(...))`，只保留 `quitGame()`（QuitGame 事件）；退出只触发一次 reset
- [ ] **QuitGame 退出链路**：quitGame() 同步释放数据 + 同帧帧尾 flush 切到 MENU_MAIN，全程无"游戏场景访问已释放数据"窗口（P0-2 验证）
- [ ] 帧间安全：所有 dispose/create 在帧尾 flush() 中执行，不在帧内
- [ ] 回退兼容：现有场景文件（GamePlay/MenuMain 等）不改代码仍能运行
- [ ] RecoverNormalPipeline 删除后，渲染管线能自动恢复
- [ ] **双 AM 验证**：游戏内场景过渡使用游戏的 AnimationManager/TransitionManager（SM 从 ActiveContext 动态获取，P0-3 验证）
- [ ] 过渡中新来 transitionTo 请求被拒绝（WARN 日志）；新来 immediatelyTo 强制打断（P0-1 卡死风险验证）
- [ ] 同一帧 flush 中多个切换事件：transitionTo 只执行第一个，后续丢弃；immediatelyTo 始终可打断执行
- [ ] **过渡视觉暂缓确认**：本阶段无淡出/淡入动画，TM 空壳，行为与现状完全一致（P1-4/P1-8 验证）

---

## Phase 3b：Application 主循环替换

### 痛点

当前主循环链路：

```
Main.render()
  ├── 清屏 + 视口适配
  ├── 懒初始化（slash/lazyInit）
  ├── mainRender(deltaTime)
  │   ├── spriteBatch.begin()
  │   ├── gameHost.run(deltaTime)
  │   │   ├── renderPipeline.update(deltaTime)    ← 逻辑更新
  │   │   ├── while(eventQueue.hasEvent())         ← 事件处理
  │   │   │   └── eventDispatcher.handleEvent()
  │   │   └── renderPipeline.render(deltaTime)     ← 渲染
  │   ├── stage.act(deltaTime)
  │   ├── spriteBatch.end()
  │   └── stage.draw()
  └── topRender(deltaTime)                         ← 顶层覆盖
```

问题：
1. **Main 职责过重**：684 行，既管 libGDX 生命周期又管渲染管线
2. **GameHost.run() 是中间层**：只做 update→event→render 的串接，没有独立价值
3. **spriteBatch 生命周期和渲染逻辑耦合**：begin/end 在 Main 里，实际绘制在 GameHost 里
4. **没有标准的 update/render 分离**：事件处理夹在 update 和 render 之间，时序不清晰

### 改动内容

引入 `Application` 类统一驱动帧循环，替代 Main.render() → GameHost.run() 链路。

#### 新增文件

| # | 文件 | 职责 |
|---|------|------|
| 1 | `engine.Application` | 主循环驱动，持有 EngineContext，调度 update/render |

#### 改动文件

| # | 文件 | 改动内容 | 风险 |
|---|------|---------|------|
| 1 | `Main.java` | render() 改为调用 Application.update/render，移除 gameHost.run() | 高 |
| 2 | `GameHost.java` | run() 方法废弃，职责移交给 Application + SceneManager | 高 |
| 3 | `RenderPipeline.java` | 废弃或合并到 SceneManager | 中 |
| 4 | `SceneStack.java` | 废弃，职责已移交给 SceneManager（Phase 3a） | 中 |
| 5 | `EventQueue.java` | 废弃，职责已移交给 EventBus | 低 |
| 6 | `EventDispatcher.java` | 废弃，职责已移交给 SceneManager + 平台层 subscribe | 中 |
| 7 | `GameRenderPipeLineState.java` | 废弃 | 低 |

#### 新主循环（含工作室图标闪屏）

```java
// Main.java — 改造后（保留懒初始化 + 新增工作室闪屏）
public class Main extends ApplicationAdapter
{
    private UseViewport useViewport = UseViewport.STRETCH;
    private Stage stage;
    private SpriteBatch spriteBatch;
    private InputMultiplexer inputMultiplexer;

    private Application application;

    // ====== 闪屏状态机 ======
    private enum SplashState { SPLASH, INIT, RUNNING }
    private SplashState splashState = SplashState.SPLASH;
    private int splashFrameCount = 0;
    private Texture studioLogo;  // 同步加载，AssetManager 还没初始化

    /** 闪屏持续帧数（60fps 下 90 帧 ≈ 1.5 秒） */
    private static final int SPLASH_DURATION_FRAMES = 90;

    @Override
    public void create ()
    {
        // libGDX 基础设施初始化（Stage/SpriteBatch/InputMultiplexer）
        // ...

        // 同步加载工作室图标（AssetManager 还没初始化，只能直接 new Texture）
        // Q7：ui/studio_logo.png 当前不存在——先 //TODO 占位，资源补上后再改回真图；缺失时留空屏继续走闪屏帧数
        // TODO: 补工作室 logo 资源 ui/studio_logo.png
        studioLogo = new Texture(Gdx.files.internal("ui/studio_logo.png"));
    }

    @Override
    public void render ()
    {
        try
        {
            float deltaTime = Gdx.graphics.getDeltaTime();

            // 清屏 + 适配视口（每帧起点，固定流程）
            ScreenUtils.clear(0, 0, 0, 1f);
            stage.getViewport().apply();
            spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);

            switch (splashState)
            {
                // ====== 阶段 1：闪屏（显示工作室图标）======
                // 前 N 帧显示工作室 logo，同时让窗口管理器识别渲染窗口
                // 解决 Windows 任务栏图标丢失 bug，后期加工作室品牌展示
                case SPLASH:
                    spriteBatch.begin();
                    // 居中绘制工作室图标
                    float logoW = studioLogo.getWidth();
                    float logoH = studioLogo.getHeight();
                    float x = (stage.getViewport().getWorldWidth() - logoW) / 2f;
                    float y = (stage.getViewport().getWorldHeight() - logoH) / 2f;
                    spriteBatch.draw(studioLogo, x, y);
                    spriteBatch.end();

                    splashFrameCount++;
                    if (splashFrameCount >= SPLASH_DURATION_FRAMES)
                    {
                        splashState = SplashState.INIT;
                    }
                    break;

                // ====== 阶段 2：懒初始化（闪屏结束后执行一次）======
                // 注入 InstanceContent、初始化输入/解析器/游戏、创建 Application
                case INIT:
                    lazyInit();
                    splashState = SplashState.RUNNING;
                    break;

                // ====== 阶段 3：正常主循环（Application 接管）======
                case RUNNING:
                    application.update(deltaTime);
                    application.render(deltaTime);
                    topRender(deltaTime);
                    break;
            }
        }
        catch (Throwable e)
        {
            CrashUtils.safeCrash(e);
        }
    }

    /**
     * 懒初始化（闪屏结束后执行一次）
     * 保留原有的 initInstance / initInputAdapter / initParser / threadUpdateVersion / gameHost.init() 流程
     * 初始化完成后创建 Application 接管主循环
     */
    private void lazyInit ()
    {
        LogUtils.debug(Main.class, "lazyInit 懒加载初始化");

        // 保留原有逻辑（与当前代码一致）
        if (!initInstance()) { /* 错误处理 */ }
        if (!initInputAdapter()) { /* 错误处理 */ }
        if (!initParser()) { /* 错误处理 */ }
        if (!threadUpdateVersion()) { /* 错误处理 */ }
        if (!gameHost.init()) { /* 错误处理 */ }

        // 初始化完成后，创建 Application 接管主循环
        EngineContext engineContext = /* 从 InstanceContent 或 GameHost 中获取 */;
        application = new Application(engineContext);
        application.create();

        // 释放闪屏资源（不再需要）
        if (studioLogo != null)
        {
            studioLogo.dispose();
            studioLogo = null;
        }

        LogUtils.debug(Main.class, "lazyInit 懒加载初始化完成");
    }

    /**
     * 顶层绘制（保留，虚拟输入提示等覆盖层）
     */
    private void topRender (float deltaTime)
    {
        // 保留原有逻辑
    }

    @Override
    public void dispose ()
    {
        if (studioLogo != null) studioLogo.dispose();
        if (application != null) application.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (stage != null) stage.dispose();
        // 保留原有的 InstanceContent.dispose() 等
    }
}

```java
// Application.java — 帧循环驱动（懒初始化完成后接管）
public final class Application
{
    private final EngineContext engineContext;

    public void update (float delta)
    {
        // 场景逻辑（含过渡状态机推进；Init 场景由 SM 驱动 stepInit 分帧初始化）
        engineContext.getSceneManager().update(delta);

        // 动画引擎
        engineContext.getActiveContext().getAnimationManager().update(delta);

        // 输入状态刷新（虚拟输入/手柄，原 RenderPipeline.inputUpdater = instanceContent::update）
        // Q9：Input 接口由 platform 实现（封装 controller/virtual handler）并注入 engine，EngineContext 构造时传入
        engineContext.getInput().update(delta);

        // 帧尾 flush：统一处理本帧入队的切换事件（场景栈操作集中在此，render 前执行）
        engineContext.getEventBus().flush();
    }

    public void render (float delta)
    {
        Graphics g = engineContext.getActiveContext().getGraphics();
        g.beginFrame();
        engineContext.getSceneManager().render(delta);
        engineContext.getActiveContext().getUiManager().render(delta);
        g.endFrame();
    }
}
```

**Init 进 SM 栈（Q10）**：初始化场景（原 Init 渲染机）作为栈底场景，在 Application.create() 时压入；InitService 由 **platform 工厂**创建并注入 Init 场景（保持现状对 gameHost/configService/updateChecker 的依赖，engine 不直接 new），`InitService.stepInit()` 由 Init 场景 update 每帧驱动；`USER_CONFIG` 等异步阶段跨帧等待。完成后 `immediatelyTo(MENU_MAIN, PUSH)` 硬切换（P0-1）。过渡期（Phase 3b 前）InitService 对 gameHost/各 Manager 的引用：Init 场景构造时从 EngineContext/GameHost 注入，与现状 Init 渲染机通过 InstanceContent 访问等效。

**topRender 留在 Main**（P2-14）：虚拟输入提示等覆盖层归属 Main，Application.render() 不负责。Main RUNNING 阶段 = `application.update + application.render + topRender`，覆盖层绘在最后。

### 收益

| 收益 | 说明 |
|------|------|
| Main 瘦身 | 从 684 行降到 ~100 行，只管 libGDX 生命周期 |
| 职责清晰 | Application 管帧循环，SceneManager 管场景，各司其职 |
| 可测试性 | Application 可以用 Mock EngineContext 做测试 |
| 为引擎复用铺路 | Application + EngineContext 可以被其他项目复用 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 主循环替换导致全局行为变化 | **高** | 在 feature branch 上做，逐场景手动验证 |
| 懒初始化流程（slash/lazyInit）如何迁移 | **高** | 保留 Main 中的懒初始化逻辑，Application.create() 在 lazyInit 完成后调用 |
| **事件时序从"同帧 while"变"帧尾 flush"** | 中 | flush 放在 update 末尾 render 之前（不是帧首），保持"切换本帧生效"；逐场景验证 UI 点击响应无感延迟 |
| **InitService 分帧驱动** | 中 | Init 场景进 SM 栈，stepInit() 由 SM.update 驱动；完成入队 immediatelyTo(MENU_MAIN) |
| **输入更新纳入主循环**（P1-9） | 中 | Application.update 调用 Input.update(delta)，替代原 RenderPipeline.inputUpdater |
| Stage/SpriteBatch 生命周期归属 | 中 | Main 持有并 dispose，通过 EngineConfig 传入 Application |
| topRender 覆盖层归属 | 中 | **留在 Main**（P2-14），RUNNING 阶段 = application.update + render + topRender，覆盖层绘最后 |
| 现有场景文件中对 GameHost 的引用 | 中 | GameHost 保留但瘦身为数据持有者，不再管帧循环 |

### 检查清单

- [ ] 启动流程：Main.create → SPLASH 阶段（N 帧闪屏）→ INIT 阶段（lazyInit）→ RUNNING 阶段（Application 接管）
- [ ] 闪屏显示：工作室图标居中显示，持续 SPLASH_DURATION_FRAMES 帧（可配置）
- [ ] 懒初始化：闪屏结束后才执行 lazyInit()（InstanceContent/输入/解析器/游戏初始化），Application 在 lazyInit 末尾创建
- [ ] Windows 任务栏图标：闪屏期间窗口管理器已识别窗口，图标正常显示在任务栏
- [ ] 闪屏资源释放：lazyInit 完成后 studioLogo.dispose()，不占内存
- [ ] 闪屏期间不响应输入（键盘/鼠标/手柄点击不会穿透到闪屏后面的逻辑）
- [ ] 场景切换：Push/Pop/Set/Reset 在新主循环下正常工作
- [ ] **帧尾 flush**：update 阶段入队的切换事件当帧（render 前）被处理，无拖帧；UI 点击响应正常（P1-5 验证）
- [ ] **Init 进栈**：InitService.stepInit() 由 SM.update 驱动，USER_CONFIG 异步阶段跨帧等待正常，完成后硬切到 MenuMain（P1-10 验证）
- [ ] **输入更新**：Input.update(delta) 驱动虚拟输入/手柄，行为与现状 RenderPipeline.inputUpdater 一致（P1-9 验证）
- [ ] 过渡动画：淡出→切换→淡入 在新主循环下无闪烁
- [ ] **topRender 留在 Main**：虚拟输入提示等覆盖层正常显示在最上层（P2-14 验证）
- [ ] UI 渲染：Stage/Widget 在新主循环下正常绘制
- [ ] 音频播放：背景音乐/音效 在新主循环下正常播放
- [ ] dispose：退出时按序释放所有资源，无泄漏（P2-13 责任矩阵，详见设计文档「dispose 责任矩阵」节 Q12：游戏 ActiveContext 随 QuitGame 释放、场景对象由 SM dispose、Input/EventBus 归 Main/EngineContext）
- [ ] 全场景手动验证：Init → MenuMain → MenuList → GamePlay → GameMenu → ConfigBasic

---

## Phase 4：UiManager 拆分 + AssetManager + ScriptExecutor 迁移

### 痛点

**UiManager（5924 行）**：
1. 控件管理（add/remove/get）和输入处理（focus/keyboard navigation）混在一起
2. 布局加载（JSON 解析、模板合并）和控件渲染混在一起
3. 文本解析（`{language$block#key}`）和主题切换混在一起
4. 所有控件类型（Button/Label/Image/Text/Graphics/MessageBox）共用一个基类，缺乏类型安全

**资源加载**：
1. `GraphicsManager.loadPicture()` 直接 new Texture，无缓存
2. `AudioManager.loadBackgroundMusic()` 直接 new Music，无引用计数
3. 各 Manager 的 init() 中手动加载资源，重复代码多

### 改动内容

#### UiManager 拆分

| 拆分后 | 职责 | 行数估算 |
|--------|------|---------|
| `Widget` 接口 | 控件基类（getTag/render/dispose） | ~20 |
| `InteractableWidget` 接口 | 可交互控件（focus/click） | ~20 |
| `TextWidget` 接口 | 文本控件（setText/getText） | ~15 |
| `WidgetManager<W>` | 泛型控件管理器 | ~80 |
| `UiManager`（瘦身后） | 组合 WidgetManager，提供快捷方法 | ~500 |
| `LayoutLoader` | 布局 JSON 解析 + 模板合并 + 实例化 | ~300 |
| `VirtualInputHandler` | 键鼠/手柄→控件选择映射 | ~200 |

**保留在平台层**：
- `LanguageManager`：`{language$block#key}` 文本解析
- `ThemeManager`：主题资源路径解析

#### AssetManager 新增

| 文件 | 职责 |
|------|------|
| `AssetManager.java` | 统一资源加载（同步+异步）、缓存、引用计数 |
| `AssetDescriptor.java` | 资源描述符（路径+类型） |

#### ScriptExecutor 迁移

| 改动 | 说明 |
|------|------|
| `ScriptExecutor.java` | 从 `com.hujiugame.qingfeng.script` 移到 `engine.script` |
| `CommandRegistry.java` | 命令注册表，平台层通过 `register()` 注入具体命令 |
| `ScriptTask.java` / `TriggerTask.java` | 任务模型，移到 engine.script |

### 收益

| 收益 | 说明 |
|------|------|
| UiManager 可维护 | 从 5924 行拆分为多个 200-500 行的类 |
| 类型安全 | Widget/InteractableWidget/TextWidget 接口提供编译期约束 |
| 资源管理统一 | AssetManager 提供缓存+引用计数，防止资源泄漏 |
| 脚本引擎复用 | ScriptExecutor 移到 engine 层，其他项目可复用 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| UiManager 拆分影响所有场景文件 | **高** | 逐场景验证 UI 渲染，保留旧 API 做兼容层 |
| Widget 接口与现有控件类不匹配 | 中 | 先定义最小接口，逐步扩展 |
| AssetManager 引入改变加载时序 | 中 | 先做同步加载，异步加载作为后续优化 |
| ScriptExecutor 迁移影响脚本执行 | 中 | 只移包，不改内部逻辑，CommandRegistry 注册机制不变 |

### 检查清单

- [ ] 所有场景的 UI 渲染正常（Init/MenuMain/MenuList/GamePlay/...）
- [ ] 布局加载正常（JSON→控件树→渲染）
- [ ] 虚拟输入正常（键鼠/手柄选择控件）
- [ ] 文本解析正常（`{language$block#key}` 替换）
- [ ] 主题切换正常
- [ ] 资源加载正常（Texture/Music/Sound）
- [ ] 脚本执行正常（ScriptTask/TriggerTask）
- [ ] 命令注册正常（ForwardPage/GotoPage/ShowLayout）

---

## Phase 5：InstanceContent 瘦身 + ActiveContext + 平台层重组

### 痛点

`InstanceContent`（584 行，30+ 字段）是 God Object：
- 持有从 SpriteBatch 到 LanguageManager 的一切
- 任何子系统都要通过 `InstanceContent.getInstance().getXxx()` 访问
- 初始化顺序隐含在 `init()` 方法中，没有显式依赖声明
- 启动器和游戏场景共用同一组 Manager，无法独立配置

### 改动内容

#### InstanceContent 瘦身

| 阶段 | InstanceContent 持有 | 说明 |
|------|---------------------|------|
| 当前 | 30+ 字段（全量） | God Object |
| Phase 5 目标 | 只做启动引导，运行时无状态 | GameHost 持有 EngineContext |

```java
// 最终形态
public final class InstanceContent
{
    public static void init ()
    {
        EngineContext engine = new EngineContext(defaultConfig);
        GameHost gameHost = new GameHost(engine, configService);
        Application app = new Application(engine, gameHost);
        app.create();
    }
}
```

#### ActiveContext 引入

```java
// 启动器上下文（低分辨率、简单 UI）
ActiveContext launcherContext = new ActiveContext(
    launcherAnimationManager,
    launcherGraphics,     // 800x600
    launcherAudio,
    launcherUiManager     // 简单菜单 UI
);

// 游戏上下文（游戏分辨率、完整 UI）
ActiveContext gameContext = new ActiveContext(
    gameAnimationManager,
    gameGraphics,         // 1920x1080
    gameAudio,
    gameUiManager         // 完整游戏 UI
);

// 切换时（SM 不缓存 AM/TM，每次从 EC 动态取当前上下文，切换后自然感知）
engineContext.setActiveContext(gameContext);
```

#### 平台层重组

| 当前位置 | 移动到 | 说明 |
|---------|--------|------|
| `com.hujiugame.qingfeng.scene.impl.*` | `platform.game.scene.*` | 场景实现 |
| `com.hujiugame.qingfeng.manager.*` | `platform.game.manager.*` | 游戏管理器 |
| `com.hujiugame.qingfeng.game.*` | `platform.game.*` | 游戏逻辑 |
| `com.hujiugame.qingfeng.type.*` | `platform.type.*` | 平台类型 |
| `com.hujiugame.qingfeng.config.*` | `platform.config.*` | 配置管理 |

### 收益

| 收益 | 说明 |
|------|------|
| InstanceContent 不再是瓶颈 | 从 584 行降到 ~30 行 |
| 启动器/游戏独立配置 | ActiveContext 允许不同的 Graphics/Audio/Ui 配置 |
| 包结构清晰 | engine/platform 边界明确，依赖方向单一 |
| 可复用性 | engine 层可以被其他项目复用 |

### 风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 包移动导致大量 import 变更 | 中 | IDE 自动重构，逐文件验证 |
| ActiveContext 切换时机 | 中 | 在 GameSessionManager 中显式切换，不在场景内部 |
| **SM 缓存了旧 ActiveContext 的 AM/TM**（P0-3） | 中 | SM 不缓存 AM/TM 引用，每次过渡驱动从 EC 动态取当前 ActiveContext |
| InstanceContent 瘦身后引用断裂 | 中 | 全局搜索 `InstanceContent.getInstance()`，逐个替换为 EngineContext 访问 |

### 检查清单

- [ ] InstanceContent 只有启动引导代码
- [ ] 所有 `InstanceContent.getInstance().getXxx()` 调用已替换
- [ ] ActiveContext 切换正常（启动器→游戏→返回启动器），切换后 SM 过渡自动用新上下文（P0-3）
- [ ] 包结构符合 engine/platform 边界规则
- [ ] engine 包不 import 任何 platform 类
- [ ] 全场景手动验证通过

---

## 附录：当前代码文件索引

| 文件 | 行数 | 当前职责 | 重构后归属 |
|------|------|---------|-----------|
| `Main.java` | 684 | libGDX 入口 + 主循环 | 瘦身为 libGDX 适配器 |
| `GameHost.java` | 373 | 帧循环串接 + 游戏数据持有 | 移除帧循环职责，保留数据持有 |
| `InstanceContent.java` | 584 | God Object 服务定位器 | 瘦身为启动引导 |
| `EventDispatcher.java` | 410 | 事件分发（13 种事件） | 废弃，职责移给 SceneManager + 平台层 |
| `EventQueue.java` | 39 | 事件排队 | 废弃，职责移给 EventBus |
| `SceneStack.java` | 569 | 场景栈管理 | 废弃，职责移给 SceneManager |
| `TransitionManager.java` | 168 | 场景过渡动画 | 改为纯动画执行器，归 AM 持有 |
| `AnimationManager.java` | 111 | 动画管理 | 移到 engine.animation，持有 TM |
| `UiManager.java` | 5924 | UI 管理（控件/输入/布局/文本） | 拆分为多个 200-500 行的类 |
| `EngineContext.java` | 122 | 引擎上下文（3 字段） | 扩展为完整子系统容器 |
| `GameRenderRegistry.java` | 41 | 场景工厂注册 | 泛化为 SceneRegistry |
| `GraphicsManager.java` | - | 图形管理 | 封装在 LibGDXGraphics 内部 |
| `AudioManager.java` | - | 音频管理 | 封装在 LibGDXAudio 内部 |
| `RenderPipeline.java` | - | 渲染管线 | 废弃，职责移给 SceneManager |
| `GameRenderPipeLineState.java` | - | 渲染管线状态枚举 | 废弃，由 SceneManager.State 替代 |
