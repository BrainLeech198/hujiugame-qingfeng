# 今日临时进度 — 页面切换控件级动画配置确认 + UiKind 重构

> **状态:** 2026-08-09 工作进度记录（当时尚未提交）。
>
> **2026-08-18 更新：** ① menu_main 及 menu_list/menu_load/config_basic/config_display 的控件级 `animation` 配置已实现（smooth_move + schedule 串行 + synchronization 同步 + 预偏移 + 时间戳计时），本进度文档的语义结论已落地并提交。② UiKind 重构（含 UiObject/findObject）已提交。
>
> 待解决问题见 [2026-08-07-page-transition-animation.md](2026-08-07-page-transition-animation.md)（淡出→淡入卡顿计入淡入计时）。
>
> 关联方案：[2026-08-07-page-transition-animation.md](2026-08-07-page-transition-animation.md)（页面级过渡动画，控件级配置是该方案的落地深化）。

---

## 一、页面切换控件级动画（menu_main/config.json）

### 配置结构（用户实际写入）

```
animation
├── fade_in   切入动画
│   ├── default           通用
│   │   ├── duration 0.2  整段动画窗口
│   │   └── action
│   │       ├── synchronization  同步组：backgroundPicture / logo → type:none（保持不动）
│   │       └── schedule         串行组：start/create/config/quit → smoothMove 向左 100、duration 0.05
│   └── from_page.config_basic   来源页特例：仅 duration 0.5，无控件动作
└── fade_out  切出动画
    └── default           通用（无 from_page）
        ├── duration 0.2
        └── action
            ├── synchronization  backgroundPicture / logo → none
            └── schedule         四按钮 → smoothMove 向左 100、duration 0.1
```

每个 action 均含字段：`type`（none / smoothMove）、`delay`（相对前一个动作的延迟，秒）、`param`（smoothMove 含 orientation/speed/duration）。

### 已确认语义（用户拍板）

- **schedule = 按列表顺序串行执行**，每个动作的 `delay` = **相对前一个动画结束**的延迟（前一个播完 → 再等 delay → 启动下一个）。
- **能否播完取决于是否在总 `duration` 窗口内**：超出窗口的动作被截断。这就是 schedule 与" synchronization + 绝对 delay"的本质区别（后者可被同步组实现，单开 schedule 无意义）。
- **synchronization = 全部同时启动**（无顺序、无 delay 概念）。
- **smoothMove = 从控件原本位置出发**做相对位移；fade_in 是切入本页面（fade_out 对称 = 切出）。

### 当前配置的串行推演

| 方向 | 时间线 | 结果 |
|------|--------|------|
| fade_in（窗口 0.2） | start 0→0.05 → create 0.05→0.1 → config 0.1→0.15 → quit 0.15→0.2 | 四按钮全部播完，刚好收尾 |
| fade_out（窗口 0.2） | start 0→0.1 → create 0.1→0.2 → config 0.2（启动即到边界） → quit 超窗不启动 | 仅前两个按钮实际滑出 |

若 fade_out 想让四按钮都滑出，需把总 `duration` 调大到 0.4，或把按钮时长压到 0.05。

### 待定点

- **`speed` 语义**：若按"像素/秒"，fade_in 位移 = 100×0.05 = 5px、fade_out = 100×0.1 = 10px，视觉近乎不动。若 `speed` 实为"总位移距离"（复用原 distance 语义）则 100px 才合理。字段叫 speed 但数值像距离，待用户定。
- **方向**：fade_in / fade_out 的 orientation 都是 `x:-1`（向左）。切入本页面时按钮也向左移是有意设计，还是 fade_in 应为 `x:+1` 让按钮归位？待确认。

### 与设计文档的分层关系

2026-08-07 方案的**页面级**配置（`immediatelyOut/In` + `outDuration/inDuration`）管"播不播/强制立即"，本配置的**控件级** `animation` 管"播什么"。若仍要保留总开关能力，页面级字段可能还需补上（未定）。

## 二、UiKind 重构（已完成）

### 改动清单

| 文件 | 改动 |
|------|------|
| `type/ui/UiKind.java` | 新建。枚举 BUTTON/LABEL/IMAGE/FONT/MESSAGE_BOX，displayString 绑定 `UiKey.Button.KEY` 等（字符串单一来源 UiKey），带 `getDisplayString()` / `fromString()` |
| `ui/info/UiObject.java` | 填充数据类：`UiKind` + `String tag`（final 字段 + 构造器 + getter + toString） |
| `ui/UiManager.java` | 新增 `findObject(UiObject)`：switch UiKind 分发到 getButton/getLabel/getImage，不支持类型返回 null；`loadUiConfig` 的 `switch(category)` 迁移为 `UiKind.fromString` + `switch(kind)` |
| `input/VirtualInputHandler.java` | `setPriorityConfirmSelectObject` 的 `switch(type)` 迁移为 `UiKind.fromString` → `new UiObject(kind, tag)` → `uiManager.findObject(...)`；删除 UiKey import |

### 设计备注

- enum switch 的 case 必须写未限定名（`case BUTTON:`），不能写 `case UiKind.BUTTON:`——这是 Java 语法；选择器已是 UiKind 类型，编译器据此归属。
- `UiManager` 里 `UiKey.Font.NAME/PATH/SCALE`（配置字段名）保留，不属于 UI 类型枚举。

### 待办

- **LayoutManager** 中 `UiKey.Image/Label/Button.KEY` 仍作为 layout JSON 的 section key 取值（`getJsonEntityByKey`），属"键名取值"非类型分发，是否迁移 UiKind 未定。
- 本批改动未提交：需按提交规范随对应 CHANGELOG 条目一并提交。
