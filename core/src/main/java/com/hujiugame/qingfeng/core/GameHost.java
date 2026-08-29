package com.hujiugame.qingfeng.core;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.hujiugame.qingfeng.game.GameLogicService;
import com.hujiugame.qingfeng.game.GameSessionManager;
import com.hujiugame.qingfeng.game.loader.GamePlayDataLoader;
import com.hujiugame.qingfeng.game.loader.GameResourceLoader;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.data.play.PlayRuntimeData;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.engine.EngineContext;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.event.EventDispatcher;
import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.manager.*;
import com.hujiugame.qingfeng.game.GameInfoManager;
import com.hujiugame.qingfeng.util.system.LogUtils;

public final class GameHost
{
    private final UserConfigManager userConfigManager;
    private final ThemeManager themeManager;
    private final LanguageManager languageManager;
    private final EventQueue eventQueue;

    private final AudioManager audioManager;
    private final GraphicsManager graphicsManager;
    private final UiManager uiManager;

    private final LayoutService layoutManager;
    private final EngineContext launcherEngineContext;
    private final SpriteBatch spriteBatch;
    private final Stage stage;

    private final GameInfoManager gameInfoManager;
    private final PlayLocalData playLocalData;
    private final PlayRuntimeData playRuntimeData;
    private final RenderPipeline renderPipeline;
    private final SceneStack sceneStack;
    private final ConfigService configService;
    private final EventDispatcher eventDispatcher;
    private final GameLogicService gameLogicService;
    private final GameSessionManager sessionManager;

    public GameHost (UserConfigManager userConfigManager,
                     ThemeManager themeManager,
                     LanguageManager languageManager,
                     EventQueue eventQueue,
                     AudioManager audioManager,
                     GraphicsManager graphicsManager,
                     UiManager uiManager,
                     LayoutService layoutManager,
                     SpriteBatch spriteBatch,
                     Stage stage,
                     RenderPipeline renderPipeline,
                     SceneStack sceneStack,
                     ConfigService configService,
                     EventDispatcher eventDispatcher,
                     GameLogicService gameLogicService)
    {
        this.userConfigManager = userConfigManager;
        this.themeManager = themeManager;
        this.languageManager = languageManager;
        this.eventQueue = eventQueue;

        this.audioManager = audioManager;
        this.graphicsManager = graphicsManager;
        this.uiManager = uiManager;

        this.layoutManager = layoutManager;
        this.launcherEngineContext = new EngineContext(audioManager, graphicsManager, null);
        this.spriteBatch = spriteBatch;
        this.stage = stage;

        // 运行时数据
        this.gameInfoManager = new GameInfoManager();
        this.playLocalData = new PlayLocalData();
        this.playRuntimeData = new PlayRuntimeData();

        // 游戏服务
        this.renderPipeline = renderPipeline;
        this.sceneStack = sceneStack;
        this.configService = configService;
        this.eventDispatcher = eventDispatcher;
        this.gameLogicService = gameLogicService;
        this.sessionManager = new GameSessionManager(
            this.configService,
            new GameResourceLoader(
                this.userConfigManager,
                this.spriteBatch,
                this.stage,
                this.launcherEngineContext,
                this.layoutManager,
                this.eventQueue,
                this.playLocalData),
            new GamePlayDataLoader(
                this.layoutManager,
                this.playLocalData),
            this.eventQueue, this.gameLogicService, this.gameInfoManager,
            this.playLocalData, this.sceneStack);
    }

    // ===================================================================================================================
    // 框架生命周期
    // ===================================================================================================================

    /**
     * 初始化所有游戏服务（渲染器、状态服务、配置加载器、事件服务、游戏逻辑），
     * 完成后推入 INIT 状态
     *
     * @return 是否全部初始化成功
     */
    public boolean init ()
    {
        try
        {
            // 初始化渲染服务
            if (!renderPipeline.init(playLocalData))
            {
                LogUtils.error(GameHost.class, "init 渲染服务初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "init 渲染服务初始化成功");
            }

            // 初始化游戏状态服务
            if (!sceneStack.init(themeManager, languageManager, audioManager, graphicsManager, uiManager, layoutManager, playLocalData, renderPipeline, gameLogicService))
            {
                LogUtils.error(GameHost.class, "init 游戏状态服务初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "init 游戏状态服务初始化成功");
            }

            // 初始化配置服务
            if (!configService.init(userConfigManager, themeManager, languageManager))
            {
                LogUtils.error(GameHost.class, "init 配置服务初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "init 配置服务初始化成功");
            }

            // 初始化游戏事件服务
            if (!eventDispatcher.init(sceneStack))
            {
                LogUtils.error(GameHost.class, "init 游戏事件服务初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "init 游戏事件服务初始化成功");
            }

            // 初始化游戏逻辑
            if (!gameLogicService.init(playLocalData))
            {
                LogUtils.error(GameHost.class, "init 游戏逻辑初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "init 游戏逻辑初始化成功");
            }

            // 推入 INIT 状态开始渲染
            sceneStack.pushGameState(GameState.INIT);

            LogUtils.debug(GameHost.class, "init 成功启动游戏");
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameHost.class, "init", e);
            return false;
        }
    }

    /**
     * 主循环单帧调用：更新→处理事件→渲染
     * <p>
     * 性能：整条帧热路径的入口，每帧调用 1 次，其内部 updateFrame 逻辑更新 + 事件分发 + render 绘制的总耗时
     * 直接决定帧率下限。帧耗时问题应沿内部链路逐层定位，不要在 run 内新增额外逻辑；
     * 不适合承载加载、解析、纹理解码等一次性重活。
     *
     * @param deltaTime 距上一帧的时间差
     */
    public void run (float deltaTime)
    {
        try
        {
            // 1. 处理输入、更新逻辑
            renderPipeline.update(deltaTime);

            // 2. 处理事件（状态切换），本帧立即生效
            while (this.eventQueue.hasEvent())
            {
                LogUtils.debug(GameHost.class, "run 存在事件");

                Event event = this.eventQueue.getEvent();
                eventDispatcher.handleEvent(event);
            }

            // 3. 渲染当前状态
            renderPipeline.render(deltaTime);
        }
        catch (Exception e)
        {
            LogUtils.error(GameHost.class, "run", e);
            throw e;
        }
    }

    // ===================================================================================================================
    // Getters
    // ===================================================================================================================

    public GameInfoManager getGameInfoManager ()
    {
        return gameInfoManager;
    }

    public PlayLocalData getPlayLocalData ()
    {
        return playLocalData;
    }

    public PlayRuntimeData getPlayRuntimeData ()
    {
        return playRuntimeData;
    }

    public RenderPipeline getRenderPipeline ()
    {
        return renderPipeline;
    }

    public SceneStack getSceneStack ()
    {
        return sceneStack;
    }

    public ConfigService getConfigService ()
    {
        return configService;
    }

    public EventDispatcher getEventDispatcher ()
    {
        return eventDispatcher;
    }

    public GameLogicService getGameLogicService ()
    {
        return gameLogicService;
    }

    public GameSessionManager getGameSessionManager ()
    {
        return sessionManager;
    }

    // ===================================================================================================================
    // 销毁
    // ===================================================================================================================

    /**
     * 销毁所有游戏服务及其资源
     *
     * @return 是否全部销毁成功
     */
    public boolean dispose ()
    {
        try
        {
            // 销毁渲染服务
            if (!renderPipeline.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 渲染服务销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 渲染服务销毁成功");
            }

            // 销毁游戏状态服务
            if (!sceneStack.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 游戏状态服务销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 游戏状态服务销毁成功");
            }

            // 销毁配置服务
            if (!configService.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 配置服务销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 配置服务销毁成功");
            }

            // 销毁游戏事件服务
            if (!eventDispatcher.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 游戏事件服务销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 游戏事件服务销毁成功");
            }

            // 销毁游戏逻辑
            if (!gameLogicService.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 游戏逻辑服务销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 游戏逻辑服务销毁成功");
            }

            // 销毁数据
            if (!playLocalData.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 数据销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 数据销毁成功");
            }

            // 销毁运行时数据
            if (!playRuntimeData.dispose())
            {
                LogUtils.error(GameHost.class, "dispose 运行时数据销毁失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameHost.class, "dispose 运行时数据销毁成功");
            }

            LogUtils.debug(GameHost.class, "dispose 游戏资源销毁成功");
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameHost.class, "dispose", e);
            return false;
        }
    }

}
