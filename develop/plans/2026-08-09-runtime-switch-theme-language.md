# 运行时切换主题/语言 — 完整实现蓝图

> **状态:** 2026-08-09 深挖完整逻辑，**本轮不动代码**。本文档是把"游戏运行中切换语言/主题"从现象分析推进到**具体到方法与改动点**的实现蓝图，供后续按阶段落地。
>
> 背景：`ThemeManager.reload` / `LanguageManager.reload` / `MessageBox.reload` 只是包了一层 `init()`，全项目无调用方。本文档先确认每一条链路的现有机制，再给出各改动点的精确位置与理由。

---

## 一、现状结论（reload 为什么没用）

| 侧 | reload 做了什么 | 为什么对 UI 无效 |
|----|----------------|-----------------|
| 语言 | `reload → init → parseLanguagePath + parseJson + update()` | `blockMap`（final LRU 字段，LanguageManager.java:49）在 init 里**不清空**，`getText` 命中已加载块返回旧语言文本（:478-491） |
| 主题 | `reload → init → parseThemePath + parseJson + 字体/颜色` | UiManager 的字体/图片/样式在启动时 `init(ThemeManager)` 一次性加载（UiManager.java:647-714），reload 不触发重新 init，已创建控件持旧资源 |
| 调用方 | 全项目无 `.reload()` 调用 | 没有任何接线，连触发都没有 |

注：reload 几乎总能返回 true —— 主题/语言名无法解析时会走"修复用户配置 + 回退默认"分支，仍是 true，但不代表切到了目标。

---

## 二、语言切换：链路已就位，只差一个断点

### 已确认的自动刷新链路（无需改动）

1. **Label/Button 每帧刷新文本** — `act(delta)` → `updateText()` → `textObject.getDisplayText()`（UiManager.java:5194-5198 Label、:5521-5525 Button）
2. **TextObject 状态码检测** — `getDisplayText()` 每次调用比较 `textManagerStateCode != textManager.getStateCode()`，变化即重新 `parseRawText()`（TextObject.java:157-182）
3. **TextManager 轮询** — `getStateCode()` 轮询 `languageManager.getStateCode()`，变化则递增自身状态码（TextManager.java:145-162）
4. **LanguageManager 状态递增** — `init` 末尾调 `update()`（LanguageManager.java:327），reload 时必然触发
5. **块懒加载用当前 pathHandle** — `getText` 命中未加载块时 `loadBlock` 从 `pathHandle.child(block)` 读取（LanguageManager.java:406-464），reload 后 pathHandle 已指向新语言

结论：语言切换**不需要重建页面**。把断点补上，Label/Button 每帧轮询就会自动拿到新语言文本。

### 断点与修法

**断点**：`blockMap` 不清空 → `getText` 命中已加载块直接返回旧语言文本（:478-491）。

**修法**：在 `LanguageManager.init` 的 `parseLanguagePath` 之后、`update()` 之前加 `blockMap.clear()`（LanguageManager.java:301-327）。理由：

- reload 直接走 init，**无需单独接线**，一处改动全链路生效；
- 必须先 clear 再 `update()`：`update()` 递增状态码让所有 TextObject 感知变化，下一帧 `getDisplayText` 重新解析时 `getText` 已从新 `pathHandle` 读新语言块；
- `blockMap` 存的是 `Map<String,String>` 扁平文本，clear 无资源释放问题，LRU 淘汰逻辑（:49）不受影响。

### 触发入口（待接线，属阶段 3）

切换语言 = 写用户配置 + reload：
- 启动器：`userConfigManager.setLanguage(新语言)`（UserConfigManager.java:468）+ `languageManager.reload(新语言名, 语言集目录, isLauncherLanguage, userConfigManager)`
- 游戏内：`gameUserConfigManager.setLanguage(新语言)`（GameUserConfigManager.java:246）+ 游戏内 `languageManager.reload(...)`（isLauncherLanguage=false）
- reload 的 `directoryPathHandle`（语言集目录）需调用方持有或从现有配置链获取，属入口接线细节

---

## 三、主题切换：需重建 UI 层，重建链路现成

### 本轮深挖确认的三条关键事实

**事实 1：`UiManager.dispose()` 是终端操作，不能 dispose 后 re-init。**
dispose（UiManager.java:4627）调 `disposeExecutor.shutdown()`（:4671）+ `awaitTermination`（:4674），执行器一次性，之后 `scheduleDisposeTexture` 等异步销毁全部失效。主题切换**必须新增独立的样式重载路径**，不能走 dispose。

**事实 2：`UiManager.init()` 重复调用会泄漏，不能直接复用 init 做 reload。**
- `loadFont` 用相同 fontTag `put` 新 `CustomFont`，旧 `BitmapFont` 未 dispose（UiManager.java:802）——重复 init 字体泄漏；
- `packPendingPixmaps` 创建新 `atlasTexture`，旧大纹理未 dispose（:614）；
- `packPendingPixmaps` 在 `pendingPixmapMap` 为空时提前 return（:580-583）——新主题若无待合并小图，旧 `regionMap`/Kind 保留，**样式错误**；
- `loadImageKindFromTheme`/`loadLabelKindFromTheme`/`loadButtonKindFromTheme` 按名 put 覆盖 kind（:308-430），新主题若有缺失的样式名，旧 kind 残留。

**事实 3：场景重进链路现成可用，控件每次全量重建。**
- `setGameState(同状态)` → `updateGameState`（SceneStack.java:89）→ `loadGameLayout` 用 `themeManager.getPathHandle()` 拼 layout 路径（:327-340）→ 主题 pathHandle 变了 → `layoutManager.loadLayout` 缓存 key（`path@root`，LayoutManager.java:588）miss → 重新加载新主题 layout；
- `updateGameRender`（SceneStack.java:497）→ `renderPipeline.clear()` dispose 旧渲染机 → `renderPipeline.update()` 从 registry **工厂新建**渲染机（RenderPipeline.java:50-82、GameRenderRegistry.java:54 `factory.get()`）；
- 渲染机 `dispose` → `uiManager.deleteLayout(layoutConfig)`，`init` → `uiManager.addLayout(layoutConfig)`（MenuMain.java:78、:185），控件全部从 UiManager 的 kind map 创建；
- 因此：**UiManager 样式更新后，场景重进即用新样式重建全部控件**，无需改任何页面渲染机。

### 主题切换四步（改动点精确清单）

**Step 1 — `themeManager.reload(新主题名, 主题集目录, isLauncherTheme, userConfigManager)`**（现有方法，无需改）
更新 `pathHandle` / `font` / `fontUseSize` / `colorConfig`。

**Step 2 — 新增 `UiManager.reloadStyles(ThemeManager)`**（核心改动点，新方法）

顺序与 init 加载序列一致，但前置清理旧资源：

1. `deleteAllObject()`（UiManager.java:4261）清掉当前 stage 控件；
2. dispose `fontMap` 全部 `CustomFont` 并 clear（参考 dispose :4633-4638）；
3. dispose 旧 `atlasTexture` 并置 null（参考 dispose :4640-4645），`regionMap.clear()`；
4. 清理 `imageKindMap` / `labelKindMap` / `buttonKindMap`（注意：独立加载的图片纹理需逐个 dispose，见下方待验证点）；
5. clear `pendingPixmapMap` / `pendingLabelStyles` / `pendingButtonStyles` / `pendingButtonAudios` / `pendingLabelBorderScales` / `pendingButtonBorderScales`（:109-115）；
6. 重新执行 init 的加载序列：`loadUiConfig` → `loadFontFromTheme` → `loadImageKindFromTheme` → `loadLabelKindFromTheme` → `loadButtonKindFromTheme` → `packPendingPixmaps` → `messageBox.init(audioManager, this, themeManager)`（对照 init :647-711）；
7. **不碰 `disposeExecutor`**（保持存活，与 dispose 的核心区别）。

> 备选（更省代码）：把上面的清理挪到 `init` 开头，让 init 幂等，reload = 直接调 init。init 全项目仅启动时调用一次（Init.java:214），改造风险可控。二选一，实现时定。

**Step 3 — 触发场景重进：`sceneStack.setGameState(当前 state)`**（复用现成链路）
SceneStack 用新 `pathHandle` → layout 缓存 miss → 新 layout → 新渲染机 → 新控件。

**Step 4 — 顺序约束（重要）**
1. `themeManager.reload` 先（更新 pathHandle）→ 2. `uiManager.reloadStyles`（用新 pathHandle 读样式）→ 3. 场景重进（新控件用新样式）。顺序不可颠倒。

### 游戏内主题差异

- SceneStack 游戏内走 `playLocalData.getThemeManager()`（SceneStack.java:299），reload 的 manager 对象**必须与 SceneStack 用的是同一个**；
- 游戏内切主题同理：reload 那个 manager + `uiManager.reloadStyles` + 重进 GAME 状态。

---

## 四、分阶段实现建议

| 阶段 | 内容 | 成本 | 效果 |
|------|------|------|------|
| 1 | 语言切换：`LanguageManager.init` 加 `blockMap.clear()` | 低（1 行） | 切语言文本每帧自动刷新，无需重建页面 |
| 2 | 主题切换：新增 `UiManager.reloadStyles` + 场景重进 | 中 | 切主题需重建 UI 层，生效有页面重建成本 |
| 3 | 接入具体入口（config_basic 设置页 / 调试热重载工具） | 中 | 玩家/开发者可操作 |

---

## 五、风险与待验证

- **ImageKind 纹理归属**：`imageKindMap` 里既可能有 atlasTexture 上的 region，也可能有 `loadImageKind(String, FileHandle)` 直接加载的独立纹理（UiManager.java:881-884），清理时需区分处置，避免重复 dispose。
- **MessageBox 重复 init**：`messageBox.init`（UiManager.java:706）重复调用是否泄漏弹窗 Table 控件，需验证或并入 Step 2 的清理。
- **`deleteLayout`/`addLayout` 与 `layoutGroupMap`**：确认重进场景时 layoutGroupMap 的 add/remove 配对，避免 Group 残留。
- **LayoutManager 缓存累积**：`layoutConfigMap`（LayoutManager.java:29）随每次主题切换新增缓存条目，旧 layout 持旧图片/音乐引用，内存随切换次数增长；切回原主题命中旧缓存行为正确。长期需考虑缓存上限或切换时清理。
- **非 act 驱动控件的语言刷新**：TextObject 自动刷新依赖每帧 `act` 调用，需确认 MessageBox、图片标签等非 act 控件上的文本也走每帧刷新，否则个别控件不刷新。
