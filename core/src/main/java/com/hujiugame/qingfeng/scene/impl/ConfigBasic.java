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

public final class ConfigBasic extends AbstractGameRender
{
    private final EventQueue eventQueue;
    private final AudioManager audioManager;
    private final GraphicsManager graphicsManager;
    private final UiManager uiManager;
    private final VirtualInputHandler virtualInputHandler;
    private final AnimationManager animationManager;

    // ===================================================================================================================

    public ConfigBasic (EventQueue eventQueue, AudioManager audioManager,
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
    protected void onInit (GameStateDataContainer gameStateDataContainer)
    {
        // 提取动画配置
        animationManager.setAnimation(new Animation(gameStateDataContainer.getConfigJson()));

        uiManager.addLayout(gameStateDataContainer.getLayoutConfig());

        // 虚拟输入优先选中：必须在 addLayout 之后，否则 getLabel 拿不到控件
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
            eventQueue.addEvent(new PopGameState(GameState.CONFIG_BASIC));
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
        if (animationManager.getTransitionManager().isReady())
        {
            // 淡出完成阶段 即淡入
            if (animationManager.getTransitionManager().isFadedOut())
            {
                // 进行中
                if (animationManager.getTransitionManager().isFadingIn())
                {
                    animationManager.getTransitionManager().fadingIn(
                        gameStateDataContainer.getLayoutConfig(),
                        audioManager, graphicsManager, uiManager,
                        deltaTime
                    );
                }
                // 初始化
                else
                {
                    animationManager.getTransitionManager().initFadeIn(
                        gameStateDataContainer.getLayoutConfig(),
                        animationManager.getAnimation(),
                        uiManager
                    );
                }
            }
            // 淡出阶段
            else
            {
                // 进行中
                if (animationManager.getTransitionManager().isFadingOut())
                {
                    animationManager.getTransitionManager().fadingOut(
                        gameStateDataContainer.getLayoutConfig(),
                        audioManager, graphicsManager, uiManager,
                        deltaTime
                    );
                }
                // 初始化
                else
                {
                    animationManager.getTransitionManager().initFadeOut(
                        gameStateDataContainer.getLayoutConfig(),
                        animationManager.getAnimation()
                    );
                }
            }
        }
        else
        {
            animationManager.getTransitionManager().stopTransition();
        }
    }

    @Override
    public void dispose ()
    {
        uiManager.deleteLayout(gameStateDataContainer.getLayoutConfig());

        this.gameStateDataContainer = null;
    }
}
