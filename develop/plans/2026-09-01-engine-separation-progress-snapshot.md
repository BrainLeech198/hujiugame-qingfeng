# 引擎分离工作进度快照（2026-09-01）

> **文档定位**：跨会话接续断点。记录引擎分离改造的当前工作状态、已完成项、待办项，供次日新会话快速恢复上下文后无缝继续。
>
> **文档结构**：一句话状态 → 文档关系图 → 15 项决策汇总 → 已完成工作 → 待办清单 → 接续入口指引 → 注意事项
>
> **更新规范**：随工作推进刷新；每阶段开始/结束时更新"已完成/待办"两节；任务完成后从待办勾掉。

---

## 一句话状态

引擎分离方案已完成代码级审查（3 P0 + 12 P1/P2），**全部 15 个问题已由用户确认决策，并已回写设计文档 + 实施计划**。下一步：① 更新 `temp/CLAUDE_MEMORY.md` + `DOCUMENTATION_INDEX.md` → ② 用户确认后提交文档（分内容拆分提交，不碰官网改动）→ ③ 开始 Phase 1（engine 包骨架，纯新增零风险）。**3 个 P0 已全部解决，满足"P0 解决前不进入 Phase 1"的前置条件。**

---

## 相关文档（含状态）

| 文档 | 状态 | 说明 |
|------|------|------|
| `develop/plans/2026-08-29-engine-separation-design.md` | ✅ 决策已回写 | 设计方案：8 设计节 + 6 阶段迁移 + 3D 扩展路径 |
| `develop/plans/2026-09-01-engine-separation-implementation-plan.md` | ✅ 决策已回写 | 6 阶段实施计划（Phase 3a/3b/5 已按决策修正） |
| `develop/plans/2026-09-01-engine-separation-plan-review.md` | ✅ 审查报告 | 代码级逐帧推演，3 P0 + 12 P1/P2 问题清单 + 过时认知修正 |
| `develop/plans/2026-09-01-engine-separation-options.md` | ✅ 决策记录表已填满 | 15 个问题各 2-3 方案 + 用户选择记录 |
| `develop/plans/2026-09-01-engine-separation-progress-snapshot.md` | 🆕 本文档 | 进度断点 |
| `temp/CLAUDE_MEMORY.md` | 🔄 待更新 | gitignored 本地记忆，需同步"决策完成"状态 |

---

## 15 项决策汇总（用户已全部确认）

| # | 问题 | 选定方案 | 关键含义 |
|---|------|---------|---------|
| **P0-1** | 立即切换语义丢失 | 双 API + 强制打断 | SM 提供 `transitionTo`（UI 带过渡）+ `immediatelyTo`（业务硬切换，强制打断进行中过渡，等效现状 `*_EXECUTE` 绕过 TM） |
| **P0-2** | QuitGame 崩溃窗口 | 零改动 + 同帧 | quitGame 释放逻辑不改（仍先 addEvent 后同步释放）；QuitGame 走 `immediatelyTo` + 帧尾 flush，保证"释放数据 → 切 MENU_MAIN"同帧完成 |
| **P0-3** | 双 AM 矛盾 | SM 持 ActiveContext | SM 持 EngineContext，过渡驱动时每次动态 `getActiveContext().getAnimationManager().getTransitionManager()`，**不缓存 AM/TM 引用**，切换后自然感知 |
| P1-4 | 动画引擎立项 | 解耦单独立项 | Phase 3a 只做架构重构，TM 保持空壳；真正动画执行引擎后续独立阶段设计 |
| P1-5 | 事件处理时机 | 帧尾 flush | flush 在 update 之后 render 之前；场景 update 入队事件**当帧**处理，等效现状"同帧 while" |
| P1-6 | publish/queue 语义 | 统一走 queue | 场景切换请求统一 `queue`，帧尾 flush 统一执行；`publish` 仅限必须立即生效的事件 |
| P1-7 | SafePostRunnable | 保留 SafePost | RefreshUi 的"下一帧释放旧 UiManager"机制必须保留（防渲染帧内 dispose 崩溃） |
| P1-8 | 过渡视觉 | 暂缓 | 等架构重构完成后，在 AM/TM 设计阶段再定淡出淡入视觉 |
| P1-9 | 输入更新 | 引擎层 Input.update | Application.update 调 `Input.update(delta)`，替代原 `RenderPipeline.inputUpdater`（虚拟输入/手柄） |
| P1-10 | InitService 驱动 | Init 进 SM 栈 | Init 场景进 SM 栈，`stepInit()` 由 SM.update 分帧驱动；完成后入队 `immediatelyTo(MENU_MAIN)` |
| P2-11 | Pop inState 时序 | 执行时计算 | 弹栈前 `getSecondGameState()` 计算目标状态（栈顶第二） |
| P2-12 | 场景工厂依赖 | 平台层工厂类 | lambda 捕获的依赖由平台层工厂类注入 SceneRegistry |
| P2-13 | dispose 顺序 | dispose 责任矩阵 | Main/InstanceContent/EngineContext/各 Manager 释放职责按责任矩阵对照，避免 double-dispose / use-after-free |
| P2-14 | topRender 归属 | 留在 Main | 虚拟输入提示覆盖层归属 Main，RUNNING 阶段 = application.update + render + topRender |
| P2-15 | 过渡期间行为 | 暂缓 | 联动 P1-8，动画引擎阶段再定 |

---

## 已完成工作

### 1. 代码级审查（已完成）
- 逐帧推演验证方案对现状代码的真实假设，发现关键过时认知：**TransitionManager 已是空壳**（commit 3952fb5 删除 1195 行动画逻辑），方案"痛点 3：过渡状态机由场景文件推进"不成立
- 确认场景切换**双路径**：空壳路径（场景 UI 用 PushGameState 走 TM）+ 直接执行路径（EnterGame/QuitGame/PlayGame/InitService 用 `*_EXECUTE` 绕过 TM，无过渡硬切换）
- 3 P0 + 12 P1/P2 全部定位并代码验证，审查报告落盘 `plan-review.md`

### 2. 决策确认（已完成）
- 用户对全部 15 个问题逐一决策，`options.md` 决策记录表已填满

### 3. 决策回写（已完成）
- **设计文档第二节**：每帧流程改帧尾 flush（update → SM.update → Input.update → EventBus.flush → render）
- **设计文档第三节**：EventBus publish（同步，仅限必须立即）/ queue（入队，帧尾 flush）/ flush（帧尾冲刷）；映射规则补双 API + 业务事件统一 queue
- **设计文档第四节**：SM 持 EngineContext 动态取 ActiveContext 的 AM/TM + `transitionTo`/`immediatelyTo` 双 API + 立即切换打断过渡 + 过渡保护仅限 transitionTo + 帧间红线（executeSceneSwitch 只能在帧尾 flush 执行）
- **设计文档第七节**：`new SceneManager(this, eventBus)`、依赖图改 EngineContext、ActiveContext 描述（SM 不缓存 AM/TM，每次动态取）
- **实施计划 Phase 3a**：痛点修正（TM 空壳 / 双路径）、改动文件（TM 保持空壳仅补 abortTransition、SM 双 API + 持 EC）、事件映射表（业务事件 → immediatelyTo + queue）、并发切换处理（transitionTo 拒绝 / immediatelyTo 打断）、帧间约束改帧尾 flush、风险表 + 检查清单（移除过时认知条目，加 P0 验证项）
- **实施计划 Phase 3b**：Application.update = SM.update + AM.update + Input.update + 帧尾 flush；Init 进 SM 栈说明；topRender 留 Main；风险表补帧尾时序/InitService/输入更新/topRender；检查清单补 P1-5/9/10/P2-14 验证项
- **实施计划 Phase 5**：ActiveContext 切换注释（SM 每次从 EC 动态取，感知切换）+ 风险表 + 检查清单补 P0-3

---

## 待办清单

### 下一步（立即）
- [ ] 更新 `temp/CLAUDE_MEMORY.md`：记录 15 决策全部确认并回写完成，状态从"审查中"改为"决策完成，待提交"
- [ ] 更新 `DOCUMENTATION_INDEX.md`：重构计划分类补 4 个条目（implementation-plan / plan-review / options / progress-snapshot）
- [ ] 用户确认后提交：设计文档 + 实施计划 + 审查报告 + options + 进度快照 + CHANGELOG（**分内容拆分提交**）

### 之后
- [ ] 开始 Phase 1：创建 engine 包骨架，定义接口（Scene/EventBus/Graphics/Audio/Input），纯新增零风险，不改现有代码
- [ ] Phase 2-5 逐阶段实施（详见实施计划，每阶段有检查清单）

---

## 接续入口指引

明天新会话恢复工作路径：
1. 读 `temp/CLAUDE_MEMORY.md`（本地记忆，含本快照位置与设计决策）
2. 读本快照（当前状态 + 15 决策汇总 + 待办）
3. 读 `develop/plans/2026-08-29-engine-separation-design.md` + `2026-09-01-engine-separation-implementation-plan.md`（方案细节）
4. 需回顾问题背景时读 `plan-review.md`（审查报告）+ `options.md`（方案选项与选择）

---

## 注意事项

- **提交范围红线**：工作区混着一大批官网重构未提交改动（docs/ 大量删除、vite、index.html、src/、public/、package.json、package-lock.json 等）。提交引擎分离时**只提交 develop/plans 下 5 个 md 文件 + CHANGELOG 条目**，不碰官网改动。`temp/CLAUDE_MEMORY.md` 被 gitignore，无需提交
- **CHANGELOG 提交规则**（CLAUDE.md）：CHANGELOG 条目不独立提交；每个内容改动 = 一笔提交（改动文件 + 该改动对应 CHANGELOG 条目一并提交），按内容逐条拆分
- **P0 已全部解决**：3 个 P0 决策已回写，满足"P0 解决前不进入 Phase 1"的前置条件，可以进入 Phase 1
- **动画引擎单独立项**：Phase 3a 不改 TM 动画逻辑（保持空壳，行为与现状完全一致）；真正的淡出淡入动画引擎 + 过渡视觉（P1-8）+ 过渡期间场景行为（P2-15）都在后续动画引擎设计阶段处理
