package com.hujiugame.qingfeng.scene.impl;

import com.hujiugame.qingfeng.animation.Animation;
import com.hujiugame.qingfeng.animation.AnimationManager;
import com.hujiugame.qingfeng.data.game.GameStateDataContainer;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.key.ui.UniversalUiKey;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.input.VirtualInputHandler;
import com.hujiugame.qingfeng.scene.AbstractGameRender;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.state.PopGameState;

public final class ConfigDisplay extends AbstractGameRender
{
    private final EventQueue eventQueue;
    private final AudioManager audioManager;
    private final GraphicsManager graphicsManager;
    private final UiManager uiManager;
    private final VirtualInputHandler virtualInputHandler;
    private final AnimationManager animationManager;

    // ===================================================================================================================

    public ConfigDisplay (EventQueue eventQueue, AudioManager audioManager,
                        GraphicsManager graphicsManager, UiManager uiManager,
                        VirtualInputHandler virtualInputHandler,
                        AnimationManager animationManager)
    {
        this.eventQueue = eventQueue;
        this.audioManager = audioManager;
        this.graphicsManager = graphicsManager;
        this.uiManager = uiManager;
        this.virtualInputHandler = virtualInputHandler;
        this.animationManager = animationManager;
    }

    /**
     * 初始化配置界面布局
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    @Override
    public GameStateDataContainer getGameStateDataContainer ()
    {
        return gameStateDataContainer;
    }

    @Override
    protected void init (GameStateDataContainer gameStateDataContainer)
    {
        this.gameStateDataContainer = gameStateDataContainer;

        // 提取动画配置
        animationManager.setAnimation(new Animation(gameStateDataContainer.getConfigJson()));

        uiManager.addLayout(gameStateDataContainer.getLayoutConfig());

        // 虚拟输入优先选中：必须在 addLayout 之后，否则 getButton 拿不到控件
        virtualInputHandler.setPriorityConfirmSelectObject(gameStateDataContainer.getConfigJson());
    }

    /**
     * 处理配置界面返回按钮点击
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void update (float deltaTime)
    {
        // 按下返回按钮
        if (uiManager.isButtonClicked(UniversalUiKey.BUTTON_BACK))
        {
            eventQueue.addEvent(new PopGameState(GameState.CONFIG_DISPLAY));
        }
    }

    /**
     * 渲染配置界面布局和音频
     *
     * @param deltaTime 距上一帧的时间差
     */
    @Override
    public void render (float deltaTime)
    {
        audioManager.playLayout(gameStateDataContainer.getLayoutConfig());
        graphicsManager.putLayout(gameStateDataContainer.getLayoutConfig(), deltaTime);
    }

    @Override
    public void transitionRender (float deltaTime)
    {
        // 过渡动画引擎待重新设计，当前 TM 空壳不产生过渡渲染
        render(deltaTime);
    }

    @Override
    public void dispose ()
    {
        uiManager.deleteLayout(gameStateDataContainer.getLayoutConfig());

        this.gameStateDataContainer = null;
    }
}
