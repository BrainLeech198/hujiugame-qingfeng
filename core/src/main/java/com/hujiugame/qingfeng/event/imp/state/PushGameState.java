package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.game.GameState;

/**
 * 推入游戏状态事件（空壳事件），用于向状态栈压入目标状态。
 * <p>
 * 仅记录切换意图，不携带动画组件；淡入淡出动画由过渡链按页获取
 * （淡出取当前页动画，淡入取切换后目标页动画）。
 */
public class PushGameState implements Event
{
    private final EventAction eventAction = EventAction.PUSH_GAME_STATE;
    private GameState outState;
    private GameState inState;

    /**
     * 构造推入游戏状态事件
     *
     * @param outState 弹出状态
     * @param inState 进入状态
     */
    public PushGameState (GameState outState, GameState inState)
    {
        this.outState = outState;
        this.inState = inState;
    }

    /**
     * 获取弹出状态
     *
     * @return 弹出状态
     */
    public GameState getOutState ()
    {
        return outState;
    }

    /**
     * 获取目标状态
     *
     * @return 目标状态
     */
    public GameState getInState ()
    {
        return inState;
    }

    /**
     * 获取事件名称
     * @return 事件类型
     */
    @Override
    public EventAction getEventAction ()
    {
        return eventAction;
    }

}
