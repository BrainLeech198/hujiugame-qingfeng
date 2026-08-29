package com.hujiugame.qingfeng.game.loader;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.animation.AnimationManager;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.engine.EngineContext;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.script.ScriptExecutor;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.manager.LayoutService;
import com.hujiugame.qingfeng.manager.LanguageManager;
import com.hujiugame.qingfeng.manager.ThemeManager;
import com.hujiugame.qingfeng.manager.UserConfigManager;
import com.hujiugame.qingfeng.util.system.LogUtils;

public final class GameResourceLoader
{
    private final UserConfigManager userConfigManager;
    private final SpriteBatch spriteBatch;
    private final Stage stage;
    private final EngineContext launcherEngineContext;
    private final LayoutService layoutManager;
    private final EventQueue eventQueue;
    private final PlayLocalData playLocalData;

    /**
     * 构造游戏资源加载器
     *
     * @param userConfigManager       用户配置管理器
     * @param spriteBatch             精灵批处理
     * @param stage                   舞台对象
     * @param launcherEngineContext   启动器引擎上下文
     * @param layoutManager           布局管理器
     * @param eventQueue              事件队列（创建游戏动画管理器用）
     * @param playLocalData         游戏数据内容
     */
    public GameResourceLoader (UserConfigManager userConfigManager,
                               SpriteBatch spriteBatch,
                               Stage stage,
                               EngineContext launcherEngineContext,
                               LayoutService layoutManager,
                               EventQueue eventQueue,
                               PlayLocalData playLocalData)
    {
        this.userConfigManager = userConfigManager;
        this.spriteBatch = spriteBatch;
        this.stage = stage;
        this.launcherEngineContext = launcherEngineContext;
        this.layoutManager = layoutManager;
        this.eventQueue = eventQueue;
        this.playLocalData = playLocalData;
    }

    /**
     * 加载游戏资源，包括音频、图形、UI和消息框
     *
     * @param gameThemeManager 游戏主题管理器
     * @return 加载是否成功
     */
    public boolean loadResource (ThemeManager gameThemeManager)
    {
        try
        {
            // audio
            launcherEngineContext.getAudioManager().stopAll();
            AudioManager gameAudioManager = new AudioManager(userConfigManager);
            if (!gameAudioManager.init())
            {
                LogUtils.error(GameResourceLoader.class, "loadResource 音频初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameResourceLoader.class, "loadResource 音频初始化成功");
            }
            layoutManager.setAudioManager(gameAudioManager);

            // graphics
            GraphicsManager gameGraphicsManager = new GraphicsManager(spriteBatch, gameThemeManager);
            if (!gameGraphicsManager.init())
            {
                LogUtils.error(GameResourceLoader.class, "loadResource 绘图初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameResourceLoader.class, "loadResource 绘图初始化成功");
            }
            layoutManager.setGraphicsManager(gameGraphicsManager);

            // ui（游戏 UiManager 使用启动器的 Audio/Graphics，因为 UI 元素在启动器 stage 上创建）
            LanguageManager gameLanguageManager = playLocalData.getLanguageManager();
            UiManager gameUiManager = new UiManager(stage, launcherEngineContext.getAudioManager(), launcherEngineContext.getGraphicsManager(), gameLanguageManager);
            if (!gameUiManager.init(gameThemeManager))
            {
                LogUtils.error(GameResourceLoader.class, "loadResource ui初始化失败");
                return false;
            }
            else
            {
                playLocalData.setUiManager(gameUiManager);
                LogUtils.debug(GameResourceLoader.class, "loadResource ui初始化成功");
            }

            // 游戏内图形引用游戏内UI的字体来源（与启动器画布由 setUiManager 注入对称，供游戏内 putText 取字）
            gameGraphicsManager.quoteUiManager(gameUiManager);
            LogUtils.debug(GameResourceLoader.class, "loadResource 游戏内绘图引用字体成功");

            // script
            ScriptExecutor scriptExecutor = new ScriptExecutor();
            if (!scriptExecutor.init())
            {
                LogUtils.error(GameResourceLoader.class, "loadResource script初始化失败");
                return false;
            }
            else
            {
                playLocalData.setScriptExecutor(scriptExecutor);
                LogUtils.debug(GameResourceLoader.class, "loadResource script初始化成功");
            }

            // animation
            AnimationManager gameAnimationManager = new AnimationManager(eventQueue);
            if (!gameAnimationManager.init())
            {
                LogUtils.error(GameResourceLoader.class, "loadResource 动画初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(GameResourceLoader.class, "loadResource 动画初始化成功");
            }

            // 创建游戏引擎上下文并存入 PlayLocalData
            EngineContext gameEngineContext = new EngineContext(gameAudioManager, gameGraphicsManager, gameAnimationManager);
            playLocalData.setEngineContext(gameEngineContext);

            LogUtils.debug(GameResourceLoader.class, "loadResource 加载游戏资源成功");
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameResourceLoader.class, "loadResource", e);
            return false;
        }
    }

    /**
     * 销毁游戏资源，依次释放消息框、UI、图形和音频
     * @return 销毁是否成功
     */
    public boolean disposeResource ()
    {
        try
        {
            // 销毁脚本执行器
            ScriptExecutor scriptExecutor = playLocalData.getScriptExecutor();
            if (scriptExecutor != null)
            {
                if (!scriptExecutor.dispose())
                {
                    LogUtils.error(GameResourceLoader.class, "disposeResource script销毁失败");
                    return false;
                }
                else
                {
                    LogUtils.debug(GameResourceLoader.class, "disposeResource script销毁成功");
                }
            }

            // 销毁UI（内部会同时销毁弹窗）
            UiManager gameUiManager = playLocalData.getUiManager();
            if (gameUiManager != null)
            {
                if (!gameUiManager.dispose())
                {
                    LogUtils.error(GameResourceLoader.class, "disposeResource ui销毁失败");
                    return false;
                }
                else
                {
                    LogUtils.debug(GameResourceLoader.class, "disposeResource ui销毁成功");
                }
            }

            // 销毁引擎上下文（动画、图形、音频）
            EngineContext gameEngineContext = playLocalData.getEngineContext();
            if (gameEngineContext != null)
            {
                if (!gameEngineContext.dispose())
                {
                    LogUtils.error(GameResourceLoader.class, "disposeResource 引擎上下文销毁失败");
                    return false;
                }
                LogUtils.debug(GameResourceLoader.class, "disposeResource 引擎上下文销毁成功");
            }

            // 恢复启动器引擎上下文到 LayoutService
            layoutManager.setAudioManager(launcherEngineContext.getAudioManager());
            layoutManager.setGraphicsManager(launcherEngineContext.getGraphicsManager());

            LogUtils.debug(GameResourceLoader.class, "disposeResource 销毁游戏资源成功");
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameResourceLoader.class, "disposeResource", e);
            return false;
        }
    }
}
