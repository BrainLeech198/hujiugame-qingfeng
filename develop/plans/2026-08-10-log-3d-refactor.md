# 日志"三维"改造 + Script throw 消息统一 + GraphicsObject tag 优化 — 设计方案

> **状态:** 方案设计已确认（2026-08-10），**代码暂不实施**，待用户明确指示后按本方案分阶段执行。
>
> 背景：现有日志难追踪（tag 无调用位置、异常堆栈压扁、跨端格式不一致、上下文靠手拼）；Script 体系 90 处 throw 消息为英文且无溯源；`GraphicsObject` 唯一元素无 tag 时 `buildJson` 会写出 `"tag": null`。

---

## 背景与目标

**问题一：日志难追踪。** `LogUtils` 现在：

- tag 只有类简单名 `clazz.getSimpleName()`，无包名/方法/行号，同名类多，无法定位"哪一行打的"。
- 异常堆栈 `message + "\n" + stackTrace` 整坨拼进 message，被 GDX 按行拆散，Logcat 截断，异常类型与根因淹没。
- 时间戳/级别塞进 GDX 的 tag 参数，Android 上整个变 Logcat 的 TAG，跨端日志长两样。
- 异常重载只收 `Exception` 不收 `Throwable`（OOM 等 Error 无法带栈）。
- 上下文全靠手拼，格式不统一；无耗时/时序、无调用链。

目标（用户明确）：四个维度 + 三友好——**空间维**（自动调用点）、**异常升级**（Throwable + 根因链）、**上下文维**（结构化重载）、**时间维**（耗时统计）；**不耗性能**（被过滤日志零开销）、**对写日志的地方友好**（现有 1732 处调用不改）、**对调试看日志友好**（格式可读、定位精确）。

**问题二：Script throw 消息不统一。** Script 体系 90 处 throw（44 文件）是英文且无溯源，需对齐 `AnimationObject.fromJson` 的"中文 + 类名溯源 + 带 json"风格。

**问题三：GraphicsObject tag 可空。** `background_picture` 等唯一 graphics 元素无 tag，`buildJson` 会写 `"tag": null`，优化为 tag 非空才写。

已探明的事实：

- `LogUtils` 全项目 1732 处调用，首参几乎全 `Class<?>`；带 Exception 三参约 506 处；无日志文件解析/无 UI 消费，改格式安全；无每帧无条件日志。
- `JsonEntity.getJsonString()` 内部 fastjson、失败返回 null，安全。
- `LogLevel.displayString` 仅 LogUtils 消费，INFO 有尾随空格。
- Script 90 处 throw 分布：buildJson 路径 `IllegalStateException("An invalid command parameter cannot be built.")` 35 处、缺字段 28 处、"must be a map" 15 处、动作类类型不匹配 8 处、`TypeMapper` 3 处、`ArgumentInfo` 5 处。

## 方案一 — LogUtils "三维"日志改造

### 核心设计

- **零开销过滤**：所有公开重载入口先 `if (level > logLevel && level > fileLogLevel) return;`。被过滤日志不提栈、不取时间、不格式化。
- **GDX tag 只放稳定短 tag**（Class 重载 = `clazz.getSimpleName()`）；时间戳/级别/耗时/调用点/上下文/异常全进 message，修复"整串时间戳塞 tag"。
- **惰性格式化**：公开重载只把 Throwable/JsonEntity/FileHandle 原样传给私有漏斗 `consolePrintLog(level, tag, message, throwable, json, file)`，漏斗内先判等级、需要输出才格式化。

### 公开重载（18 个）

- 2 参保留 6 个：`debug/info/error(String,String)` 与 `(Class<?>,String)`。
- 3 参 Throwable 6 个：`(String,String,Throwable)` 与 `(Class<?>,String,Throwable)` —— 原 `Exception` 参数改 `Throwable`，兼容全部 506 处调用。
- 3 参上下文 6 个（仅 Class 版）：`(Class<?>,String,JsonEntity)`、`(Class<?>,String,FileHandle)`。
- 无重载歧义：Throwable/JsonEntity/FileHandle 两两无关，非 null 实参唯一命中。

### 输出格式模板

- **控制台**（单行）：
  `[2026-08-10 12:00:00.123] [+3ms] [INFO ] [Foo.bar(Foo.java:123)] 消息 (json): {...} (file): /path`
  级别定宽 5 字符 `[DEBUG]/[INFO ]/[ERROR]`（绕开 LogLevel 尾随空格）。
- **文件**（多行）：头部同控制台；异常块分层——每个异常 `(exception): 类型 | 消息` + 抛出点首帧，末尾 `(root stack):` 用根因 `printStackTrace` 完整栈。
- 上下文 null 时输出 `(json): null` / `(file): null`。

### 调用点提取（空间维）

`getCallerElement()`：`Thread.currentThread().getStackTrace()`，扫描跳过 LogUtils 自身机器帧（静态 Set 收集 `debug/info/error/consolePrintLog/getCallerElement/format*/outputConsole/writeLogFile` 等），第一个非 LogUtils 帧即真实调用者；LogUtils 自身业务方法（如 `init()`）打日志时该帧即调用点。只取单帧、不拼整栈。

### 耗时统计（时间维）

`AtomicLong LAST_LOG_NANO_TIME` + `getAndSet` 无锁，`System.nanoTime()` 差转毫秒，负值兜底为 0。

### 异常格式化（异常升级）

- `getStackTraceAsString(Exception)` → 改 `(Throwable)`（供文件根因栈）。
- 新增 `formatThrowableSummary`（单行紧凑：类型 + 消息 + caused by 链每层一行）/ `formatThrowableDetail`（结构化链 + 根因完整栈）。

### 现有调用点

**1732 处不需要改**。可选清理：5 处 `(Exception)e` 强转（AudioManager:101、Main:723/794、GraphicsManager:58、UiManager:123）、`Main.java:167-169` 手工 gdxString 迁移为 `LogUtils.info(Main.class, ...)`。

### 涉及文件

- `core/src/main/java/com/hujiugame/qingfeng/util/system/LogUtils.java`（主改造）
- `core/src/main/java/com/hujiugame/qingfeng/util/system/CrashUtils.java`（:105 调用兼容，:64 getNowLogFileHandle 不变）
- `android/proguard-rules.pro`（可选：`-keepattributes SourceFile,LineNumberTable` 保 R8 行号，Android release 已开 minify）

## 方案二 — Script 体系 throw 消息统一（90 处 / 44 文件）

### 消息格式约定（对齐 AnimationObject 样例 `<Source> <中文描述> (key): "..." (json): <json>`）

- 构造器溯源：`ClassName(JsonEntity)`；buildJson 无 json → `ClassName.buildJson`，用 `ClassName.class.getName()` 溯源。
- **硬编码类名字面量**（非 `getClass().getSimpleName()`）：与 AnimationObject 一致、R8 不混淆字符串字面量、静态方法里可用。
- 模板：
  - 缺字段：`"IfControlScriptCommandParam(JsonEntity) 缺少字段 (key): \"CONDITION\" (json): " + json`
  - 非 Map：`"IfControlScriptCommandParam(JsonEntity) 需要 Map 数据 (json): " + json`
  - buildJson：`"IfControlScriptCommandParam.buildJson 内部状态无效，无法构造 JSON (class): " + IfControlScriptCommandParam.class.getName()`
  - 动作类：`"VariableScriptCommand 构造器参数类型与动作不匹配 (class): " + param.getClass().getName() + " (action): " + action`
  - TypeMapper：`"TypeMapper.parseClass 不支持的类型 (type): " + typeString`

### 实施顺序（由小到大闭环）

1. `TypeMapper.java`（3 处，静态无 json）→ 2. 动作类 8 处（手改）→ 3. `ArgumentInfo.java`（5 处）→ 4. buildJson IllegalStateException 35 处（整行替换，类名按文件基名派生）→ 5. "must be a map" 15 处 → 6. 缺字段 28 处。后三类单行统一模式，可用正则按文件基名注入类名，**替换后逐文件复核**。

### 陷阱

- 只改文案前缀，`\"" + ScriptKey... + "\"` 常量拼接原样保留。
- buildJson 路径无 json 参数，勿引用 `json`（该处 json 为 null），用 `(class)` 溯源。
- 价值参数类（AddMathValueCommandParam 等）JsonEntity 构造器不校验字段、只有 buildJson 一处 throw，归入步骤 4，勿加多余缺字段消息。
- 中文风格对齐项目注释："缺少字段 / 需要 Map 数据 / 不支持的类型 / 内部状态无效，无法构造 JSON"。
- 影响脚本错误提示约定时留意 `develop/SCRIPT_INTERNAL_STANDARD.md`。

### 涉及目录

- `core/src/main/java/com/hujiugame/qingfeng/script/data/`（`*ScriptCommandParam`、`ArgumentInfo`、`TypeMapper`、action 类）

## 方案三 — GraphicsObject tag 可空优化（小）

- `GraphicsAnimationObject.buildJson`：仅当 `target.getTag() != null` 才写 `UiKey.UiObject.TAG`，让 background_picture 等唯一元素的 object 节点不出现 `"tag": null`。
- 已确认真伪安全（构造/fromString/valid/buildJson 全链不崩：`getString` 缺失返回 null、`fromString(null)` 判空、`deepCopyValue(null)` 原样返回且 HashMap 允许 null value），此优化只解决输出干净度。
- 定位语义提醒：`GraphicsObject(kind=BACKGROUND_PICTURE, tag=null)` 代表"该类型唯一元素"，未来 `findGraphicsObject` 需支持 tag 为空按类型匹配。
- 涉及文件：`core/src/main/java/com/hujiugame/qingfeng/animation/task/object/GraphicsAnimationObject.java`

## 验证

1. `./gradlew :core:compileJava`（改 LogUtils/脚本 throw/GraphicsObject 后各跑一次）。
2. lwjgl3 跑通：观察 `init` 自打日志的调用点格式、异常链输出、`(json):`/`(file):` 上下文、`[+Nms]` 耗时；改 log 配置等级验证被过滤日志无输出。
3. Script：喂缺字段 JSON 断言新中文消息；grep 无残留英文（`Command parameter must have` / `An invalid command parameter cannot be built` / `Argument info must have` / `must be a map` / `Unsupported type` / `does not match`）；throw 总数仍 90。
4. Android 若跑：确认 R8 下行号/调用点不退化（proguard keep 后）。

## 提交策略（按项目规范逐笔拆分）

1. **LogUtils 三维改造**：LogUtils.java + CrashUtils.java（如改）+ proguard + CHANGELOG 对应条目。
2. **Script throw 消息统一**：script/data/ 全部 + CHANGELOG 对应条目。
3. **GraphicsObject tag 优化**：GraphicsAnimationObject.java + CHANGELOG 对应条目。
4. 每笔独立提交，temp/CLAUDE_MEMORY.md 追加设计决策记录（附提交 hash）。
