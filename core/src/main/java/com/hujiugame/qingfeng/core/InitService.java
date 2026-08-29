package com.hujiugame.qingfeng.core;

import com.hujiugame.qingfeng.animation.AnimationManager;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.state.PushGameStateExecute;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.manager.LanguageManager;
import com.hujiugame.qingfeng.manager.ThemeManager;
import com.hujiugame.qingfeng.manager.UserConfigManager;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.game.InitState;
import com.hujiugame.qingfeng.type.key.config.GameInfoKey;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.util.system.CrashUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 分帧初始化服务。
 * <p>
 * 持有各 Manager 引用，按 {@link InitState} 顺序逐步执行初始化。
 * 由 Init 渲染机每帧调用 {@link #stepInit()}，不在渲染管线内，不产生循环调用。
 * <p>
 * 生命周期：在 {@link com.hujiugame.qingfeng.di.InstanceContent} 的渲染注册表 lambda 中创建，
 * 每个 Init 实例拥有独立的 InitService 实例。
 */
public final class InitService
{
    private final UpdateChecker updateChecker;
    private final GameHost gameHost;
    private final UserConfigManager userConfigManager;
    private final LanguageManager languageManager;
    private final ThemeManager themeManager;
    private final AudioManager audioManager;
    private final GraphicsManager graphicsManager;
    private final UiManager uiManager;
    private final AnimationManager animationManager;
    private final EventQueue eventQueue;

    private InitState initState = InitState.USER_CONFIG;

    /** 是否已完成所有初始化步骤 */
    private boolean finished = false;

    /** 是否出错 */
    private boolean error = false;

    /** 错误信息 */
    private String errorMessage = "";

    // ===================================================================================================================

    public InitService (UpdateChecker updateChecker, GameHost gameHost,
                        UserConfigManager userConfigManager,
                        LanguageManager languageManager, ThemeManager themeManager,
                        AudioManager audioManager,
                        GraphicsManager graphicsManager, UiManager uiManager,
                        AnimationManager animationManager,
                        EventQueue eventQueue)
    {
        this.updateChecker = updateChecker;
        this.gameHost = gameHost;
        this.userConfigManager = userConfigManager;
        this.languageManager = languageManager;
        this.themeManager = themeManager;
        this.audioManager = audioManager;
        this.graphicsManager = graphicsManager;
        this.uiManager = uiManager;
        this.animationManager = animationManager;
        this.eventQueue = eventQueue;
    }

    // ===================================================================================================================

    /**
     * 执行一步初始化（每帧由 Init.update 调用一次）。
     * <p>
     * 按 InitState 状态机顺序推进：USER_CONFIG → AUDIO → GRAPHICS → UI → ANIMATION → TOTAL。
     * USER_CONFIG 阶段需等待 updateChecker.doFileUpdateFinish() 异步完成，可能跨多帧。
     * 其余阶段每帧推进一步。
     * <p>
     * 性能：每帧调用 1 次，大部分阶段为单次 Manager.init() 调用，耗时可忽略；
     * USER_CONFIG 阶段可能跨帧等待，不做重活。
     */
    public void stepInit ()
    {
        if (finished || error) return;

        switch (initState)
        {
            case USER_CONFIG:
                stepUserConfig();
                break;

            case AUDIO:
                stepAudio();
                break;

            case GRAPHICS:
                stepGraphics();
                break;

            case UI:
                stepUi();
                break;

            case ANIMATION:
                stepAnimation();
                break;

            case TOTAL:
                stepStop();
                break;
        }
    }

    // ===================================================================================================================

    /**
     * 获取当前初始化阶段（用于进度条计算）。
     *
     * @return 当前阶段
     */
    public InitState getInitState ()
    {
        return initState;
    }

    /**
     * 是否已完成所有初始化步骤。
     *
     * @return 是否完成
     */
    public boolean isFinished ()
    {
        return finished;
    }

    /**
     * 是否出错。
     *
     * @return 是否出错
     */
    public boolean isError ()
    {
        return error;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息，未出错时返回空字符串
     */
    public String getErrorMessage ()
    {
        return errorMessage;
    }

    // ===================================================================================================================

    /**
     * 初始化用户配置（等待文件差异化更新完成）。
     */
    private void stepUserConfig ()
    {
        // 等待文件差异化更新
        if (updateChecker.doFileUpdateFinish())
        {
            // 记录版本，以及读取用户&游戏配置
            gameHost.getGameInfoManager().putInfo(GameInfoKey.Launcher.VERSION, updateChecker.getInternalVersionString());
            if (!gameHost.getConfigService().loadLauncherConfig())
            {
                LogUtils.error(InitService.class, "stepUserConfig 读取游戏配置失败");
                error = true;
                errorMessage = "读取游戏配置失败";
                CrashUtils.crash(new RuntimeException("stepUserConfig configService.loadLauncherConfig() 读取游戏配置失败"));
                return;
            }
            LogUtils.debug(InitService.class, "stepUserConfig 读取游戏配置成功");
            // 上传用户&游戏配置 到游戏信息管理器
            userConfigManager.uploadTo(gameHost.getGameInfoManager());
            languageManager.uploadTo(gameHost.getGameInfoManager());
            themeManager.uploadTo(gameHost.getGameInfoManager());
            initState = initState.next();
        }
    }

    /**
     * 初始化音频。
     */
    private void stepAudio ()
    {
        if (!audioManager.init())
        {
            LogUtils.error(InitService.class, "stepAudio audioManager.init() 音频初始化失败");
            error = true;
            errorMessage = "音频初始化失败";
            CrashUtils.crash(new RuntimeException("stepAudio audioManager.init() 音频初始化失败"));
            return;
        }
        LogUtils.debug(InitService.class, "stepAudio audioManager.init() 音频初始化成功");
        initState = initState.next();
    }

    /**
     * 初始化图形。
     */
    private void stepGraphics ()
    {
        if (!graphicsManager.init())
        {
            LogUtils.error(InitService.class, "stepGraphics graphicsManager.init() 绘图初始化失败");
            error = true;
            errorMessage = "绘图初始化失败";
            CrashUtils.crash(new RuntimeException("stepGraphics graphicsManager.init() 绘图初始化失败"));
            return;
        }
        LogUtils.debug(InitService.class, "stepGraphics graphicsManager.init() 绘图初始化成功");
        initState = initState.next();
    }

    /**
     * 初始化 UI。
     */
    private void stepUi ()
    {
        if (!uiManager.init(themeManager))
        {
            LogUtils.error(InitService.class, "stepUi uiManager.init() ui初始化失败");
            error = true;
            errorMessage = "UI初始化失败";
            CrashUtils.crash(new RuntimeException("stepUi uiManager.init() ui初始化失败"));
            return;
        }
        LogUtils.debug(InitService.class, "stepUi uiManager.init() ui初始化成功");
        initState = initState.next();
    }

    /**
     * 初始化动画。
     */
    private void stepAnimation ()
    {
        if (!animationManager.init())
        {
            LogUtils.error(InitService.class, "stepAnimation animationManager.init() 动画初始化失败");
            error = true;
            errorMessage = "动画初始化失败";
            CrashUtils.crash(new RuntimeException("stepAnimation animationManager.init() 动画初始化失败"));
            return;
        }
        LogUtils.debug(InitService.class, "stepAnimation animationManager.init() 动画初始化成功");
        initState = initState.next();
    }

    /**
     * 初始化完成，检查更新并跳转菜单。
     */
    private void stepStop ()
    {
        // 链接到网页判断需不需要更新
        updateChecker.checkWebVersion();

        // 跳转菜单
        eventQueue.addEvent(new PushGameStateExecute(GameState.MENU_MAIN));

        finished = true;
        LogUtils.debug(InitService.class, "stepStop 初始化完成，跳转主菜单");
    }
}
