# 引擎分离实施计划审查报告

> **文档定位**：对 `2026-09-01-engine-separation-implementation-plan.md` 的代码级审查，逐帧推演验证，找出会导致游戏无法运行或功能不一致的纰漏。
>
> **文档结构**：审查结论 → 当前代码真实时序（验证基准）→ 问题清单总表 → P0/P1/P2 详述 → 过时认知修正
>
> **关联文档**：`2026-08-29-engine-separation-design.md`（设计）、`2026-09-01-engine-separation-implementation-plan.md`（实施计划）
>
> **更新规范**：审查发现的新问题追加到对应级别；问题修复后在状态列标记。

---

## 审查结论

方案主体方向（engine/platform 分层、SM 统一场景栈、渐进 6 阶段）成立，但**直接按当前实施计划执行存在 3 个 P0 级问题（会导致崩溃或功能失效）和 1 批 P1 级问题（功能不一致/时序风险）**，必须先解决再进入 Phase 3。

核心症结：**实施计划对"当前代码的真实行为"存在过时认知**（TransitionManager 早已空壳化），导致多个关键决策建立在错误假设上。以下问题清单全部经代码逐帧推演验证。

---

## 当前代码真实执行时序（验证基准）

以下是审查依赖的事实基准，均从代码确认：

### 主循环（GameHost.run, GameHost.java:198）

```
renderPipeline.update(delta)     ← 逻辑更新（NORMAL: gameRender.update + inputUpdater）
while (eventQueue.hasEvent())    ← 事件处理（"状态切换，本帧立即生效"）
    eventDispatcher.handleEvent(event)
renderPipeline.render(delta)     ← 渲染
```

### 场景切换双路径（关键！）

**路径 A — 空壳路径（带过渡意图，实际无动画）**：
```
场景文件 addEvent(new PushGameState(out, in))     ← 帧内（场景 update 阶段）
→ EventDispatcher.handleEventOfPushGameState
    → TM.setPendingTask(event)                    ← 记录意图
    → renderPipeline.setState(TRANSITION)
    → TM.startTransition()
        → executePendingTask()                    ← 入队 PushGameStateExecute
        → recoverRenderPipeline()                 ← 入队 RecoverNormalRenderPipeLine
→ while 循环继续（同一帧）
    → PushGameStateExecute → SceneStack.pushGameState → updateGameState
        → renderPipeline.clear()                  ← dispose 旧渲染机
        → renderPipeline.register()               ← 创建新渲染机
    → RecoverNormalRenderPipeLine → setState(NORMAL)
→ 同一帧 render 新场景
```

**路径 B — 直接执行路径（无过渡意图）**：
```
InitService.stepStop  → addEvent(PushGameStateExecute(MENU_MAIN))    InitService.java:265
EnterGame  → EventDispatcher → PushGameStateExecute(GAME_MENU)       EventDispatcher.java:359
PlayGame   → EventDispatcher → PushGameStateExecute(GAME_PLAY)       EventDispatcher.java:384
QuitGame   → EventDispatcher → ResetGameStateExecute                 EventDispatcher.java:373
```
这些**直接入队 Execute 事件，绕过 TransitionManager，无过渡**，是刻意的"硬切换"语义。

### TransitionManager 现状（TransitionManager.java:17 注释 + CHANGELOG commit 3952fb5）

**已是空壳**：1195 行动画执行逻辑（initFadeOut/fadingOut/initFadeIn/fadingIn/smooth_move）已删除，只保留双事件模型骨架（setPendingTask → executePendingTask）。`startTransition()` 立即执行 + 恢复渲染管线，无任何动画。

### 渲染管线状态（RenderPipeline.java:117,182）

- `NORMAL` → `updateNormal`（gameRender.update + inputUpdater）
- `TRANSITION` → `updateFading`（**空实现**）+ `renderFading`（调 `gameRender.transitionRender`）
- 场景文件 `transitionRender` 是空壳（MenuMain.java:203 直接调 render）

### 其他关键事实

- **启动器/游戏双 AnimationManager**：`EventDispatcher.getCurrentAnimationManager()`（EventDispatcher.java:287）按 `gameSessionManager.isInGame()` 动态切换，各有独立 TransitionManager
- **RefreshUiManager**（EventDispatcher.java:132）：替换 UiManager → `sceneStack.refreshGameState()`（内部 setGameState(peek) → clear+register）→ `SafePostRunnable.post(oldUiManager::dispose)`（延迟下一帧释放）
- **场景工厂**全部集中在 `InstanceContent.registerRenderRegistry()`（InstanceContent.java:84），lambda 捕获 instanceContent 字段
- **InitService** 分帧初始化（InitService.java:85），由 Init 渲染机每帧调 `stepInit()`；USER_CONFIG 阶段跨帧等待异步
- **输入更新**：`RenderPipeline.inputUpdater = instanceContent::update`（InstanceContent.java:144），每帧驱动虚拟输入/手柄
- **QuitGame 时序**：`GameSessionManager.quitGame()`（GameSessionManager.java:193）**先 addEvent(QuitGame) 后立即同步执行 disposeData/disposeResource/disposeGameConfig**

---

## 问题清单总表

| # | 级别 | 问题 | 状态 |
|---|------|------|------|
| 1 | **P0** | EnterGame/QuitGame/PlayGame/InitService 的"立即切换"语义被方案丢失 | 待定 |
| 2 | **P0** | QuitGame 先释放数据后切场景，跨帧过渡产生崩溃窗口 | 待定 |
| 3 | **P0** | ActiveContext 含 AM 与 SM 构造时固定持有 AM 矛盾（双 AM 架构被破坏） | 待定 |
| 4 | **P1** | 方案痛点基于过时认知：过渡方法已删、TM 已空壳，工作量低估 | 待定 |
| 5 | **P1** | 事件处理从"同帧 while"变"下帧 flush"，所有 addEvent 触发点延迟一帧 | 待定 |
| 6 | **P1** | EventBus publish/queue 语义与切换"立即性"未明确 | 待定 |
| 7 | **P1** | RefreshUiManager 的 SafePostRunnable 帧间释放机制可能丢失（崩溃风险） | 待定 |
| 8 | **P1** | 废弃 GameRenderPipeLineState 需连带适配 RenderPipeline；过渡视觉实现未定 | 待定 |
| 9 | **P1** | 输入更新（inputUpdater/instanceContent.update）未纳入 Application 主循环 | 待定 |
| 10 | **P1** | InitService 分帧驱动在主循环替换后未安排 | 待定 |
| 11 | **P2** | PopGameState 的 inState（getSecondGameState）计算时序 | 待定 |
| 12 | **P2** | 场景工厂 lambda 捕获的依赖迁移方案缺失 | 待定 |
| 13 | **P2** | dispose 顺序（Main/InstanceContent/EngineContext）需对照 | 待定 |
| 14 | **P2** | topRender 覆盖层归属（Main 还是 Application）未明确 | 待定 |
| 15 | **P2** | 过渡期间 currentScene 的 update/render/输入行为未完整定义 | 待定 |

---

## P0 — 严重问题（会导致崩溃/卡死/核心功能失效）

### P0-1：立即切换语义丢失

**现状推演**：EnterGame/PlayGame/QuitGame/InitService 四条路径直接入队 `*_EXECUTE` 事件，绕过 TransitionManager，**当前是无过渡的硬切换**。这是刻意的（进入/退出游戏的硬性切换、初始化完成直接进主菜单）。

**方案处理**：实施计划把 EnterGame/QuitGame/PlayGame 归为"业务事件→保留，平台层 subscribe"（映射表），InitService 未提。但方案只给 SM 提供了带过渡的 `push()/pop()/set()/reset()` 公开 API，且加了过渡保护（非 IDLE 拒绝）。

**重构后推演**：若业务事件处理逻辑调 `SM.push()/SM.reset()`：
1. 行为变化：进入游戏/退出游戏/开始游戏会从"立即切换"变成"带淡出淡入"
2. **卡死风险**：若恰在过渡中（如脚本连续触发）触发 QuitGame → `SM.reset()` 被过渡保护拒绝 → **游戏无法退出**
3. 若业务事件不调 SM 而直接操作 SceneStack（立即切换），则 SM 状态机与栈操作脱节，Phase 3b 场景栈接管失败

**建议**：SM 提供两套 API：
- `transitionTo(action, state)`（带过渡，供 UI 触发）
- `immediatelyTo(action, state)`（立即切换，供业务事件/初始化），立即切换内部仍须走帧间安全路径（不在帧内 render 中途），且**不触发过渡保护拒绝逻辑**（或明确硬切换可打断过渡）

---

### P0-2：QuitGame 先释放数据后切场景 → 跨帧过渡崩溃窗口

**现状推演**：
```
帧 N：场景 update → 用户点退出 → quitGame()
  ├── addEvent(new QuitGame())                  ← 入队
  ├── disposeData() / disposeResource() / disposeGameConfig()   ← 同步立即释放游戏数据/资源/配置
帧 N：while 循环（同帧）处理 QuitGame → resetGameState → 切到 MENU_MAIN
  ├── MENU_MAIN isInGame=false，走启动器 themeManager，不依赖已释放的游戏数据 → 安全
帧 N：render 主菜单
```
**安全原因**：数据释放和场景切换发生在**同一帧**，中间没有"游戏场景还在渲染但数据已释放"的窗口。

**重构后推演**（若 QuitGame 走 SM 状态机 FADE_OUT 多帧）：
```
帧 N：quitGame() 同步释放游戏数据/资源/配置
帧 N+1：EventBus.flush → SM.reset() → 进入 FADE_OUT（持续多帧）
帧 N+1~N+K：FADE_OUT 期间游戏场景（GAME_MENU/GAME_PLAY）仍在渲染
  → 访问已释放的 playLocalData/资源/配置 → 崩溃
```

**即使 QuitGame 走"立即切换"（P0-1 修复）**，若立即切换被延迟到下一帧 flush 才执行（P1-5），仍存在一帧窗口。必须保证**切到 MENU_MAIN 完成前不释放游戏数据**，或**释放动作也延迟到切换完成后**。

**建议**：quitGame 的数据释放顺序重构为"先入队 QuitGame → 切换完成后（帧间）再释放"，释放动作通过 SafePostRunnable 或等效的帧间机制挂到切换之后。方案未提此点。

---

### P0-3：ActiveContext 含 AM 与 SM 固定持有 AM 矛盾

**现状**：启动器和游戏各有独立 AnimationManager/TransitionManager，`EventDispatcher.getCurrentAnimationManager()` 按 `isInGame()` 动态选择（EventDispatcher.java:287）。这是刻意的：游戏内用 PlayLocalData 的 AM（含游戏动画数据）。

**方案**：设计文档第七节 ActiveContext 含 AnimationManager（启动器/游戏各一份），但 EngineContext 构造时 `sceneManager = new SceneManager(am, eventBus)` **固定持有默认启动器 AM**；`setActiveContext(gameContext)` 切换时 SM 持有的 AM 引用不变。

**重构后推演**：
- 游戏内场景切换会用到**启动器的 TransitionManager**，而不是游戏的 → 行为不一致
- 游戏 AM（含游戏动画数据）永远不会被 SM 的过渡驱动使用 → 游戏动画数据丢失
- 与"SM 通过 AM 驱动过渡"的设计目标直接矛盾

**建议**：SM 的 `getTransitionManager()` 应**动态从当前 ActiveContext 获取**，而非构造时固定持有。明确 SM 与 ActiveContext 的协作关系（SM 持 ActiveContext 引用，或通过 EngineContext.getActiveContext() 间接访问）。

---

## P1 — 中等问题（功能不一致/时序风险/覆盖不全）

### P1-4：方案痛点基于过时认知，工作量低估

**事实**：过渡方法（initFadeOut/fadingOut/initFadeIn/fadingIn）在 commit 3952fb5 **早已删除**；TM 已是 168 行空壳；场景文件的 transitionRender 是空壳（直接调 render）。

**方案影响**：
1. 实施计划"痛点 3：过渡状态机由场景文件推进（场景调 fadingOut()/initFadeOut()）"**不成立**——场景文件已不再耦合过渡
2. "重写 TM 为纯动画执行器"实际是**从零新建动画执行引擎**，不是重构 168 行空壳。Phase 3a 的"改动 ~10 文件"严重低估
3. 方案假设"过渡方法标记 @Deprecated 但保留空实现"（Phase 3a 风险表）——但方法不存在，无需此步骤，属多余设计

### P1-5：事件处理时机从"同帧"变"下帧"

**现状**：`GameHost.run` 的 while 循环在 update 之后，场景 update 阶段 addEvent 的事件**同一帧**被处理。

**方案**：`Application.update` 第一行 `eventBus.flush()`，即**每帧开头**处理上帧入队事件。场景 update 阶段入队的事件要**下一帧**才处理。

**影响**：所有"场景 update → addEvent → 同帧切换"的触发点延迟一帧。UI 点击后切换慢一帧（16ms，基本无感），但：
- 逐帧推演需全部重做（当前所有基于"同帧"的推演失效）
- 若脚本在 update 内连续触发切换，跨帧累积可能与过渡保护交互出意外

### P1-6：EventBus publish/queue 语义与切换立即性未明确

方案 EventBus 提供 `publish`（同步立即）和 `queue`（下帧 flush）。场景切换事件走哪个未定义。若走 queue → 下帧执行（P1-5）；若走 publish → 在 update 中途直接执行栈操作，需确认不违反帧间约束（render 前仍安全，但 update 内部中间态要推演）。

**建议**：明确场景切换请求统一走 queue（下帧 flush 统一执行），保持"切换在帧间"红线。

### P1-7：RefreshUiManager 的帧间释放机制可能丢失（崩溃风险）

**现状**：REFRESH_UI_MANAGER → 替换 UiManager + `sceneStack.refreshGameState()`（clear+register 重建渲染机）+ `SafePostRunnable.post(oldUiManager::dispose)`（**下一帧**释放旧 UiManager，避免在渲染帧中释放）。

**方案**：RefreshUiManager 归为"业务事件→保留，UiManager.subscribe()"，但未说明：
1. `refreshGameState()` 依赖 SceneStack/RenderPipeline（重构后归 SM），谁协调执行？
2. **SafePostRunnable 的"下一帧释放"语义如何保留**？EventBus.flush 是当帧派发，替代不了 SafePostRunnable。若丢失，旧 UiManager 在渲染帧内 dispose → **崩溃**。

### P1-8：废弃 GameRenderPipeLineState 需连带适配 RenderPipeline；过渡视觉未定

**现状**：RenderPipeline.update/render 内部 `switch(state)` 依赖 GameRenderPipeLineState（NORMAL/TRANSITION）。

**方案**：GameRenderPipeLineState 废弃，由 SM.State 替代。但：
1. RenderPipeline 内部的 switch 逻辑如何改？若 SM 接管过渡，RenderPipeline 还需要 TRANSITION 分支吗？
2. **过渡视觉实现（淡出/淡入渲染什么）方案明确"未定，留到动画系统设计时再决定"**。但 Phase 3a 就要 TM 驱动淡出淡入，淡出期间屏幕显示什么必须落地（黑幕？旧场景定格？）。否则过渡期间渲染行为未定义。

### P1-9：输入更新未纳入 Application 主循环

**现状**：`RenderPipeline.inputUpdater = instanceContent::update`（InstanceContent.java:144），每帧驱动虚拟输入/手柄更新。

**方案**：实施计划 Application.update 伪代码只有 `flush + SM.update + AM.update`，设计文档虽有 `Input.update(delta)` 但实施计划未落实。若丢失，**虚拟输入/手柄失效**。

### P1-10：InitService 分帧驱动未安排

**现状**：InitService 由 Init 渲染机每帧调 `stepInit()`，跨多帧（USER_CONFIG 等异步），完成后 addEvent(PushGameStateExecute(MENU_MAIN))。

**方案**：InitService 归属 platform/launcher，但主循环替换后：
1. Init 场景如何进 SM 栈并由 SM.update 驱动 stepInit？需明确
2. 完成时入队的是**直接执行事件**（P0-1），SM 要支持
3. InitService 依赖 gameHost/各 Manager，过渡期（Phase 3b 前）这些引用怎么接

---

## P2 — 细节问题

### P2-11：PopGameState 的 inState 计算时序

**现状**：`handleEventOfPopGameState` 调 `sceneStack.getSecondGameState()` 设置 inState（EventDispatcher.java:208）。

**重构后**：SM.pop() 的淡出完成后 executeTransition 要算 pop 目标。需确认在弹栈前 getSecondGameState 才有效（栈顶第二），且过渡期间栈不得被其他操作改变。

### P2-12：场景工厂 lambda 依赖迁移缺失

**现状**：全部场景工厂在 InstanceContent.registerRenderRegistry 的 lambda 中，捕获 instanceContent 字段（InstanceContent.java:84-145）。

**方案**：泛化为 SceneRegistry。但 lambda 捕获的依赖（gameHost/layoutManager/virtualInputHandler 等）如何从 EngineContext 注入？需给出迁移映射，否则 Phase 1-2 建立的新 registry 无法实例化现有场景。

### P2-13：dispose 顺序对照

**现状**：InstanceContent.dispose（InstanceContent.java:572）：updateChecker → gameHost → uiManager → animationManager → graphicsManager → audioManager；Main.dispose（Main.java:350）：关线程 → spriteBatch → stage → InstanceContent。

**方案**：EngineContext.dispose：SM → ScriptExecutor → ActiveContext → AssetManager → EventBus。需对照确认 SpriteBatch/Stage（Main 持有）与渲染机（RenderPipeline 持有）的释放顺序，避免 double-dispose 或 use-after-free。

### P2-14：topRender 归属未明确

**现状**：Main.topRender → instanceContent.topRender（虚拟输入提示覆盖层，Main.java:301）。

**方案**：Phase 3b 伪代码在 RUNNING 阶段调 topRender(delta)，但 Application.render 的职责未提。需明确覆盖层归属 Main 还是 Application。

### P2-15：过渡期间 currentScene 行为未完整定义

设计文档 SM.update：FADE_OUT/FADE_IN 期间不调 currentScene.update，只推进 TM。但：
- 过渡期间 currentScene.render 调不调？调的话渲染什么（配合 P1-8 的过渡视觉）
- 过渡期间输入是否屏蔽？若 UI 按钮还响应，会触发新切换（被过渡保护拒绝，需明确）
- 场景内脚本（ScriptExecutor 每帧驱动）在过渡期间是否暂停？若暂停，恢复后状态是否一致

---

## 过时认知修正（方案需更新的前提）

| 方案中的认知 | 实际代码 | 影响 |
|-------------|---------|------|
| 过渡状态机由场景文件推进（调 fadingOut/initFadeOut） | 方法早已删除（commit 3952fb5），TM 空壳，场景 transitionRender 直接调 render | 痛点描述错误，改动范围假设失效 |
| 重写 TM 为纯动画执行器 | TM 是 168 行空壳，无动画逻辑可"重写" | 实际是**新建**动画执行引擎，工作量低估 |
| 过渡方法标记 @Deprecated 保留空实现 | 方法不存在 | 多余设计步骤 |
| 场景切换走 EventQueue 延迟到帧间 | 实际是同帧 while 循环立即执行 | "延迟到帧间"的表述与实际不符 |
| ActiveContext 切换时 SM 无感知 | SM 构造时固定持有启动器 AM | P0-3 矛盾 |

---

## 建议的下一步

1. **先解决 P0-1/P0-2/P0-3** 三个问题，更新设计方案（第四节 SM、第七节 ActiveContext）+ 实施计划（Phase 3a 事件映射、Phase 3b 主循环、Phase 5 平台层）
2. 修正 P1-4 的过时认知，重新估算 Phase 3a 工作量（新建动画执行引擎）
3. 明确 P1-5/P1-6 的事件时序与 publish/queue 语义，重做逐帧推演
4. 落实 P1-7/P1-9/P1-10 的 SafePostRunnable 保留、输入更新纳入主循环、InitService 驱动安排
5. 以上确认后再进入 Phase 1 实施
