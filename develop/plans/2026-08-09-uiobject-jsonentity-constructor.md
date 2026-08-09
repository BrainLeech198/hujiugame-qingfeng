# UiObject 支持 JsonEntity 构造 + 复用点替换 — 设计

> **状态:** 2026-08-09 设计定案，已获用户批准。小改动：4 文件、纯重构无行为变化。

---

## 目标

给 `UiObject` 增加 `JsonEntity` 构造函数，把"从 JSON 配置解析 UI 类型+tag 并构造 UiObject"的逻辑下沉到数据类，替换 `VirtualInputHandler` 中的手工解析冗余代码。

## 设计决策

| 方面 | 决策 |
|------|------|
| type/tag 键唯一来源 | 收进 `UiKey.UiObject`（新增嵌套类，值 `"type"`/`"tag"`），`RequirementKey.Config` 现有常量转发引用 `UiKey.UiObject.TYPE/TAG` |
| 失败处理 | 构造函数只解析：type→`UiKind.fromString`（非法得 null）、tag→`getString`（缺失得 null）；字段存 null，由调用方校验 |
| 复用范围 | 仅 `VirtualInputHandler.setPriorityConfirmSelectObject(JsonEntity)` 一处（项目唯一从 JSON 解析 UiObject 的代码点） |

## 改动清单

| 文件 | 改动 |
|------|------|
| `type/key/UiKey.java` | 新增 `UiObject` 嵌套类（`TYPE`/`TAG` 字符串常量，字符串唯一来源） |
| `type/key/RequirementKey.java` | `Config.UNIVERSAL_PRIORITY_CONFIRM_UI_TYPE/TAG` 转发引用 `UiKey.UiObject.TYPE/TAG` |
| `ui/info/UiObject.java` | 新增 `UiObject(JsonEntity)` 构造函数，读 type→UiKind、tag→String |
| `input/VirtualInputHandler.java` | `setPriorityConfirmSelectObject` 改为 `new UiObject(priorityConfig)` + 前置校验（kind==null / tag 空或缺失），删除手工解析与 UiKind import |

## 复用点扫描结论

| 位置 | 是否适用 | 原因 |
|------|---------|------|
| VirtualInputHandler.setPriorityConfirmSelectObject | ✅ | 手工读 type/tag + 解析 + 构造 UiObject |
| UiManager.loadUiConfig 的 switch(category) | ❌ | 遍历配置清单样式名，不构造 UiObject |
| LayoutManager layout ui 节点读取 | ❌ | 结构为 kind 字段 + 元素名做 tag，键名与 type/tag 不同 |

## 注意事项

- 行为不变：原「tag 空」「type 非法」两类报错合并为前置校验，语义等价
- 校验需兼容 null：tag 缺失时 `getString` 返回 null，不能沿用 `isEmpty()`，需用 `getTag() == null || getTag().isEmpty()`
- 空 JsonEntity 读键安全返回 null（`getString` 对缺失 key 返回 null），调用方保证传入非 null
