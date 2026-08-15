package com.hujiugame.qingfeng.event;

import com.hujiugame.qingfeng.core.SceneStack;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.di.InstanceContent;
import com.hujiugame.qingfeng.event.imp.PushGameState;
import com.hujiugame.qingfeng.event.imp.RefreshUiManager;
import com.hujiugame.qingfeng.event.imp.ResetGameState;
import com.hujiugame.qingfeng.event.imp.SetGameState;
import com.hujiugame.qingfeng.type.game.Event;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.util.system.LogUtils;
import com.hujiugame.qingfeng.util.system.SafePostRunnable;

public final class EventDispatcher
{
    private SceneStack sceneStack;

    /**
     * 初始化事件服务，绑定状态服务
     *
     * @param playLocalData  游戏数据容器
     * @param sceneStack     游戏状态服务
     * @return 是否初始化成功
     */
    public boolean init (PlayLocalData playLocalData,
                         SceneStack sceneStack)
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
     * 分发事件到对应的处理方法（REFRESH_UI_MANAGER/PUSH/POP/SET/RESET/ENTER/QUIT/PLAY）
     *
     * @param eventObject 事件对象
     */
    public void handleEvent (EventObject eventObject)
    {
        try
        {
            // 获取事件名
            String eventName = eventObject.getEventName();
            LogUtils.debug(EventDispatcher.class, "handleEvent 事件名: " + eventName);

            // 根据事件名执行对应的方法
            switch (eventName)
            {
                case Event.REFRESH_UI_MANAGER:
                    handleEventOfRefreshUiManager(eventObject);
                    break;

                case Event.PUSH_GAME_STATE:
                    handleEventOfPushGameState(eventObject);
                    break;

                case Event.POP_GAME_STATE:
                    handleEventOfPopGameState(eventObject);
                    break;

                case Event.SET_GAME_STATE:
                    handleEventOfSetGameState(eventObject);
                    break;

                case Event.RESET_GAME_STATE:
                    handleEventOfResetGameState();
                    break;

                case Event.ENTER_GAME:
                    handleEventOfEnterGame();
                    break;

                case Event.QUIT_GAME:
                    handleEventOfQuitGame();
                    break;

                case Event.PLAY_GAME:
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
    private void handleEventOfRefreshUiManager (EventObject event)
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

    /**
     * 处理游戏状态压栈事件
     *
     * @param event 事件对象
     */
    private void handleEventOfPushGameState (EventObject event)
    {
        PushGameState pushEvent = (PushGameState) event;
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 尝试执行推入游戏状态 (PushGameState):{ (State): " + pushEvent.getState() + " }");
        sceneStack.pushGameState(pushEvent.getState());
        LogUtils.debug(EventDispatcher.class, "handleEventOfPushGameState 执行推入游戏状态成功");
    }

    /**
     * 处理游戏状态弹栈事件
     *
     * @param event 事件对象
     */
    private void handleEventOfPopGameState (EventObject event)
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 尝试执行弹出游戏状态");
        sceneStack.popGameState();
        LogUtils.debug(EventDispatcher.class, "handleEventOfPopGameState 执行弹出游戏状态成功");
    }

    /**
     * 处理游戏状态设置事件
     *
     * @param eventObject 事件对象
     */
    private void handleEventOfSetGameState (EventObject eventObject)
    {
        SetGameState setEvent = (SetGameState) eventObject;
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 尝试执行设置游戏状态 (SetGameState):{ (State): " + setEvent.getState() + " }");
        sceneStack.setGameState(setEvent.getState());
        LogUtils.debug(EventDispatcher.class, "handleEventOfSetGameState 执行设置游戏状态成功");
    }

    /**
     * 处理游戏状态重置事件
     */
    private void handleEventOfResetGameState ()
    {
        LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 尝试执行重置游戏状态");
        if (sceneStack.resetGameState())
        {
            LogUtils.debug(EventDispatcher.class, "handleEventOfResetGameState 重置游戏状态成功");
        }
        else
        {
            LogUtils.error(EventDispatcher.class, "handleEventOfResetGameState 重置游戏状态失败");
        }
    }

    /**
     * 处理进入游戏事件
     *
     */
    private void handleEventOfEnterGame ()
    {
        // 进入游戏统一从游戏菜单页开始，其余状态由玩家在游戏内逐步进入
        PushGameState pushGameStateEvent = new PushGameState(GameState.GAME_MENU);
        LogUtils.debug(EventDispatcher.class, "handleEventOfEnterGame 尝试执行进入游戏状态 (PushGameState):{ (State): " + pushGameStateEvent.getState() + " }");
        handleEventOfPushGameState(pushGameStateEvent);
        LogUtils.debug(EventDispatcher.class, "handleEventOfEnterGame 执行进入游戏状态成功");
    }

    /**
     * 处理退出游戏事件
     */
    private void handleEventOfQuitGame ()
    {
        // 退出游戏直接重置回主菜单，清空游戏内的状态栈（统一复用重置逻辑）
        ResetGameState resetGameStateEvent = new ResetGameState();
        LogUtils.debug(EventDispatcher.class, "handleEventOfQuitGame 尝试执行退出游戏状态 (ResetGameState)");
        handleEventOfResetGameState();
        LogUtils.debug(EventDispatcher.class, "handleEventOfQuitGame 执行退出游戏状态成功");
    }

    /**
     * 处理游戏开始事件
     *
     */
    public void handleEventOfPlayGame ()
    {
        // 开始游戏压入游戏播放页，从玩家选择的角色故事进入实际游玩
        PushGameState pushGameStateEvent = new PushGameState(GameState.GAME_PLAY);
        LogUtils.debug(EventDispatcher.class, "handleEventOfPlayGame 尝试执行游戏开始状态 (PushGameState):{ (State): " + pushGameStateEvent.getState() + " }");
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
