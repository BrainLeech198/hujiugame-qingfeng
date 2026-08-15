package com.hujiugame.qingfeng.scene.impl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.hujiugame.qingfeng.core.UpdateChecker;
import com.hujiugame.qingfeng.data.game.GameStateDataContainer;
import com.hujiugame.qingfeng.input.VirtualInputHandler;
import com.hujiugame.qingfeng.manager.ThemeManager;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.key.RequirementKey;
import com.hujiugame.qingfeng.type.ui.UseViewport;
import com.hujiugame.qingfeng.type.url.WebSite;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.scene.GameRender;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.PushGameState;
import com.hujiugame.qingfeng.util.system.FileUtils;

public final class MenuMain implements GameRender
{
    private final UpdateChecker updateChecker;
    private final AudioManager audioManager;
    private final GraphicsManager graphicsManager;
    private final ThemeManager themeManager;
    private final UiManager uiManager;
    private final EventQueue eventQueue;
    private final UseViewport useViewport;
    private final VirtualInputHandler virtualInputHandler;
    private GameStateDataContainer gameStateDataContainer;

    /** 版本号区域可点击宽高（虚拟坐标） */
    private static final int VERSION_LABEL_WIDTH = 180;
    private static final int VERSION_LABEL_HEIGHT = 40;

    /** 版本号文字缩放与屏幕位置（虚拟坐标） */
    private static final float VERSION_TEXT_SCALE = 1.0f;
    private static final int VERSION_TEXT_X = 5;
    private static final int VERSION_TEXT_Y = 5;

    public MenuMain (UpdateChecker updateChecker, AudioManager audioManager,
                     GraphicsManager graphicsManager, ThemeManager themeManager,
                     UiManager uiManager,
                     EventQueue eventQueue,
                     UseViewport useViewport,
                     VirtualInputHandler virtualInputHandler)
    {
        this.updateChecker = updateChecker;
        this.audioManager = audioManager;
        this.graphicsManager = graphicsManager;
        this.themeManager = themeManager;
        this.uiManager = uiManager;
        this.eventQueue = eventQueue;
        this.useViewport = useViewport;
        this.virtualInputHandler = virtualInputHandler;
    }

    /**
     * 初始化主菜单布局，缓存背景图和网站图标
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    @Override
    public void init (GameStateDataContainer gameStateDataContainer)
    {
        this.gameStateDataContainer = gameStateDataContainer;

        // 缓存当前背景图到app_init.png
        FileHandle backgroundPicturePath = themeManager.getPathHandle().child(PathName.ASSET_S_RESOURCE_IMAGE).child(gameStateDataContainer.getLayoutConfig().getBackgroundPicture());
        FileHandle appInitPicturePath = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET_S_RESOURCE_IMAGE, FileName.DEFAULT_SPLASH));
        FileUtils.copyFile(backgroundPicturePath, appInitPicturePath);

        uiManager.addLayout(gameStateDataContainer.getLayoutConfig());

        // 虚拟输入优先选中：必须在 addLayout 之后，否则 getButton 拿不到控件
        virtualInputHandler.setPriorityConfirmSelectObject(gameStateDataContainer.getConfigJson());
    }

    /**
     * 处理主菜单按钮点击和版本更新检测
     * <p>
     * 性能：每帧 1 次。含 4 次 isButtonClicked 轮询（各做 map 查询）与 4 次 messageBox.handleAsk（map 查询），
     * 仅当 justTouched 时才做坐标反投影；整体为轻量轮询，但均属每帧固定开销，不应在其中新增重活。
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void update (float deltaTime)
    {
        // 点击左下角版本号区域打开官方网站（通过视口将屏幕坐标转换到虚拟坐标系）
        if (Gdx.input.justTouched())
        {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            useViewport.getViewport().unproject(touchPos);
            if (touchPos.x >= 0 && touchPos.x <= VERSION_LABEL_WIDTH && touchPos.y >= 0 && touchPos.y <= VERSION_LABEL_HEIGHT)
            {
                uiManager.getMessageBox().showAsk(RequirementKey.Language.MessageBox.OPEN_OFFICIAL_WEBSITE,
                    "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.OPEN_OFFICIAL_WEBSITE_TITLE + "}",
                    "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.OPEN_OFFICIAL_WEBSITE_CONTENT + "}");
            }
        }

        // 按下开始按钮
        if (uiManager.isButtonClicked(RequirementKey.Ui.MenuMain.BUTTON_START))
        {
            eventQueue.addEvent(new PushGameState(GameState.MENU_LIST));
        }
        // 按下创作按钮
        else if (uiManager.isButtonClicked(RequirementKey.Ui.MenuMain.BUTTON_CREATE))
        {

        }
        // 按下配置按钮
        else if (uiManager.isButtonClicked(RequirementKey.Ui.MenuMain.BUTTON_CONFIG))
        {
            eventQueue.addEvent(new PushGameState(GameState.CONFIG_BASIC));
        }
        // 按下退出按钮
        else if (uiManager.isButtonClicked(RequirementKey.Ui.MenuMain.BUTTON_QUIT))
        {
            uiManager.getMessageBox().showAsk(RequirementKey.Language.MessageBox.QUIT_GAME,
                "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.QUIT_GAME_TITLE + "}",
                "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.QUIT_GAME_CONTENT+ "}");
        }

        // 更新检测
        if (updateChecker.doDetectUpdateFinish())
        {
            // 请求成功
            if (updateChecker.doDetectSuccess())
            {
                if (updateChecker.isNeedVersionUpdate())
                {
                    // 测试版更新：提示可能不稳定；正式版更新：沿用普通提示
                    boolean isBetaUpdate = updateChecker.isNewestVersionBeta();
                    String updateTitleKey = isBetaUpdate
                        ? RequirementKey.Language.MessageBox.UPDATE_DETECTED_BETA_TITLE
                        : RequirementKey.Language.MessageBox.UPDATE_DETECTED_TITLE;
                    String updateContentKey = isBetaUpdate
                        ? RequirementKey.Language.MessageBox.UPDATE_DETECTED_BETA_CONTENT
                        : RequirementKey.Language.MessageBox.UPDATE_DETECTED_CONTENT;
                    uiManager.getMessageBox().showAsk(RequirementKey.Language.MessageBox.UPDATE_DETECTED,
                        "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + updateTitleKey + "}",
                        "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + updateContentKey + "}");
                }
            }
            // 请求失败
            else
            {
                uiManager.getMessageBox().showAsk(RequirementKey.Language.MessageBox.UPDATE_REQUEST_FAILED,
                    "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.UPDATE_REQUEST_FAILED_TITLE + "}",
                    "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.MESSAGE_BOX + "." + RequirementKey.Language.MessageBox.UPDATE_REQUEST_FAILED_CONTENT + "}");
            }
            // 重置检测更新完成状态
            updateChecker.setDoDetectUpdateFinish(false);
        }

        uiManager.getMessageBox().handleAsk(RequirementKey.Language.MessageBox.OPEN_OFFICIAL_WEBSITE,
            () -> Gdx.net.openURI(WebSite.OFFICIAL));

        uiManager.getMessageBox().handleAsk(RequirementKey.Language.MessageBox.QUIT_GAME,
            Gdx.app::exit);

        uiManager.getMessageBox().handleAsk(RequirementKey.Language.MessageBox.UPDATE_DETECTED,
            () -> Gdx.net.openURI(WebSite.OFFICIAL));

        uiManager.getMessageBox().handleAsk(RequirementKey.Language.MessageBox.UPDATE_REQUEST_FAILED,
            () -> Gdx.net.openURI(WebSite.OFFICIAL));

    }

    /**
     * 渲染主菜单布局和版本号文字
     * <p>
     * 性能：每帧 1 次。playLayout 驱动音频 + putLayout 遍历绘制全量布局图片/GIF + putText 内含 getFont map 查询，
     * 是主菜单渲染热路径；绘制资源应预加载，避免在渲染路径内创建。
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void render (float deltaTime)
    {
        audioManager.playLayout(gameStateDataContainer.getLayoutConfig());
        graphicsManager.putLayout(gameStateDataContainer.getLayoutConfig(), deltaTime);
        graphicsManager.putText(updateChecker.getDisplayVersionString(), VERSION_TEXT_SCALE, VERSION_TEXT_X, VERSION_TEXT_Y);
    }

    /**
     * 释放主菜单布局资源
     */
    @Override
    public void dispose ()
    {
        uiManager.deleteLayout(gameStateDataContainer.getLayoutConfig());
    }
}
