package com.hujiugame.qingfeng.event;

import com.hujiugame.qingfeng.animation.AnimationManager;
import com.hujiugame.qingfeng.core.SceneStack;
import com.hujiugame.qingfeng.di.InstanceContent;
import com.hujiugame.qingfeng.event.imp.RefreshUiManager;
import com.hujiugame.qingfeng.event.imp.state.*;
import com.hujiugame.qingfeng.type.game.GameRenderPipeLineState;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.util.system.LogUtils;
import com.hujiugame.qingfeng.util.system.SafePostRunnable;

import java.util.Objects;

public final class EventDispatcher
{
    private SceneStack sceneStack;

    /**
     * 初始化事件服务，绑定状态服务
     *
     * @param sceneStack 游戏状态服务
     * @return 是否初始化成功
     */
    public boolean init (SceneStack sceneStack)
    {
        try
        {
            this.sceneStack = sceneStack;
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(EventDispatcher.class, "init", e);
            return false;
        }
    }

    // ===================================================================================================================

    /**
     * 分发事件到对应的处理方法（REFRESH_UI_MANAGER/PUSH/POP/SET/RESET 及对应 *_EXECUTE/ENTER/QUIT/PLAY）
     * <p>
     * 性能：事件分发回调，调用频率随事件量变化（页面切换、点击等）；内部 switch 分派 + 各 handler 日志拼接，
     * 单次开销小，但 DEBUG 级日志在热路径累积；应避免在事件 handler 内做重活。
     *
     * @param event 事件对象
     */
    public void handleEvent (Event event)
    {
        try
        {
            // 获取事件类型
            EventAction eventAction = event.getEventAction();
            LogUtils.debug(EventDispatcher.class, "handleEvent 事件名: " + eventAction);

            // 根据事件类型执行对应的方法
            switch (eventAction)
            {
                case REFRESH_UI_MANAGER:
                    handleEventOfRefreshUiManager(event);
                    break;

                case RECOVER_NORMAL_RENDER_PIPELINE:
                    handleEventOfRecoverNormalRenderPipeline(event);
                    break;

                case PUSH_GAME_STATE:
                    handleEventOfPushGameState(event);
                    break;

                case PUSH_GAME_STATE_INIT_SPECIALLY:
                    handleEventOfPushGameStateInitSpecially(event);
                    break;

                case POP_GAME_STATE:
                    handleEventOfPopGameState(event);
                    break;

                case SET_GAME_STATE:
                    handleEventOfSetGameState(event);
                    break;

                case RESET_GAME_STATE:
                    handleEventOfResetGameState(event);
                    break;

                case PUSH_GAME_STATE_EXECUTE:
                    handleEventOfPushGameStateExecute(event);
                    break;

                case POP_GAME_STATE_EXECUTE:
                    handleEventOfPopGameStateExecute();
                    break;

                case SET_GAME_STATE_EXECUTE:
                    handleEventOfSetGameStateExecute(event);
                    break;

                case RESET_GAME_STATE_EXECUTE:
                    handleEventOfResetGameStateExecute();
                    break;

                case ENTER_GAME:
                    handleEventOfEnterGame();
                    break;

                case QUIT_GAME:
                    handleEventOfQuitGame();
                    break;

                case PLAY_GAME:
                    handleEventOfPlayGame();
                    break;

                default:
                    break;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(EventDispatcher.class, "handleEvent", e);
            throw new RuntimeException(e);
        }
    }

    // ===================================================================================================================

    /**
     * 处理 UI 管理器刷新事件：将调用方创建好的新 UiManager 整体替换进全局持有者，
     * 重进当前场景使渲染机用新实例重建页面，旧 UiManager 延后到帧间释放。
     *
     * @param event 事件对象（RefreshUiManager，携带已用目标主题创建并 init 的新 UiManager）
     */
    private void handleEventOfRefreshUiManager (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfRefreshUiManager 尝试替换新Ui管理器");

        // 取出事件携带的新 UiManager（创建与 init 由调用方完成：themeManager.reload → new UiManager → init）
        RefreshUiManager refreshEvent = (RefreshUiManager) event;
        UiManager newUiManager = refreshEvent.getUiManager();
        InstanceContent instanceContent = InstanceContent.getInstance();

        // 记下旧 UiManager：此刻它仍被当前渲染机引用，必须在场景重进（旧渲染机 dispose）之后才能释放
        UiManager oldUiManager = instanceContent.getUiManager();

        // 替换新 UiManager 进所有持有者（setUiManager 内部连带切换图形字体来源、布局管理器、虚拟输入的引用）
        instanceContent.setUiManager(newUiManager);

        LogUtils.debug(EventDispatcher.class, "handleEventOfRefreshUiManager 尝试刷新渲染机以及场景");
        // 场景重进：内部先 dispose 当前渲染机（其持有旧 UiManager，此刻旧实例仍存活可安全删控件），再用新 UiManager 重建页面
        sceneStack.refreshGameState();

        // 帧间释放旧 UiManager：当前帧渲染已切到新实例，旧实例不再被任何持有者引用，延后到下一帧执行避免在渲染帧中释放资源
        SafePostRunnable.post(oldUiManager::dispose);
        LogUtils.debug(EventDispatcher.class, "handleEventOfRefreshUiManager 刷新Ui管理器成功");
    }

    private void handleEventOfRecoverNormalRenderPipeline (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfRecoverNormalRenderPipeline 尝试恢复正常渲染管线");
        // 恢复正常渲染管线
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.NORMAL);
        LogUtils.debug(EventDispatcher.class, "handleEventOfRecoverNormalRenderPipeline 恢复正常渲染管线成功");
    }

    // ===================================================================================================================

    /**
     * 处理游戏状态压栈事件（空壳事件）
     * <p>
     * 仅记录意图并启动淡出（TransitionManager 接入后填充），不直接切换状态栈；
     * 淡出完成后由动画过渡链入队 {@link PushGameStateExecute} 执行事件，由执行事件真正压栈。
     *
     * @param event 事件对象
     */
    private void handleEventOfPushGameState (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 尝试预处理推入游戏状态 " +
            "(PushGameState):{ (outState, inState): " + ((PushGameState) event).getOutState() + ", " + ((PushGameState) event).getInState() + " }");

        // 设置到正在使用的动画管理器（游戏内用游戏版，启动器用启动器版）
        getCurrentAnimationManager().getTransitionManager().setPendingTask(event);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 设置待处理任务成功");

        // 启动特殊渲染管线状态
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.FADING);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 启动特殊渲染管线状态成功");

        // 启动过度状态
        getCurrentAnimationManager().getTransitionManager().startTransition();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 启动过度状态成功");

        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 预处理游戏状态成功");
    }

    /**
     * 处理游戏状态压栈Init特殊处理事件（空壳事件）
     * <p>
     * 仅记录意图并启动淡出（TransitionManager 接入后填充），不直接切换状态栈；
     * 淡出完成后由动画过渡链入队 {@link PushGameStateExecute} 执行事件，由执行事件真正压栈。
     *
     * @param event 事件对象
     */
    private void handleEventOfPushGameStateInitSpecially (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 尝试预处理推入游戏状态 " +
            "(PushGameState):{ (outState, inState): " + ((PushGameStateInitSpecially) event).getOutState() + ", " + ((PushGameStateInitSpecially) event).getInState() + " }");

        // 创建 PushGameState 事件
        Event pushGameState = new PushGameState(((PushGameStateInitSpecially) event).getOutState(), ((PushGameStateInitSpecially) event).getInState());
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 创建 PushGameState 事件成功");

        // 设置到正在使用的动画管理器（游戏内用游戏版，启动器用启动器版）
        getCurrentAnimationManager().getTransitionManager().setPendingTask(pushGameState);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 设置待处理任务成功");

        // 启动特殊渲染管线状态
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.FADING);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 启动特殊渲染管线状态成功");

        // 启动过度状态
        getCurrentAnimationManager().getTransitionManager().startTransition();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 启动过度状态成功");

        // 立即完成淡入
        getCurrentAnimationManager().getTransitionManager().finishFadingOut();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 立即完成淡出成功");

        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateInitSpecially 预处理游戏状态成功");
    }

    /**
     * 处理游戏状态弹栈事件（空壳事件）
     * <p>
     * 仅记录意图并启动淡出（TransitionManager 接入后填充），不直接切换状态栈；
     * 淡出完成后由动画过渡链入队 {@link PopGameStateExecute} 执行事件，由执行事件真正弹栈。
     *
     * @param event 事件对象
     */
    private void handleEventOfPopGameState (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 尝试预处理弹出游戏状态 " +
            "(PopGameState):{ (outState): " + ((PopGameState) event).getOutState() + " }");

        // 获取pop下一次状态
        ((PopGameState) event).setInState(sceneStack.getSecondGameState());

        // 设置到正在使用的动画管理器（游戏内用游戏版，启动器用启动器版）
        getCurrentAnimationManager().getTransitionManager().setPendingTask(event);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 设置待处理任务成功");

        // 启动特殊渲染管线状态
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.FADING);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 启动特殊渲染管线状态成功");

        // 启动过度状态
        getCurrentAnimationManager().getTransitionManager().startTransition();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 启动过度状态成功");

        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 预处理弹出游戏状态成功");
    }

    /**
     * 处理游戏状态设置事件（空壳事件）
     * <p>
     * 仅记录意图并启动淡出（TransitionManager 接入后填充），不直接切换状态栈；
     * 淡出完成后由动画过渡链入队 {@link SetGameStateExecute} 执行事件，由执行事件真正替换状态。
     *
     * @param event 事件对象
     */
    private void handleEventOfSetGameState (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 尝试预处理设置游戏状态 " +
            "(SetGameState):{ (outState, inState): " + ((SetGameState) event).getOutState() + ", " + ((SetGameState) event).getInState() + " }");

        // 设置到正在使用的动画管理器（游戏内用游戏版，启动器用启动器版）
        getCurrentAnimationManager().getTransitionManager().setPendingTask(event);
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 设置待处理任务成功");

        // 启动特殊渲染管线状态
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.FADING);
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 启动特殊渲染管线状态成功");

        // 启动过度状态
        getCurrentAnimationManager().getTransitionManager().startTransition();
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 启动过度状态成功");

        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 预处理设置游戏状态成功");
    }

    /**
     * 处理游戏状态重置事件（空壳事件）
     * <p>
     * 仅记录意图并启动淡出（TransitionManager 接入后填充），不直接切换状态栈；
     * 淡出完成后由动画过渡链入队 {@link ResetGameStateExecute} 执行事件，由执行事件真正重置。
     */
    private void handleEventOfResetGameState (Event event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 尝试预处理重置游戏状态");

        // 设置到正在使用的动画管理器（游戏内用游戏版，启动器用启动器版）
        getCurrentAnimationManager().getTransitionManager().setPendingTask(event);
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 设置待处理任务成功");

        // 启动特殊渲染管线状态
        Objects.requireNonNull(InstanceContent.getInstance()).getRenderPipeline().setState(GameRenderPipeLineState.FADING);
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 启动特殊渲染管线状态成功");

        // 启动过度状态
        getCurrentAnimationManager().getTransitionManager().startTransition();
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 启动过度状态成功");

        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 预处理重置游戏状态成功");
    }

    /**
     * 获取当前正在使用的动画管理器
     * <p>
     * 启动器与游戏各持有一个 AnimationManager 实例：通过 GameHost 判断当前是否处于游戏内
     * （与 VirtualInputHandler 的 UiManager 切换模式一致），游戏内用游戏版（PlayLocalData），
     * 启动器用 InstanceContent 版。
     *
     * @return 当前正在使用的动画管理器
     */
    private AnimationManager getCurrentAnimationManager ()
    {
        if (InstanceContent.getInstance().getGameHost().getGameSessionManager().isInGame())
        {
            return InstanceContent.getInstance().getGameHost().getPlayLocalData().getAnimationManager();
        }
        return InstanceContent.getInstance().getAnimationManager();
    }

    /**
     * 处理游戏状态压栈执行事件：真正执行状态栈压入（淡出完成后由动画过渡链入队）
     *
     * @param event 事件对象
     */
    private void handleEventOfPushGameStateExecute (Event event)
    {
        PushGameStateExecute pushEvent = (PushGameStateExecute) event;
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateExecute 尝试执行推入游戏状态 " +
            "(PushGameStateExecute):{ (State): " + pushEvent.getState() + " }");
        sceneStack.pushGameState(pushEvent.getState());
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameStateExecute 执行推入游戏状态成功");
    }

    /**
     * 处理游戏状态弹栈执行事件：真正执行状态栈弹出（淡出完成后由动画过渡链入队）
     */
    private void handleEventOfPopGameStateExecute ()
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameStateExecute 尝试执行弹出游戏状态");
        sceneStack.popGameState();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameStateExecute 执行弹出游戏状态成功");
    }

    /**
     * 处理游戏状态设置执行事件：真正执行状态栈替换（淡出完成后由动画过渡链入队）
     *
     * @param event 事件对象
     */
    private void handleEventOfSetGameStateExecute (Event event)
    {
        SetGameStateExecute setEvent = (SetGameStateExecute) event;
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameStateExecute 尝试执行设置游戏状态 " +
            "(SetGameStateExecute):{ (State): " + setEvent.getState() + " }");
        sceneStack.setGameState(setEvent.getState());
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameStateExecute 执行设置游戏状态成功");
    }

    /**
     * 处理游戏状态重置执行事件：真正执行状态栈重置（淡出完成后由动画过渡链入队）
     */
    private void handleEventOfResetGameStateExecute ()
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameStateExecute 尝试执行重置游戏状态");
        if (sceneStack.resetGameState())
        {
            LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameStateExecute 重置游戏状态成功");
        }
        else
        {
            LogUtils.error(EventDispatcher.class, "handleEventOfResetGameStateExecute 重置游戏状态失败");
        }
    }

    // ===================================================================================================================

    /**
     * 处理进入游戏事件
     *
     */
    private void handleEventOfEnterGame ()
    {
        // 进入游戏统一从游戏菜单页开始，其余状态由玩家在游戏内逐步进入
        PushGameState pushGameStateEvent = new PushGameState(null, GameState.GAME_MENU);
        LogUtils.debug(EventDispatcher.class, "handleEventOfEnterGame 尝试执行进入游戏状态 " +
            "(PushGameState):{ (State): " + pushGameStateEvent.getOutState() + ", " + pushGameStateEvent.getInState() + " }");
        handleEventOfPushGameState(pushGameStateEvent);
        LogUtils.debug(EventDispatcher.class, "handleEventOfEnterGame 执行进入游戏状态成功");
    }

    /**
     * 处理退出游戏事件
     */
    private void handleEventOfQuitGame ()
    {
        // 退出游戏直接重置回主菜单，清空游戏内的状态栈（统一复用重置逻辑）
        ResetGameState resetGameStateEvent = new ResetGameState(null);
        LogUtils.debug(EventDispatcher.class, "handleEventOfQuitGame 尝试执行退出游戏状态 (ResetGameState)");
        handleEventOfResetGameState(resetGameStateEvent);
        LogUtils.debug(EventDispatcher.class, "handleEventOfQuitGame 执行退出游戏状态成功");
    }

    /**
     * 处理游戏开始事件
     *
     */
    public void handleEventOfPlayGame ()
    {
        // 开始游戏压入游戏播放页，从玩家选择的角色故事进入实际游玩
        PushGameState pushGameStateEvent = new PushGameState(null, GameState.GAME_PLAY);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPlayGame 尝试执行游戏开始状态 " +
            "(PushGameState):{ (State): " + pushGameStateEvent.getOutState() + ", " + pushGameStateEvent.getInState() + " }");
        handleEventOfPushGameState(pushGameStateEvent);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPlayGame 执行游戏开始状态成功");
    }

    // ===================================================================================================================

    /**
     * 销毁事件服务
     *
     * @return 是否销毁成功
     */
    public boolean dispose ()
    {
        try
        {
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(EventDispatcher.class, "dispose", e);
            return false;
        }
    }
}
