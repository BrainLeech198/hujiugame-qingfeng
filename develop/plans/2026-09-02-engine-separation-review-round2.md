# 引擎分离实施计划二次审查报告（2026-09-02）

> **文档定位**：对「已回写 15 项决策的最新版设计文档 + 实施计划」的二次代码级审查。第一轮审查（2026-09-01，`2026-09-01-engine-separation-plan-review.md`）聚焦方案假设真实性；本轮聚焦「功能效果与现状是否一致」，重点逐帧推演场景切换/退出/进入游戏/初始化链路。
>
> **文档结构**：审查结论 → 现状时序验证基准 → 问题清单总表 → P0/P1/P2 详述 → 文档表述修正建议 → 待用户决策清单
>
> **关联文档**：`2026-08-29-engine-separation-design.md`（设计）、`2026-09-01-engine-separation-implementation-plan.md`（实施计划）、`2026-09-01-engine-separation-plan-review.md`（一审）
>
> **更新规范**：与用户讨论确认解决方案后，回写设计文档/实施计划并在本报告状态列标记。**本报告只列问题与证据，不自行给定方案**；方案由用户讨论后确定。

---

## 决策回写状态（2026-09-02 全部确认，已回写设计文档 + 实施计划）

| # | 问题 | 用户决策 |
|---|------|---------|
| Q1 | SM 公开 API 调用时机 | 场景仍走 queue，SM 方法只在帧尾 flush 被调用 |
| Q2 | 场景对象生命周期 | 弹栈重建（场景对象不常驻，枚举栈 + 仅栈顶活跃） |
| Q3 | executeSceneSwitch 页面加载链 | 留在 SM 内部（createScene 含 loadLayout/loadConfig/init） |
| Q4 | immediatelyTo API 签名 | 带 action 参数；QuitGame → RESET 清栈回主菜单 |
| Q5 | RESET 语义 | 清空全部 + 压 MENU_MAIN |
| Q6 | QuitGame 双事件 | 去重，只走 QuitGame（删 GameMenu 多余 ResetGameState） |
| Q7 | 闪屏 | 本次做，logo 资源留 //TODO 占位，SPLASH 期间忽略输入 |
| Q8 | ActiveContext 切换 + UiManager | 场景每次从 ActiveContext 动态取 UiManager |
| Q9 | 输入更新 engine/platform 边界 | Input 接口由 platform 实现注入（方案1 接口接线） |
| Q10 | Init 进栈 | SM 栈底压 Init，平台注入 InitService |
| Q11 | 空壳阶段 transitionTo 表述 | 文档表述修正（空壳阶段不互相拒绝） |
| Q12 | dispose 责任矩阵 | 设计文档补全责任矩阵（含游戏 ActiveContext 释放时机） |

---

## 审查结论（一句话）

方案骨架仍成立，但**直接按实施计划执行仍有 5 个 P0 会导致崩溃或功能不一致**：核心集中在「SM 公开 API 被场景直接调用时的帧内 dispose 风险」「场景对象生命周期模型从『枚举栈+仅栈顶渲染机』变为『场景对象常驻栈』造成的行为差异」「executeSceneSwitch 丢失 layout/config 加载」「RESET 语义与现状不一致」「闪屏为新增功能且依赖不存在资源」。

---

## 现状时序验证基准（本轮新确认）

| 链路 | 现状真实行为 | 源码证据 |
|------|-------------|---------|
| 场景触发切换 | 场景 update 里 `eventQueue.addEvent(new PushGameState/PopGameState/...)`，**只入队不执行**；切换在 `GameHost.run` 的 while 循环（update 之后 render 之前）执行，**不在场景 update 调用栈内** | MenuMain.java:117, GameMenu.java:63, ConfigBasic.java:68；GameHost.java:198-222 |
| 空壳→执行 | 空壳事件 → `setPendingTask` + `setState(TRANSITION)` + `startTransition`；`startTransition` 立即 `executePendingTask`（入队 `*_EXECUTE`）+ 入队 `RecoverNormalRenderPipeLine` | TransitionManager.java:143-167 |
| 业务事件 | Enter/Quit/Play 直接调对应 `*_EXECUTE` handler → 直接 `sceneStack.push/reset`（绕过 TM），同帧 while 内执行 | EventDispatcher.java:356-389 |
| 场景创建 | `updateGameState` = `loadGameLayout`（isInGame 判断 + theme 选择 + 缓存 + firstInGame 强制重读）+ `loadGameConfig` + `updateGameRender`（`renderPipeline.clear()` 旧渲染机 + `register` 新渲染机 + `init_(container)`） | SceneStack.java:85-140, 515-542 |
| 渲染机生命周期 | `RenderPipeline` 只持有**一个** `gameRender`（栈顶）；push/pop/set/reset 都 `clear` 旧 + `register` 新 → **非栈顶场景渲染机被销毁，弹栈后重建** | RenderPipeline.java:75-105, 244-261 |
| QuitGame | GameMenu 退出按钮：`quitGame()`（内部 addEvent(QuitGame) + 同步释放数据/资源/配置）**随后又** `addEvent(new ResetGameState(...))` → 同一帧内 QuitGame 与 ResetGameState 各触发一次 reset，共两次 | GameMenu.java:78-81；GameSessionManager.java:193-235 |
| inputUpdater | `inputUpdater = deltaTime -> instanceContent.update(deltaTime)`，内部 = `controllerInputHandler.update + virtualInputHandler.update`（**platform 层 handler**） | InstanceContent.java:144, 554-558 |
| 输入更新顺序 | `gameRender.update(deltaTime)` 先，`inputUpdater.accept` 后 | RenderPipeline.java:156-161 |
| InitService | 在 `registerRenderRegistry` lambda 中创建，**每个 Init 实例独立**；Init 渲染机 update 每帧调 `stepInit()`；USER_CONFIG 跨帧等待；完成入队 `PushGameStateExecute(MENU_MAIN)` | InstanceContent.java:87-101；InitService.java:85-115, 259-269 |
| 场景基类 | `AbstractGameRender` 只持有 `gameStateDataContainer`；各场景构造时**注入** uiManager 等依赖（非每次从 EC 取） | AbstractGameRender.java:12-46；InstanceContent.java:103-110 |
| 双 AM | `getCurrentAnimationManager`：`isInGame()` 时用 `playLocalData.getAnimationManager()`，否则用 `InstanceContent` 版 | EventDispatcher.java:287-294 |
| Main 启动 | 无闪屏：`slash()` 仅置标志，第 1 帧 slash 第 2 帧 lazyInit；无 studioLogo、无 SPLASH 帧数 | Main.java:255-261, 411-414 |

---

## 问题清单总表

| # | 级别 | 问题 | 影响 |
|---|------|------|------|
| Q1 | **P0** | SM 公开 API 被场景 update 直接调用 → executeSceneSwitch 在场景栈内执行 → 场景自 dispose 后继续运行 → use-after-free | 崩溃 |
| Q2 | **P0** | 场景对象生命周期模型变化（枚举栈+仅栈顶渲染机 → 场景对象常驻栈）：弹栈回到的页面状态与现状不同（现状重建/新方案保留），且 POP 分支缺重建 | 功能不一致 |
| Q3 | **P0** | `executeSceneSwitch` 伪代码只 `sceneRegistry.create(state)`，丢失 loadGameLayout/loadGameConfig/init_(container) 加载链 | 页面无布局/配置 |
| Q4 | **P0** | `immediatelyTo` 伪代码硬编码 `PUSH`；QuitGame 现状是清栈 RESET，若全走 PUSH 则游戏栈残留 | 退出行为不一致 |
| Q5 | **P0** | RESET 语义不一致：现状 `clear()+push(MENU_MAIN)`；新方案 `while(size>1) pop` 只清到栈底（可能剩 Init） | 退出后栈残留 |
| Q6 | **P0** | 双事件重复：现状 GameMenu 退出同时入队 QuitGame + ResetGameState（两次 reset）；新方案两事件映射关系未定，可能重复切换或互相冲突 | 切换次数不一致 |
| Q7 | **P0** | 闪屏为**新增功能**且依赖 `ui/studio_logo.png`（不存在）；启动慢 1.5s、期间不响应输入，均为行为变化 | 新增行为/资源缺失 |
| Q8 | **P1** | ActiveContext 切换（替换 UiManager）时机未定；场景构造时注入 UiManager 字段，切换后旧引用失效（现状靠每次重建场景拿最新） | 主题/UI 引用错乱 |
| Q9 | **P1** | 输入更新迁移到 engine 层 `Input.update`，但虚拟输入/手柄 handler 是 platform 对象依赖 InstanceContent，engine 实现无法访问 | 输入失效 |
| Q10 | **P1** | Init 进 SM 栈的初始化细节未定：栈底场景如何创建、InitService 依赖（platform）如何注入 Init 场景、`PushGameStateExecute(MENU_MAIN)` 保留与否 | 启动流程错乱 |
| Q11 | **P1** | 空壳阶段 transitionTo 立即回 IDLE，"并发切换只执行第一个"的表述在空壳阶段不成立（会全部执行，与现状一致但文档误导） | 文档误导 |
| Q12 | **P1** | dispose 责任矩阵未覆盖多 ActiveContext（游戏 context 何时释放）、Input/EventBus、Stage/SpriteBatch 与 Graphics 抽象的关系 | 资源泄漏 |
| Q13 | P2 | SceneRegistry.create(state) 返回 Scene，但现状 registry 返回 GameRender 并 init_(container)；接口语义需对齐 | 实现偏差 |
| Q14 | P2 | 游戏 ActiveContext 的 AM 必须与 `playLocalData.getAnimationManager()` 是同一实例，否则游戏动画数据丢失 | 动画丢失 |
| Q15 | P2 | 13 种事件类需全部继承 engine Event 基类 + EventBus 泛型，迁移量大 | 工作量 |
| Q16 | P2 | topRender 用 Main 自己的 spriteBatch 二次 begin/end，与 Graphics 抽象下 batch 一致性需确认 | 渲染偏差 |

---

## P0 详述

### Q1【P0-新】SM 公开 API 帧内执行 → use-after-free

**现状证据**：所有场景在 update 里 `eventQueue.addEvent(...)` 触发切换，切换在 `GameHost.run` while 循环执行（update 返回之后）。场景自身永远不会在 update 中被 dispose。

**方案矛盾**：
- 设计文档第三节：空壳事件 → **"直接改为 SM 方法调用，不再经过 EventBus"**
- 实施计划第四节：`transitionTo`/`immediatelyTo` 直接 `executeSceneSwitch`（同步执行 dispose/create）

若场景 update 里直接 `sm.immediatelyTo(MENU_MAIN)`，则 `executeSceneSwitch` 在场景 update 栈内执行：`sceneStack.pop().dispose()` 释放当前场景对象，**随后该场景 update 剩余代码继续执行，访问已释放资源 → 崩溃**。这与现状"切换在场景 update 之外执行"根本不同。

**需决策**：
1. SM 公开 API 是否只允许在帧尾 flush（EventDispatcher 订阅回调）中调用，场景内仍走 `eventQueue.queue`？
2. 若允许场景直接调 SM 方法，方法内部必须先 `queue` 延迟到 flush，再在 flush 内执行 `executeSceneSwitch`——即 SM 方法本身不可同步执行切换。

### Q2【P0-新】场景对象生命周期模型差异

**现状**：`SceneStack` 存 `GameState` 枚举栈；`RenderPipeline` 只持一个渲染机（栈顶）。push B 时清 A、建 B；pop B 时清 B、**重新建 A**（重新 loadLayout/create）。

**新方案**：`Deque<Scene>` 场景对象常驻。push 建新对象；pop `sceneStack.pop().dispose()`，回到的栈顶第二对象未销毁（保留压栈前状态）。**POP 分支没有重建栈顶第二场景的逻辑**。

**行为差异**：例如 `MENU_MAIN → CONFIG_BASIC → pop`：现状 MENU_MAIN 重新构建（回到初始状态）；新方案若 MENU_MAIN 对象常驻，保留进入配置前的状态（焦点、滚动等）。需要确认产品预期是「弹栈重建」还是「保留状态」。

**需决策**：
1. 场景对象在压栈时是否 dispose（现状语义）还是常驻（保留状态）？
2. POP 分支弹栈后是否需要重新 create 栈顶第二场景？

### Q3【P0-新】executeSceneSwitch 丢失页面加载链

**现状证据**：场景创建 = `loadGameLayout`（isInGame 判断、themeManager 选择、缓存机制、firstInGame 强制重读）+ `loadGameConfig` + `updateGameRender`（clear + register + init_(container)）。这是场景切换的核心，且 `isInGame` 标志在此切换（决定双 AM/theme 归属）。

**新方案**：`executeSceneSwitch` 伪代码 `sceneRegistry.create(intent.getTargetState())` + `newScene.create()`，**只传 state，无 layout/config，无 isInGame 切换逻辑**。

**需决策**：场景创建参数与加载链（loadLayout/loadConfig/init）归属 SceneRegistry 还是 executeSceneSwitch？isInGame 标志（→ActiveContext 切换）何时更新？

### Q4【P0-新】immediatelyTo 硬编码 PUSH + QuitGame 语义

**现状证据**：QuitGame → `ResetGameStateExecute` → `resetGameState()` = `clear()` + `push(MENU_MAIN)`（**清空整个游戏栈**）。EnterGame/PlayGame → `PushGameStateExecute`（压栈）。

**新方案**：实施计划 `immediatelyTo(GameState state)` 伪代码硬编码 `SceneTransitionAction.PUSH`。若 QuitGame 也走 PUSH，则游戏栈 `[MenuMain, GameMenu, GamePlay]` 变为 `[MenuMain, GameMenu, GamePlay, MenuMain]`——**游戏状态残留**，与现状"清栈回主菜单"不一致。

**需决策**：immediatelyTo 的 API 签名（是否带 action 参数，或提供 immediatelyReset/immediatelyPush 等），QuitGame 对应 RESET 语义。

### Q5【P0-新】RESET 语义与现状不一致

**现状证据**：`resetGameState` = 清空栈 + push MENU_MAIN，栈永远只剩 `[MENU_MAIN]`。

**新方案**：设计文档 `executeSceneSwitch` RESET 分支 = `while (sceneStack.size() > 1) sceneStack.pop().dispose()`，**只清到栈底，且不 push MENU_MAIN**。若栈底是 Init 场景（启动时压入），RESET 后剩 `[Init]`，不是 `[MENU_MAIN]`，主菜单不会显示。

**需决策**：RESET 的目标（清空到栈底 or 固定 MENU_MAIN）；栈底场景是什么（Init or MenuMain）。

### Q6【P0-新】双事件重复切换

**现状证据**：GameMenu 退出按钮同时 `quitGame()`（addEvent QuitGame）+ `addEvent(ResetGameState)`，同一帧内 QuitGame 与 ResetGameState 各触发一次 reset（两次 reset 幂等，现状无 bug）。

**新方案**：QuitGame → immediatelyTo(RESET)，ResetGameState 空壳 → transitionTo(RESET)。两个事件都入队时，immediatelyTo 执行后 state=IDLE，transitionTo 又执行一次 reset → 重复创建 MenuMain；且 mapping 未说明两事件的协调。

**需决策**：保留现状"quitGame + ResetGameState 双事件"还是去重（如退出只走一个事件）？

### Q7【P0-新】闪屏为新增功能且资源缺失

**现状证据**：Main 无闪屏。`slash()` 仅置标志延迟一帧（解决任务栏图标丢失）。无 `ui/studio_logo.png`。

**新方案**：Phase 3b 引入 SPLASH 阶段（90 帧 ≈1.5s 居中绘制 logo + 不响应输入）。这是**新增功能**：启动变慢 1.5s、期间不响应输入、依赖不存在资源文件。

**需决策**：
1. 闪屏是否本次做（与引擎分离无关的新功能）？
2. 若做，logo 资源从哪来？SPLASH 帧数与"不响应输入"是否符合预期？

---

## P1 详述

### Q8【P1】ActiveContext 切换时机 + 场景持 UiManager 引用

现状场景构造时注入 `uiManager`（如 `new MenuMain(instanceContent.uiManager)`），靠每次重建场景拿最新实例。新方案若 ActiveContext 切换替换 UiManager，**已构造场景持有的字段引用不会更新**；且 ActiveContext 切换与 executeSceneSwitch 的时序未定（先进游戏再切 context，还是先切再建场景？）。

### Q9【P1】输入更新 engine/platform 边界

现状 inputUpdater = `instanceContent.update` = controllerInputHandler.update + virtualInputHandler.update（platform 层，依赖 InstanceContent）。P1-9 决策「engine 层 Input.update」无法访问 platform handler。需明确 Input 接口与虚拟输入/手柄 handler 的关系（handler 移 engine？或 Input 由 platform 实现并注入？）。

### Q10【P1】Init 进 SM 栈细节

现状 Init 由 `pushGameState(INIT)` 压栈（栈底），InitService 在 lambda 中创建且依赖 gameHost/configService/updateChecker（platform）。新方案 Init 场景作栈底：Application.create 时栈如何初始化？InitService 如何注入 Init 场景？`PushGameStateExecute(MENU_MAIN)` 在 SM 下对应 immediatelyTo(PUSH) 还是别的？设计文档"stepInit 由 SM.update 驱动"表述不准确（是 Init 场景的 update 驱动）。

### Q11【P1】空壳阶段 transitionTo 不会"互相拒绝"

实施计划说"transitionTo 之间互相拒绝（第一个执行，后续丢弃）"。但空壳阶段 transitionTo 立即回 IDLE，同一帧 flush 内第二个 transitionTo 时 state 已是 IDLE → **会执行**。现状双事件模型下连续 Push 也会全部执行。故空壳阶段行为与现状一致（不崩溃），但文档表述误导，需修正为"真动画阶段才互相拒绝"。

### Q12【P1】dispose 责任矩阵不完整

现状 dispose 链：Main.dispose → InstanceContent.dispose → updateChecker + gameHost（renderPipeline/sceneStack/configService/eventDispatcher/gameLogic/playLocalData/playRuntimeData）+ uiManager + animationManager + graphicsManager + audioManager。新方案未覆盖：多个 ActiveContext（游戏 context 何时 dispose）、Input、EventBus、SceneRegistry 内场景的 dispose、Main 的 Stage/SpriteBatch 与 Graphics 抽象的关系。

---

## P2 / 提示

- **Q13**：SceneRegistry.create(state) 与现状 registry 返回 GameRender + init_(container) 的接口语义需对齐。
- **Q14**：游戏 ActiveContext 的 AM 必须与 `playLocalData.getAnimationManager()` 同一实例，否则游戏内动画（combineAnimation 等）数据丢失（P0-3 深层联动）。
- **Q15**：13 种事件类迁移到 EventBus 泛型需全部继承 engine Event 基类，工作量大。
- **Q16**：topRender 用 Main 自身 spriteBatch 二次 begin/end，需与 Graphics 抽象确认同一 batch 一致性。

---

## 非决策性表述修正建议

| 位置 | 原文 | 修正建议 |
|------|------|---------|
| 设计文档第二节 | 帧循环顺序含 Input.update | 确认 Input.update 与 instanceContent.update 的关系（Q9） |
| 设计文档第三节 | "空壳事件直接改为 SM 方法调用" | 与帧间红线矛盾，需明确 SM 方法调用时机（Q1） |
| 实施计划 Phase 3a | "transitionTo 互相拒绝" | 空壳阶段不成立（Q11） |
| 设计文档第四节 | TM 状态机 OUT/IN_TRANSITIONING | 与"空壳退化 IDLE→EXECUTING→IDLE"并存，保留但标注当前不激活 |

---

## 待用户决策清单（✅ 全部已确认，2026-09-02）

11 项决策已由用户逐项确认，结果见本文档头部「决策回写状态」表，已回写设计文档 `2026-08-29-engine-separation-design.md` 与实施计划 `2026-09-01-engine-separation-implementation-plan.md`。

- Q1：SM 公开 API 调用时机 —— 场景仍走 queue，SM 方法只在帧尾 flush 被调用
- Q2：场景对象生命周期 —— 弹栈重建（场景对象不常驻，枚举栈 + 仅栈顶活跃）
- Q3：executeSceneSwitch 页面加载链 —— 留在 SM 内部
- Q4：immediatelyTo API 签名 —— 带 action；QuitGame → RESET 清栈回主菜单
- Q5：RESET 目标 —— 清空全部 + 压 MENU_MAIN
- Q6：QuitGame 双事件 —— 去重，只走 QuitGame
- Q7：闪屏 —— 本次做，logo 留 //TODO 占位，SPLASH 期间忽略输入
- Q8：场景获取 UiManager —— 每次从 ActiveContext 动态取
- Q9：输入更新边界 —— Input 接口由 platform 实现注入
- Q10：Init 进栈 —— SM 栈底压 Init，平台注入 InitService
- Q12：dispose 责任矩阵 —— 设计文档补全责任矩阵

（Q11 属文档表述修正，回写时顺带处理，无需决策。）
