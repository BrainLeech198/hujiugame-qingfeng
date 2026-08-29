package com.hujiugame.qingfeng.scene.impl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.hujiugame.qingfeng.core.InitService;
import com.hujiugame.qingfeng.core.UpdateChecker;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.data.game.GameStateDataContainer;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.scene.AbstractGameRender;
import com.hujiugame.qingfeng.type.ScreenSize;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.type.game.InitState;
import com.hujiugame.qingfeng.type.key.config.ThemeKey;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.util.StringPolisher;
import com.hujiugame.qingfeng.util.interact.NativeDialogUtils;
import com.hujiugame.qingfeng.util.system.FileUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 启动画面渲染机。
 * <p>
 * 负责视觉反馈（背景图、进度条、维修图标），初始化逻辑委托给 {@link InitService}。
 */
public final class Init extends AbstractGameRender
{
    private final InitService initService;
    private final UpdateChecker updateChecker;
    private final GraphicsManager graphicsManager;
    private final UiManager uiManager;
    private final EventQueue eventQueue;

    private static final float STEP_DELAY = 0.4f;

    /** 进度条高度占屏幕高度比例 */
    private static final float PROCESS_BAR_HEIGHT_RATIO = 0.02f;

    /** 维修图标位置与尺寸（虚拟坐标） */
    private static final int REPAIR_IMAGE_X = 2400;
    private static final int REPAIR_IMAGE_Y = 48;
    private static final int REPAIR_IMAGE_SIZE = 96;

    private float initTimer = 0f;

    private String backgroundPictureTag;
    private String processPictureTag;
    private String repairImageTag;
    private String repairImageKind;

    private Color processColor = null;

    /** 正在执行资源修复，阻止状态机继续推进 */
    private boolean repairing = false;

    // ===================================================================================================================

    public Init (InitService initService, UpdateChecker updateChecker,
                 GraphicsManager graphicsManager, UiManager uiManager,
                 EventQueue eventQueue)
    {
        this.initService = initService;
        this.updateChecker = updateChecker;
        this.graphicsManager = graphicsManager;
        this.uiManager = uiManager;
        this.eventQueue = eventQueue;
    }

    // ===================================================================================================================

    /**
     * 显示进度条。
     * <p>
     * 性能：每帧调用 1 次，仅做简单算术和一次 putPicture 绘制，耗时可忽略。
     */
    private void showProcess ()
    {
        // 从 InitService 读取当前进度
        InitState currentState = initService.getInitState();
        float processPercent = (float) (currentState.getValue() + 1) / (InitState.TOTAL.getValue() + 1);
        int processPictureX = 0;
        int processPictureY = 0;
        int processPictureWidth = (int) (ScreenSize.WIDTH * processPercent);
        int processPictureHeight = (int) (ScreenSize.HEIGHT * PROCESS_BAR_HEIGHT_RATIO);

        // 绘制进度（使用主题色叠加）
        if (processColor != null)
        {
            graphicsManager.putPicture(processPictureTag, processPictureX, processPictureY,
                processPictureWidth, processPictureHeight, processColor);
        }
        else
        {
            graphicsManager.putPicture(processPictureTag, processPictureX, processPictureY,
                processPictureWidth, processPictureHeight);
        }
    }

    /**
     * 从外部 app_config.json 读取进度条颜色，读取失败时使用默认色
     */
    private void loadProcessColor ()
    {
        try
        {
            FileHandle appConfigHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET, FileName.APP_CONFIG));
            if (FileUtils.isFileExist(appConfigHandle))
            {
                JsonEntity appConfig = new JsonEntity(appConfigHandle);
                if (appConfig.containsKey(ThemeKey.PROCESS_COLOR))
                {
                    String processColorStr = appConfig.getString(ThemeKey.PROCESS_COLOR);
                    if (processColorStr != null)
                    {
                        processColor = Color.valueOf(processColorStr);
                        LogUtils.debug(Init.class, "loadProcessColor 读取进度条颜色: " + processColor);
                        return;
                    }
                    else
                    {
                        LogUtils.error(Init.class, "loadProcessColor " + ThemeKey.PROCESS_COLOR + " 字段类型不是字符串");
                    }
                }
            }
        }
        catch (Exception e)
        {
            LogUtils.error(Init.class, "loadProcessColor", e);
        }

        // 读取失败时使用默认色
        processColor = Color.valueOf("#3F47B5FF");
        LogUtils.debug(Init.class, "loadProcessColor 使用默认进度条颜色: " + processColor);
    }

    /**
     * 修复游戏资源
     */
    private void repairGame ()
    {
        // 防重入：双击过快会触发两次并发修复，两个线程同时同步资源导致文件损坏
        if (repairing) return;
        repairing = true;
        updateChecker.repairGame(() ->
        {
            NativeDialogUtils.showInfo("修复完成", "游戏资源已修复完成，请重启游戏。", Gdx.app::exit);
        });
    }

    // ===================================================================================================================

    /**
     * 初始化启动画面，缓存背景图、进度条、维修图标资源
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    @Override
    protected void init (GameStateDataContainer gameStateDataContainer)
    {
        backgroundPictureTag = StringPolisher.polished("init");
        processPictureTag = StringPolisher.polished("process");
        repairImageTag = StringPolisher.polished("repair");
        repairImageKind = StringPolisher.polished("repair.image");

        // 背景图缓存
        FileHandle internalSplashPictureHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_RESOURCE_IMAGE, FileName.DEFAULT_SPLASH));
        FileHandle externalSplashPictureHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET_S_RESOURCE_IMAGE, FileName.DEFAULT_SPLASH));

        // 存在外部的背景图缓存文件
        if (!FileUtils.isFileExist(externalSplashPictureHandle))
        {
            graphicsManager.loadBackgroundPicture(backgroundPictureTag, internalSplashPictureHandle);
        }
        else
        {
            graphicsManager.loadBackgroundPicture(backgroundPictureTag, externalSplashPictureHandle);
        }

        // 创建 1x1 白色纹理作为进度条基底（通过 tint 叠加主题色）
        graphicsManager.loadWhitePicture(processPictureTag);

        // 维修图标缓存和显示
        FileHandle repairPictureHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_RESOURCE_IMAGE, FileName.DEFAULT_REPAIR));
        uiManager.loadImageKind(repairImageKind, repairPictureHandle);
        uiManager.addImage(repairImageTag, repairImageKind, REPAIR_IMAGE_X, REPAIR_IMAGE_Y, REPAIR_IMAGE_SIZE, REPAIR_IMAGE_SIZE);

        // 从 app_config.json 读取进度条颜色
        loadProcessColor();
    }

    /**
     * 逐帧执行初始化步骤（委托给 InitService）。
     * <p>
     * 性能：每帧调用 1 次，initTimer 判断 + InitService.stepInit() + 维修按钮检测，耗时可忽略。
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void update (float deltaTime)
    {
        // 每步间隔，让 splash 画面有时间显示
        initTimer += deltaTime;
        if (initTimer < STEP_DELAY) return;
        initTimer = 0f;

        // 检测修复软件按钮按下
        if (uiManager.isImageClicked(repairImageTag))
        {
            repairGame();
        }

        // 修复中跳过状态机，等待修复完成后弹窗退出
        if (repairing) return;

        // 委托 InitService 执行一步初始化
        initService.stepInit();
    }

    /**
     * 渲染启动画面背景和进度条
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void render (float deltaTime)
    {
        // 显示闪图背景
        graphicsManager.putBackgroundPicture(backgroundPictureTag);

        // 进度
        showProcess();
    }

    /**
     * 释放启动画面资源
     */
    @Override
    public void transitionRender (float deltaTime)
    {
        // 直接切换，不走过度
        // 不可能到达的语句
        throw new RuntimeException("transitionRender Init不允许调用 过渡动画渲染");
    }

    @Override
    public void dispose ()
    {
        graphicsManager.disposePicture(processPictureTag);
        uiManager.deleteImage(repairImageTag);
        uiManager.removeImageKind(repairImageKind);
    }
}
