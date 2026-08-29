# SceneStack 预加载改造方案

> **状态：** 方案已确认（2026-08-23），待实施。
>
> **背景：** 页面切换时 `SceneStack.updateGameState()` 在 GL 线程同步执行文件 IO + JSON 解析 + 资源加载 + 渲染机创建，造成 ~50-100ms 卡顿。本方案将其中的文件 IO + JSON 解析拆到后台线程预加载，为动画系统的淡出期间预加载提供基础设施。
>
> **前置依赖：** 无。本方案独立于动画系统，改造后现有流程不受影响，将来动画系统接入时直接调用新接口。
>
> **关联文档：** `2026-08-07-page-transition-animation.md`（过渡动画总体方案）

---

## 1. 当前流程

```
EventDispatcher.handleEvent(PUSH_GAME_STATE)
  → setPendingTask() + setState(TRANSITION) + startTransition()
  → executePendingTask() 入队 PUSH_GAME_STATE_EXECUTE

EventDispatcher.handleEvent(PUSH_GAME_STATE_EXECUTE)
  → sceneStack.pushGameState(inState)
    → stateStack.push(inState)
    → updateGameState()                    ← 全部同步阻塞
      → loadGameLayout()                   ← 文件IO + JSON解析 + 音乐/图形加载
      → loadGameConfig()                   ← 文件IO + JSON解析
      → updateGameRender()                 ← dispose旧 + 创建新渲染机
```

`updateGameState()` 内部 `loadGameLayout()` 做了 6 件事：

| 步骤 | 操作 | 耗时 | 线程安全 |
|------|------|------|---------|
| 1 | `getCurrentState()` 确定目标状态 | <1ms | — |
| 2 | 推算 layout 文件路径 | <1ms | 纯字符串 |
| 3 | `readLayoutJson()` 读文件 + JSON 解析 | 1-3ms | 纯 Java |
| 4 | `loadLayoutMusic()` Gdx.audio.newMusic() | 5-20ms | 需 GL 线程 |
| 5 | `loadLayoutGraphics()` new Texture() | 10-50ms | 需 GL 线程 |
| 6 | `loadLayoutUi()` 解析 UI 定义 | <1ms | 纯 Java |

步骤 3 + 6 是纯 Java 操作，可以安全地在后台线程执行。步骤 4 + 5 涉及 libGDX native 资源，必须在 GL 线程。

---

## 2. 改造目标

将 `updateGameState()` 拆成两步：

| 方法 | 执行内容 | 线程 | 用途 |
|------|---------|------|------|
| `preloadGameState(targetState)` | 步骤 1-3 + config IO | 后台线程 | 淡出期间预加载 |
| `applyPreloadedState(preloaded)` | 步骤 4-6 + 渲染机创建 | GL 线程 | 淡出完成后应用 |

现有 `updateGameState()` **保留不动**，作为同步 fallback。

---

## 3. 新增类

### 3.1 PreloadedData（数据传输类）

```java
package com.hujiugame.qingfeng.core;

/**
 * 预加载数据传输对象。
 * <p>
 * 由 preloadGameState() 在后台线程创建，传递给 applyPreloadedState() 在 GL 线程消费。
 * 纯数据类，不持有 libGDX native 资源，线程安全。
 */
public final class PreloadedData
{
    private final GameState gameState;
    private final JsonEntity layoutJson;
    private final JsonEntity configJson;
    private final FileHandle layoutFile;
    private final FileHandle resourceRoot;
    private final boolean valid;
}
```

### 3.2 GameStatePreloader（静态工具类）

```java
package com.hujiugame.qingfeng.core;

/**
 * 游戏状态预加载器。
 * <p>
 * 在后台线程读取目标页面的 layout.json 和 config.json 并解析为 Java 对象。
 * 仅做文件 IO + JSON 解析，不访问 libGDX 资源（纹理/音频/Stage），
 * 不修改任何实例状态，线程安全。
 */
public final class GameStatePreloader
{
    /**
     * 预加载目标状态的布局和配置 JSON。
     *
     * @param gameState    目标游戏状态
     * @param themeManager 主题管理器（确定文件路径）
     * @return 预加载结果，失败返回 valid=false 的 PreloadedData
     */
    public static PreloadedData preload (GameState gameState, ThemeManager themeManager)
    {
        String layoutDirName = gameState.getLayoutDirName();

        // 无需布局的页面（GAME_PLAY 等）
        if (layoutDirName == null)
        {
            return new PreloadedData(gameState, new Layout(), new JsonEntity(), null, null, true);
        }

        // 推算文件路径（纯字符串拼接，线程安全）
        FileHandle layoutFile;
        FileHandle resourceRoot;
        if (gameState.isInGame())
        {
            layoutFile = themeManager.getPathHandle()
                .child(PathName.IN_GAME_ASSET_S_PAGE)
                .child(layoutDirName)
                .child(FileName.IN_GAME_PAGE_LAYOUT);
            resourceRoot = themeManager.getPathHandle();
        }
        else
        {
            layoutFile = themeManager.getPathHandle()
                .child(PathName.ASSET_S_PAGE)
                .child(layoutDirName)
                .child(FileName.PAGE_LAYOUT);
            resourceRoot = themeManager.getPathHandle();
        }

        // 读 layout.json（纯文件 IO + JSON 解析）
        JsonEntity layoutJson = readLayoutJson(layoutFile);
        if (layoutJson == null)
        {
            return new PreloadedData(gameState, null, null, null, null, false);
        }

        // 读 config.json（可选）
        JsonEntity configJson = new JsonEntity();
        if (gameState.isNeedConfig())
        {
            FileHandle configFile =推算配置文件路径;
            if (FileUtils.isFileExist(configFile))
            {
                configJson = new JsonEntity(configFile);
            }
        }

        return new PreloadedData(gameState, layoutJson, configJson, layoutFile, resourceRoot, true);
    }
}
```

---

## 4. SceneStack 改造

### 4.1 新增方法

```java
/**
 * 预加载目标状态的布局和配置 JSON。
 * <p>
 * 仅做文件 IO + JSON 解析，不访问 libGDX 资源，不修改实例状态，
 * 可在后台线程安全调用。
 *
 * @param targetState 目标游戏状态
 * @return 预加载结果，失败返回 null
 */
public PreloadedData preloadGameState (GameState targetState)
{
    try
    {
        ThemeManager usedThemeManager = targetState.isInGame()
            ? playLocalData.getThemeManager() : themeManager;
        return GameStatePreloader.preload(targetState, usedThemeManager);
    }
    catch (Exception e)
    {
        LogUtils.error(SceneStack.class, "preloadGameState 预加载失败", e);
        return null;
    }
}

/**
 * 应用预加载数据，创建新渲染机。
 * <p>
 * 必须在 GL 线程调用。使用预加载的 JSON 跳过文件 IO，
 * 仍需通过 LayoutManager 加载音乐/图形资源和创建渲染机。
 *
 * @param preloaded 预加载结果
 * @return 是否成功
 */
public boolean applyPreloadedState (PreloadedData preloaded)
{
    try
    {
        if (preloaded == null || !preloaded.isValid())
        {
            LogUtils.error(SceneStack.class, "applyPreloadedState 预加载数据无效");
            return false;
        }

        GameState targetState = preloaded.getGameState();
        boolean isInGame = targetState.isInGame();
        boolean isFirstInGame = isInGame && !isInGame();
        setInGame(isInGame);

        ThemeManager usedThemeManager = isInGame
            ? playLocalData.getThemeManager() : themeManager;

        // 用预加载的 JsonEntity 调 LayoutManager（跳过 readLayoutJson）
        Layout layout = layoutManager.loadLayout(
            preloaded.getLayoutJson(),
            preloaded.getLayoutFile(),
            preloaded.getResourceRoot(),
            isFirstInGame
        );
        if (layout == null)
        {
            LogUtils.error(SceneStack.class, "applyPreloadedState 加载布局失败");
            return false;
        }

        // 创建渲染机（复用现有逻辑）
        if (!updateGameRender(layout, preloaded.getConfigJson()))
        {
            LogUtils.error(SceneStack.class, "applyPreloadedState 创建渲染机失败");
            return false;
        }

        LogUtils.debug(SceneStack.class, "applyPreloadedState 应用预加载数据成功"
            + " (state): " + targetState);
        return true;
    }
    catch (Exception e)
    {
        LogUtils.error(SceneStack.class, "applyPreloadedState", e);
        return false;
    }
}
```

### 4.2 现有方法保留

`updateGameState()`、`pushGameState()`、`popGameState()`、`setGameState()`、`resetGameState()` 全部不动。

---

## 5. LayoutManager 改造

新增重载，接受已解析的 JsonEntity，跳过文件读取：

```java
/**
 * 加载布局（接受已解析的 JsonEntity，跳过文件读取）。
 * <p>
 * 由 SceneStack.applyPreloadedState() 调用，复用预加载阶段解析好的 JSON。
 * 内部逻辑与原 loadLayout(FileHandle, ...) 一致，仅跳过 readLayoutJson() 调用。
 *
 * @param layoutJson               已解析的 layout JSON（由预加载提供）
 * @param fileHandle               layout 文件句柄（缓存 key 用）
 * @param resourceRootDirectoryHandle 资源根目录（music/graphics 加载用）
 * @param reload                   是否强制重新加载
 * @return 布局配置对象，失败返回 null
 */
public Layout loadLayout (JsonEntity layoutJson, FileHandle fileHandle,
                          FileHandle resourceRootDirectoryHandle, boolean reload)
{
    // 逻辑与原方法一致，只是 layoutJson 由参数传入，不调 readLayoutJson()
}
```

原有 `loadLayout(FileHandle, FileHandle, boolean)` 保留不动。

---

## 6. 调用路径

### 6.1 直接跳转（现有，不动）

```
pushGameState(state)
  → stateStack.push(state)
  → updateGameState()           ← 同步一步到位
```

### 6.2 动画过渡（将来接入）

```
startTransition()
  → CompletableFuture.supplyAsync(() -> preloadGameState(inState))

淡出期间每帧：
  → 推进淡出 alpha
  → 检查 preloadFuture.isDone()

淡出完成 + 预加载完成：
  → executePendingTask() → 入队 *_EXECUTE

*_EXECUTE handler：
  PreloadedData preloaded = transitionManager.consumePreloadedData();
  if (preloaded != null)
      applyPreloadedState(preloaded);    ← 跳过 IO
  else
      pushGameState(state);              ← fallback
```

### 6.3 预加载失败 fallback

```
preloadGameState() 返回 null 或 valid=false
  → consumePreloadedData() 返回 null
  → 走 pushGameState() → updateGameState()  ← 同步兜底
```

---

## 7. Layout 缓存机制说明

LayoutManager 的 `layoutConfigMap` 缓存 Layout 对象，key = `fileHandle.path() + "@" + resourceRoot.path()`。

| 场景 | 缓存 | 行为 |
|------|------|------|
| 启动器→启动器（二次访问） | 命中 | 重载 music/graphics（可能已 dispose） |
| 游戏页→游戏页（二次访问） | 命中 | 同上 |
| 首次访问某页 | 未命中 | 全量加载 |
| 首次进入游戏 | 强制 reload | 全量加载（`isFirstInGame=true`） |

缓存机制与预加载不冲突：预加载跳过的是 `readLayoutJson()`（文件 IO），缓存跳过的是 `loadLayoutBasicInfo` + `loadLayoutUi`（JSON 解析）。两者叠加效果最好。

---

## 8. 线程安全分析

| 操作 | 线程 | 安全性 |
|------|------|--------|
| `FileHandle.readString()` | 后台 | 安全（纯文件 IO，不需 GL 上下文） |
| `new JsonEntity(String)` | 后台 | 安全（纯 Java JSON 解析） |
| `FileUtils.isFileExist()` | 后台 | 安全（文件系统查询） |
| `FileHandle` 对象创建 | GL | 创建后传给后台线程使用安全（不可变值对象） |
| `layoutConfigMap.get()` | GL | 不在预加载中调用（在 applyPreloadedState 中调用） |
| `loadLayoutMusic/Graphics` | GL | 只在 applyPreloadedState 中调用 |

---

## 9. 改动清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `PreloadedData.java` | **新增** | 数据传输类 |
| `GameStatePreloader.java` | **新增** | 静态预加载工具 |
| `SceneStack.java` | 修改 | 新增 `preloadGameState()` + `applyPreloadedState()` |
| `LayoutManager.java` | 修改 | 新增 `loadLayout(JsonEntity, FileHandle, FileHandle, boolean)` 重载 |

**不改的文件：** EventDispatcher、TransitionManager、RenderPipeline、GameHost、现有 updateGameState() 系列方法。

---

## 10. 将来接入动画系统时的改动点

仅两处：

**TransitionManager.startTransition()：**
```java
// 现在：空壳
public void startTransition() { setEnable(true); }

// 将来：
public void startTransition() {
    setEnable(true);
    phase = FADE_OUT;
    preloadFuture = CompletableFuture.supplyAsync(
        () -> sceneStack.preloadGameState(inState)
    );
}
```

**EventDispatcher EXECUTE handler：**
```java
// 现在：
sceneStack.pushGameState(pushEvent.getState());

// 将来：
PreloadedData preloaded = transitionManager.consumePreloadedData();
if (preloaded != null)
    sceneStack.applyPreloadedState(preloaded);
else
    sceneStack.pushGameState(pushEvent.getState());
```
