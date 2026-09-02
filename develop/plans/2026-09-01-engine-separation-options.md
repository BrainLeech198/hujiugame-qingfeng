# 引擎分离审查问题方案选项

> **文档定位**：`2026-09-01-engine-separation-plan-review.md` 中每个问题的候选解决方案，每个问题给出 2-3 个方案 + 权衡 + 推荐，供选择。
>
> **文档结构**：P0 优先 → P1 → P2；每个问题 = 问题回顾 + 方案 A/B/C（做法/优点/缺点）+ 推荐
>
> **关联文档**：`2026-09-01-engine-separation-plan-review.md`（审查报告）
>
> **更新规范**：选定方案后勾选"决策记录表"并回写设计文档/实施计划。

---

## P0-1：立即切换语义丢失

**问题回顾**：EnterGame/QuitGame/PlayGame/InitService 当前走 `*_EXECUTE` 直接硬切换（无过渡）。方案只给 SM 带过渡 API + 过渡保护，会改变行为，且 QuitGame 可能被过渡保护拒绝导致卡死。

**现状逻辑（之前没 bug 的原因）**：
```
业务事件（EnterGame/QuitGame/PlayGame/InitService）
  → addEvent(new PushGameStateExecute(state)) 或 ResetGameStateExecute
  → EventDispatcher 处理 *_EXECUTE 时【直接】调 sceneStack.pushGameState/popGameState/resetGameState
  → 绕过 TransitionManager，同帧 while 循环执行（render 前），帧间安全，无过渡
```
关键：现状是**双路径并存**——空壳路径（UI 用 PushGameState，走 TM）+ 直接执行路径（业务事件用 *_EXECUTE，绕过 TM）。两条路径互不干扰。

**方案 A — 双 API（保留双路径本质）**
- 做法：SM 提供 `transitionTo(action, state)`（带过渡，对应空壳路径）和 `immediatelyTo(action, state)`（立即切换，对应直接执行路径）。立即切换直接执行 sceneStack 操作、绕过过渡状态机；若过渡进行中，先复位状态机再执行（硬切换优先级高）。
- 优点：保留现状"业务事件无过渡"行为；两条路径语义清晰映射现状双路径；业务事件调用点从 `addEvent(Execute)` 改为 `sm.immediatelyTo(...)`，改动小
- 缺点：SM 公开 API 多一组；立即切换复位过渡状态机的边界要定义（强制打断 or 排队，见下）

**方案 B — 全部统一带过渡**
- 做法：业务事件也走带过渡 API，所有切换都有淡出淡入。
- 优点：API 单一，状态机简单
- 缺点：**改变现有视觉行为**（进入/退出游戏会出现过渡动画）；QuitGame 过渡中被拒仍要解决

**方案 C — 保留 *_EXECUTE 事件路径**
- 做法：业务事件仍发 `*_EXECUTE` 事件，SM 订阅后直接执行栈操作，不启动过渡状态机。
- 优点：业务事件调用点**零改动**（仍 addEvent(Execute)）
- 缺点：SM 状态机要兼容"跳过过渡直接执行"路径；事件系统仍保留一组 Execute 事件类型（13→4 映射不能彻底合并）

**方案 D — 立即切换遇过渡：强制打断（推荐语义）**
- 做法：在方案 A 基础上明确——立即切换触发时，若 SM 处于过渡中（FADE_OUT/FADE_IN），**强制复位状态机 + 立即执行切换**，不等过渡完成。
- 优点：业务硬切换永远立即生效（用户点退出不会被卡）；行为可预期
- 缺点：过渡动画被中途截断（视觉上淡出到一半跳走，可接受，因为是硬切换）
- 备选子语义：立即切换遇过渡时**排队**（等过渡完成再切）——更"礼貌"但延迟不确定（用户点退出可能延迟），不推荐

---

## P0-2：QuitGame 崩溃窗口

**问题回顾**：`quitGame()` 先 addEvent(QuitGame) 再同步释放游戏数据/资源/配置。当前同帧切换安全；重构后 FADE_OUT 多帧期间渲染已释放数据 → 崩溃。

**现状逻辑（之前没 bug 的原因）**：
```
帧 N：场景 update → 用户点退出 → quitGame()
  ├── JsonTextParser.setLanguageManager(启动器)      ← 恢复语言
  ├── addEvent(new QuitGame())                       ← 入队
  ├── disposeData() / disposeResource() / disposeGameConfig()   ← 同步立即释放游戏数据/资源/配置
帧 N：while 循环（同帧）处理 QuitGame → resetGameState → 切到 MENU_MAIN
  ├── MENU_MAIN isInGame=false，走启动器 themeManager/audioManager/graphicsManager
  ├── 【不依赖已释放的游戏数据】→ 安全
帧 N：render 主菜单
```
安全的关键 = **释放数据和切到主菜单同一帧完成**，且**主菜单完全依赖启动器资源**（MenuMain.render 只用 instanceContent 的 audioManager/graphicsManager，不用 playLocalData）。

**方案 A — 保持现状释放顺序 + 立即切换 + 帧尾 flush（推荐，改动最小）**
- 做法：quitGame 的释放逻辑**零改动**（仍先 addEvent 后同步释放）；QuitGame 走立即切换（P0-1 方案 A 的 immediatelyTo，绕过过渡）；配合 P1-5 帧尾 flush，保证"addEvent → 同帧 flush 切到 MENU_MAIN"。
- 优点：释放逻辑完全不改，时序与现状逐帧等价；只需 SM 提供立即切换 + flush 位置正确
- 缺点：依赖"立即切换 + 帧尾 flush 保证同帧"；若未来加异步释放需重新评估（当前不需要）
- **前提**：切到 MENU_MAIN 前 activeContext 已切回启动器（P0-3 联动），主菜单渲染用启动器资源

**方案 B — 释放延后到切换完成后**
- 做法：quitGame 只入队 QuitGame；释放封装成 Runnable 挂在场景切换完成（帧间）后执行。
- 优点：释放时机由切换进度决定，双保险
- 缺点：释放变异步，调用方假设（释放完即退出）要调整；回滚顺序要重设计；**改动比方案 A 大**

**方案 C — 先切换后释放（回调驱动）**
- 做法：先入队 QuitGame 切到主菜单，释放动作由 SM 的 onSceneChanged 回调触发。
- 优点：时序最清晰
- 缺点：需要 SM 新增切换完成回调机制；控制流分散；**改动最大**

---

## P0-3：双 AM 矛盾

**问题回顾**：启动器/游戏各有独立 AnimationManager，按 isInGame 动态选（EventDispatcher.java:287）。方案 ActiveContext 含 AM，但 SM 构造固定持有启动器 AM，切换后游戏内过渡用错 AM。

**方案 A — SM 动态获取 AM/TM（推荐）**
- 做法：SM 不持有 AM 引用，每次通过 `engineContext.getActiveContext().getAnimationManager()` 动态获取，再取 TM。
- 优点：切换 ActiveContext 后自动用新 AM，与现有"按 isInGame 选 AM"完全一致；架构上 ActiveContext 概念真正生效
- 缺点：每帧过渡更新时多做一次间接访问（可忽略）；SM 与 EngineContext 的访问路径要定

**方案 B — SM 持有 ActiveContext 引用**
- 做法：SM 持 ActiveContext（可替换），从 activeContext 取 AM。等价于 A，只是引用路径不同。
- 优点：SM 不依赖完整 EngineContext，依赖更小
- 缺点：与 A 差异极小，二选一即可

**方案 C — 统一单 AM**
- 做法：取消启动器/游戏双 AM，全项目一个 AnimationManager。
- 优点：架构最简
- 缺点：**动摇了动画系统设计**（游戏内 PlayLocalData 的 AM 为什么存在需要重新论证）；游戏动画数据归属要重设计，风险大，非本次范围

---

## P1-4：过渡方法过时认知 / 工作量低估

**问题回顾**：过渡方法（fadingOut 等）已删，TM 是空壳。"重写 TM"实为**新建动画执行引擎**。

**方案 A — Phase 3a 解耦，动画引擎单独立项（推荐）**
- 做法：Phase 3a 只做"SM 接管场景栈 + 事件映射 + 过渡保护"的架构重构，TM 保持空壳 + 立即切换；淡出/淡入动画执行引擎作为独立阶段（Phase 3a2 或后续动画系统设计）再做。
- 优点：架构重构与动画引擎解耦，Phase 3a 风险可控；避免在一个阶段同时动架构 + 新建复杂动画系统
- 缺点：Phase 3a 期间仍是"无动画切换"（与现状一致），过渡动画延后

**方案 B — Phase 3a 直接新建完整动画引擎**
- 做法：Phase 3a 就实现 TM.updateOut/updateIn 完整淡出淡入。
- 优点：一步到位，过渡动画尽早落地
- 缺点：工作量放大；架构重构 + 动画引擎 + 逐帧推演全挤在一个阶段，出问题难定位

**方案 C — 先做"假过渡"验证时序**
- 做法：Phase 3a 用固定 N 帧黑幕/白幕（无真实插值）替代淡出淡入，先验证"跨帧过渡"的时序正确性（P0-2 的释放窗口、P1-5 的帧序），动画引擎后续替换。
- 优点：用最小成本验证跨帧时序，暴露 P0-2 类问题；过渡视觉占位可后续换真
- 缺点：多一次替换成本；黑幕体验临时

---

## P1-5：事件处理时机（同帧 → 下帧）

**问题回顾**：当前 GameHost.run 的 while 循环在 update 后（同帧处理）。方案 flush 在 update 前（下帧处理）。所有 addEvent 触发点延迟一帧。

**方案 A — 接受下帧 flush（方案现状）**
- 做法：维持 Application.update 第一行 flush，接受一帧延迟，重做全部逐帧推演。
- 优点：代码位置最清晰（每帧开头统一处理）
- 缺点：所有触发点延迟一帧；脚本连续切换跨帧累积，与过渡保护交互需重新推演

**方案 B — 帧尾 flush（推荐）**
- 做法：flush 放在 update 之后、render 之前（模仿当前 GameHost.run 的 while 位置），场景 update 入队的事件当帧处理。
- 优点：时序最接近现状，行为变化最小；"场景 update 里 addEvent → 同帧切换"语义保留
- 缺点：flush 位置夹在 update/render 之间，Application 帧结构多一个显式步骤

**方案 C — publish 立即派发**
- 做法：场景切换事件用 EventBus.publish 在 update 内立即处理。
- 优点：无延迟
- 缺点：update 中途执行栈操作，需严格确认不在 render 中途；与"切换统一在帧间"的红线张力大

---

## P1-6：EventBus publish/queue 语义

**问题回顾**：场景切换事件走 publish 还是 queue 未明确。

**方案 A — 场景切换统一走 queue（推荐，联动 P1-5）**
- 做法：UI/脚本触发的切换入队，由（帧首或帧尾）flush 统一执行。
- 优点：切换时机收敛到 flush 单点，符合"帧间切换"红线；易加过渡保护
- 缺点：若走"帧首 flush"则延迟一帧（P1-5 方案 A），建议联动"帧尾 flush"（P1-5 方案 B）

**方案 B — 统一走 publish 立即**
- 做法：切换事件立即派发。
- 优点：无延迟
- 缺点：帧内任意时刻可触发栈操作，帧间约束难保证

**方案 C — 区分两类**
- 做法：业务硬切换走 publish 立即，UI/脚本走 queue。
- 优点：灵活
- 缺点：两条路径语义不一，推演复杂

---

## P1-7：RefreshUiManager 的 SafePostRunnable

**问题回顾**：REFRESH_UI_MANAGER 用 SafePostRunnable 把旧 UiManager 延迟到下一帧释放（防渲染帧内释放崩溃）。方案未说明如何保留。

**方案 A — 保留 SafePostRunnable（推荐）**
- 做法：SafePostRunnable 作为引擎 util 保留，RefreshUiManager 处理时继续用它延迟释放旧 UiManager；refreshGameState 的协调者明确为 SM（提供 refresh 场景能力）。
- 优点：改动最小，机制成熟
- 缺点：SafePostRunnable 用 Gdx.app.postRunnable（下一帧），与 EventBus 的帧模型并存，需说明两者关系

**方案 B — EventBus 内部事件模拟**
- 做法：入队一个"释放旧 UiManager"的内部事件，由下帧 flush 处理。
- 优点：统一走 EventBus，模型单一
- 缺点：flush 是"帧首/帧尾"统一派发，释放时机从"下一帧任意时刻"变"下一帧 flush 时刻"，需确认不影响正确性；若 P1-5 选"帧首 flush"，释放会更早，需推演

**方案 C — SM 切换完成回调触发**
- 做法：旧 UiManager 释放挂在 SM 场景刷新完成回调。
- 优点：时机精确
- 缺点：新增回调机制；refresh 场景与普通切换的完成回调要区分

---

## P1-8：废弃 GameRenderPipeLineState + 过渡视觉

**问题回顾**：RenderPipeline 内部 switch(state) 依赖 GameRenderPipeLineState；方案废弃它但未说明 RenderPipeline 适配，且过渡视觉（淡出期间显示什么）未定。

**方案 A — 过渡视觉用 TM 画全屏淡色层（推荐）**
- 做法：GameRenderPipeLineState 废弃，RenderPipeline 删 TRANSITION 分支只留 NORMAL；过渡期间场景 render 照常，TM 在场景之上画全屏 alpha 渐变层（0→1→0），即"淡出到色层 → 切换 → 色层淡入露出新场景"。
- 优点：过渡视觉不依赖场景文件，实现最简单；色层方案可平滑升级为截图渐变
- 缺点：黑幕/色幕过渡，无场景内动画效果（后续可换）

**方案 B — 过渡期间场景冻结 + 缓存帧**
- 做法：淡出时场景 render 定格（或截取最后一帧），TM 对缓存帧做 alpha 渐变。
- 优点：视觉"场景冻结后淡出"更精致
- 缺点：需要帧缓冲截图，libGDX 实现复杂，占用显存；工作量偏大

**方案 C — 场景 transitionRender 驱动过渡视觉**
- 做法：保留 GameRender.transitionRender，过渡视觉由各场景自己画。
- 优点：场景可定制过渡效果
- 缺点：又回到"场景耦合过渡"（正是要消除的）；每个场景都要实现

---

## P1-9：输入更新纳入主循环

**问题回顾**：RenderPipeline.inputUpdater = instanceContent.update（虚拟输入/手柄）。Application 主循环伪代码未纳入。

**方案 A — Application.update 末尾加 Input.update（推荐）**
- 做法：Application.update 结构为 `flush → SM.update → AM.update → Input.update`，Input.update 封装原 inputUpdater（虚拟输入/手柄推进）。
- 优点：输入推进在引擎层统一，位置明确
- 缺点：Input 接口要扩展 update 方法（或单独 InputUpdater 组件）

**方案 B — 输入更新留在场景层**
- 做法：每个场景自己 update 虚拟输入。
- 优点：引擎不需要管
- 缺点：重复代码；场景切换期间（过渡）输入推进缺失，虚拟输入网格可能卡住

**方案 C — render 前固定步骤**
- 做法：Input.update 放 Application.render 开头。
- 优点：与渲染相邻
- 缺点：输入推进逻辑上属"更新"而非"渲染"，放 render 前语义略怪

---

## P1-10：InitService 分帧驱动

**问题回顾**：InitService 由 Init 渲染机每帧调 stepInit，跨多帧异步，完成后直接入队 PushGameStateExecute(MENU_MAIN)。主循环替换后未安排。

**方案 A — Init 场景进 SM 栈（推荐）**
- 做法：Init 场景作为普通场景进 SM 栈，SM.update(IDLE) 调 Init.update → stepInit；完成时 SM 立即切到 MENU_MAIN（走 P0-1 的 immediatelyTo）。
- 优点：符合"场景生命周期"设计；InitService 作为 Init 场景依赖由平台层注入
- 缺点：初始化期间 SM 栈非空，过渡保护要允许 Init 阶段

**方案 B — Application 持有 InitService**
- 做法：Application 在懒初始化阶段直接驱动 InitService，不进场景栈。
- 优点：初始化与场景栈解耦
- 缺点：Application（引擎层）持有 InitService（平台层）破坏依赖方向；或用接口抽象，增加复杂度

**方案 C — 保持 RenderPipeline 驱动，Phase 5 再迁**
- 做法：Phase 3b 前 InitService 仍由旧路径驱动，最后迁移。
- 优点：过渡期风险小
- 缺点：主循环替换后旧路径被删，必须在此阶段同步迁，实际不可选

---

## P2-11：PopGameState 的 inState

**方案 A — 执行时计算（推荐）**
- 做法：SM.pop() 在 executeTransition 时（弹栈前）调 getSecondGameState() 计算目标。
- 优点：栈状态为准，过渡期间栈被保护（过渡保护），计算准确
- 缺点：无

**方案 B — 空壳事件时缓存**
- 做法：空壳事件记录时缓存 inState（当前做法）。
- 优点：与现状一致
- 缺点：若过渡期间栈被其他操作改变（当前有过渡保护，理论上不会），缓存过期

---

## P2-12：场景工厂 lambda 迁移

**方案 A — 平台层 SceneFactory（推荐）**
- 做法：SceneRegistry 定义工厂接口，平台层实现 `GameSceneFactory`（依赖 EngineContext），注册到 SceneRegistry；现有 lambda 逻辑平移为工厂方法。
- 优点：依赖注入清晰，引擎层不依赖平台
- 缺点：工厂类要写一批 boilerplate

**方案 B — 平台层 lambda 注册**
- 做法：SceneRegistry 是注册表接口，平台层启动时用 lambda 注册（捕获 EngineContext）。
- 优点：改动小，lambda 直接平移
- 缺点：注册代码仍依赖平台上下文，依赖方向靠约定保证

**方案 C — 反射/注解扫描**
- 做法：自动扫描场景类。
- 优点：零注册代码
- 缺点：反射性能/启动开销，过度设计

---

## P2-13：dispose 顺序

**方案 A — dispose 责任矩阵（推荐）**
- 做法：先落一份"资源 → 持有者 → 释放顺序"矩阵，明确 SpriteBatch/Stage 归 Main、渲染机归 RenderPipeline/SM、各 Manager 归 EngineContext/ActiveContext；EngineContext.dispose 按依赖逆序。
- 优点：先梳理再动手，防 double-dispose / use-after-free
- 缺点：梳理工作，无直接功能

**方案 B — dispose 收敛到 EngineContext**
- 做法：所有引擎资源统一 EngineContext.dispose，Main 只 dispose 引擎。
- 优点：单一入口
- 缺点：SpriteBatch/Stage 在 Main 创建、平台依赖，收敛有阻力

**方案 C — 保持现状增量调整**
- 做法：维持 InstanceContent.dispose 顺序，按重构增量微调。
- 优点：改动最小
- 缺点：重构后持有关系变化，原顺序可能失效

---

## P2-14：topRender 归属

**方案 A — 留在 Main（推荐）**
- 做法：topRender 作为 libGDX 层覆盖，在 Application.render 之后由 Main 调用。
- 优点：改动最小；覆盖层本来就是顶层 HUD，属于窗口级而非场景级
- 缺点：Main 多一行调用

**方案 B — 进 Application**
- 做法：Application 提供 OverlayRender 接口，平台层注册覆盖层。
- 优点：覆盖层在引擎统一调度
- 缺点：为一行调用引入接口，过度设计

**方案 C — 进 UiManager**
- 做法：虚拟输入提示作为 UI 一部分，UiManager.render 绘制。
- 优点：UI 内聚
- 缺点：虚拟输入提示是"输入覆盖层"不是"UI 控件树"，语义不符；UiManager 又要管覆盖层，职责扩大

---

## P2-15：过渡期间场景行为

**问题回顾**：设计文档 SM.update 在 FADE_OUT/FADE_IN 不调 currentScene.update，但 render 行为、输入屏蔽、脚本暂停未完整定义。

**方案 A — 冻结 + TM 色层（联动 P1-8 方案 A，推荐）**
- 做法：过渡期间 update/render 都停（场景冻结），TM 画全屏 alpha 层；输入屏蔽（Input.update 不派发给场景）。
- 优点：最简；冻结 + 色层 = 标准"淡出淡入"视觉；脚本/输入天然暂停
- 缺点：淡出时场景定格（无动画），某些场景可能突兀

**方案 B — 场景继续 update/render + TM 叠加**
- 做法：过渡期间场景照常 update/render，TM 只叠加 alpha 层。
- 优点：淡出时场景仍在动，视觉自然
- 缺点：P0-2 的释放窗口风险（淡出期间场景 update 访问已释放数据）；需要场景在过渡期间保持数据有效

**方案 C — update 停、render 继续**
- 做法：过渡期间 update 停（逻辑冻结），render 照常 + TM 叠加。
- 优点：介于 A/B，画面定格但非黑幕
- 缺点：render 若访问动态数据仍需有效；语义略混合

---

## 决策记录表

| # | 问题 | 选定方案 | 备注 |
|---|------|---------|------|
| P0-1 | 立即切换语义 | **A：双 API + 强制打断** | 用户已确认；立即切换遇过渡强制复位状态机 |
| P0-2 | QuitGame 释放时机 | **A：零改动 + 同帧** | 用户已确认；释放逻辑不改 + 立即切换 + 帧尾 flush |
| P0-3 | 双 AM 矛盾 | **B：SM 持 ActiveContext** | 用户已选 |
| P1-4 | 动画引擎立项 | **A：解耦单独立项** | 用户已确认；Phase 3a 只做架构，动画引擎后续独立阶段 |
| P1-5 | 事件处理时机 | **B：帧尾 flush** | 用户已选 |
| P1-6 | publish/queue | **A：统一走 queue** | 用户已确认；帧尾 flush 统一执行 |
| P1-7 | SafePostRunnable | **A：保留 SafePost** | 用户已确认 |
| P1-8 | 过渡视觉 | **暂缓** | 用户已确认：等架构重构完成后，在 AM/TM 设计阶段再定过渡视觉 |
| P1-9 | 输入更新 | **A：引擎层 Input.update** | 用户已确认 |
| P1-10 | InitService 驱动 | **A：Init 进 SM 栈** | 用户已确认 |
| P2-11 | Pop inState | **A：执行时计算** | 用户已确认 |
| P2-12 | 场景工厂 | **A：平台层工厂类** | 用户已确认 |
| P2-13 | dispose 顺序 | **A：dispose 责任矩阵** | 用户已确认 |
| P2-14 | topRender | **A：留在 Main** | 用户已确认 |
| P2-15 | 过渡期间行为 | **暂缓** | 联动 P1-8，动画引擎阶段再定 |
