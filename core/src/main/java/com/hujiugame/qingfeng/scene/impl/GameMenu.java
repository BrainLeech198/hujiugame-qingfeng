package com.hujiugame.qingfeng.scene.impl;

import com.hujiugame.qingfeng.core.GameHost;
import com.hujiugame.qingfeng.data.game.GameStateDataContainer;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.key.config.RequirementKey;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.input.VirtualInputHandler;
import com.hujiugame.qingfeng.scene.AbstractGameRender;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.state.PushGameState;
import com.hujiugame.qingfeng.event.imp.state.ResetGameState;

public final class GameMenu extends AbstractGameRender
{
    private final EventQueue eventQueue;
    private final GameHost gameHost;
    private final VirtualInputHandler virtualInputHandler;
    private AudioManager gameAudioManager;
    private GraphicsManager gameGraphicsManager;
    private UiManager gameUiManager;

    // ===================================================================================================================

    public GameMenu (EventQueue eventQueue, GameHost gameHost, VirtualInputHandler virtualInputHandler)
    {
        this.eventQueue = eventQueue;
        this.gameHost = gameHost;
        this.virtualInputHandler = virtualInputHandler;
    }

    /**
     * 初始化游戏内菜单布局
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    @Override
    protected void init (GameStateDataContainer gameStateDataContainer)
    {

        gameAudioManager = gameHost.getPlayLocalData().getAudioManager();
        gameGraphicsManager = gameHost.getPlayLocalData().getGraphicsManager();
        gameUiManager = gameHost.getPlayLocalData().getUiManager();
        gameUiManager.addLayout(gameStateDataContainer.getLayoutConfig());

        // 虚拟输入优先选中：必须在 addLayout 之后，否则 getButton 拿不到控件
        virtualInputHandler.setPriorityConfirmSelectObject(gameStateDataContainer.getConfigJson());
    }

    /**
     * 处理游戏内菜单的开始、退出按钮和弹窗回调
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void update (float deltaTime)
    {
        // 开始按钮
        if (gameUiManager.isButtonClicked(RequirementKey.Ui.GameMenu.BUTTON_START))
        {
            eventQueue.addEvent(new PushGameState(GameState.GAME_MENU, GameState.GAME_ROLE));
        }

        // 按下返回按钮
        if (gameUiManager.isButtonClicked(RequirementKey.Ui.GameMenu.BUTTON_QUIT))
        {
            gameUiManager.getMessageBox().showAsk(RequirementKey.Language.InGame.MessageBox.QUIT_GAME,
                "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.InGame.MESSAGE_BOX + "." + RequirementKey.Language.InGame.MessageBox.QUIT_GAME_TITLE + "}",
                "{language$" + RequirementKey.Language.REQUIREMENT_BLOCK + "#" + RequirementKey.Language.InGame.MESSAGE_BOX + "." + RequirementKey.Language.InGame.MessageBox.QUIT_GAME_CONTENT + "}"
            );
        }

        // 检测弹窗
        gameUiManager.getMessageBox().handleAsk(RequirementKey.Language.InGame.MessageBox.QUIT_GAME,
            () ->
            {
                gameHost.getGameSessionManager().quitGame();
                eventQueue.addEvent(new ResetGameState(GameState.GAME_MENU));
            });
    }

    /**
     * 渲染游戏内菜单布局和音频
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void render (float deltaTime)
    {
        gameAudioManager.playLayout(gameStateDataContainer.getLayoutConfig());
        gameGraphicsManager.putLayout(gameStateDataContainer.getLayoutConfig(), deltaTime);
    }

    /**
     * 释放游戏内菜单布局资源
     */
    @Override
    public void transitionRender (float deltaTime)
    {
        // 过渡渲染：当前页面无过渡效果，空实现
    }

    @Override
    public void dispose ()
    {
        gameUiManager.deleteLayout(gameStateDataContainer.getLayoutConfig());
    }

}
