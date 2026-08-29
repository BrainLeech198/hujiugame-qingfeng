package com.hujiugame.qingfeng.animation.manager;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.RecoverNormalRenderPipeLine;
import com.hujiugame.qingfeng.event.imp.state.*;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.game.GameStateEventAction;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 过渡动画管理器（空壳）。
 * <p>
 * 当前仅保留双事件模型骨架：空壳事件记录意图 → executePendingTask 入队执行事件。
 * 动画执行引擎待重新设计后填充。
 */
public final class TransitionManager
{
    private final EventQueue eventQueue;

    private GameStateEventAction gameStateEventAction;
    private GameState outState;
    private GameState inState;

    public TransitionManager (EventQueue eventQueue)
    {
        this.eventQueue = eventQueue;
    }

    // ===================================================================================================================

    /**
     * 设置待切换的游戏状态。
     * <p>
     * 由 EventDispatcher 在空壳事件（PUSH/POP/SET/RESET）处理时调用，
     * 记录切换意图（outState → inState），供 executePendingTask 入队真正的执行事件。
     *
     * @param event 空壳事件
     */
    public void setPendingTask (Event event)
    {
        GameStateEventAction stateEventAction = GameStateEventAction.fromEvent(event.getEventAction());

        if (stateEventAction != null)
        {
            gameStateEventAction = stateEventAction;
            switch (gameStateEventAction)
            {
                case PUSH_GAME_STATE:
                    outState = ((PushGameState) event).getOutState();
                    inState = ((PushGameState) event).getInState();
                    break;

                case SET_GAME_STATE:
                    outState = ((SetGameState) event).getOutState();
                    inState = ((SetGameState) event).getInState();
                    break;

                case POP_GAME_STATE:
                    outState = ((PopGameState) event).getOutState();
                    inState = ((PopGameState) event).getInState();
                    break;

                case RESET_GAME_STATE:
                    outState = ((ResetGameState) event).getOutState();
                    inState = GameState.UNKNOWN;
                    break;

                default:
                    outState = GameState.UNKNOWN;
                    inState = GameState.UNKNOWN;
                    LogUtils.error(this.getClass(), "setPendingTask 忽略 *_EXECUTE 执行事件 (event): " + event.getEventAction());
                    break;
            }
            LogUtils.debug(this.getClass(), "setPendingTask 设置待切换状态 " +
                "(event): " + event.getEventAction() + " (outState): " + outState + " (inState): " + inState);
        }
        else
        {
            LogUtils.error(this.getClass(), "setPendingTask 忽略非状态操作事件 (event): " + event.getEventAction());
        }
    }

    /**
     * 执行待切换任务：根据记录的意图入队对应的执行事件。
     * <p>
     * 空壳阶段由 startTransition 直接调用，无动画延迟。
     * 未来填充动画引擎后，此方法由淡出完成时调用。
     */
    public void executePendingTask ()
    {
        if (inState != GameState.UNKNOWN)
        {
            LogUtils.debug(this.getClass(), "executePendingTask 执行待切换任务 (state): " + inState);
            switch (gameStateEventAction)
            {
                case PUSH_GAME_STATE:
                    eventQueue.addEvent(new PushGameStateExecute(inState));
                    break;

                case POP_GAME_STATE:
                    eventQueue.addEvent(new PopGameStateExecute());
                    break;

                case SET_GAME_STATE:
                    eventQueue.addEvent(new SetGameStateExecute(inState));
                    break;

                case RESET_GAME_STATE:
                    eventQueue.addEvent(new ResetGameStateExecute());
                    break;

                default:
                    LogUtils.error(this.getClass(), "executePendingTask 错误的事件操作类型 (event): " + gameStateEventAction);
                    break;
            }
            LogUtils.debug(this.getClass(), "executePendingTask 执行待切换任务完成 (state): " + inState);
        }
        else
        {
            LogUtils.error(this.getClass(), "executePendingTask 没有设置待切换任务");
        }
    }

    // ===================================================================================================================

    /**
     * 渲染过渡效果。
     * <p>
     * 空壳阶段：无动画，无需渲染。
     * 未来填充动画引擎后，此方法由淡出和淡入调用。
     */
    public void render ()
    {
    }

    /**
     * 启动过渡效果。
     * <p>
     * 空壳阶段：无动画，直接执行待切换任务并恢复正常渲染管线。
     * 未来填充动画引擎后，此方法启动淡出，淡出完成后才调用 executePendingTask。
     */
    public void startTransition ()
    {
        executePendingTask();
        stopTransition();
    }

    /**
     * 停止过渡效果，恢复正常渲染管线。
     * <p>
     * 未来填充动画引擎后，此方法由淡入完成时调用。
     */
    public void stopTransition ()
    {
        recoverRenderPipeline();
    }

    /**
     * 恢复渲染管线
     */
    private void recoverRenderPipeline ()
    {
        // 恢复渲染管线
        eventQueue.addEvent(new RecoverNormalRenderPipeLine());
    }

}
