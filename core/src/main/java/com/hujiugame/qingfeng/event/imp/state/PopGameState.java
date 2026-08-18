package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.game.GameState;

/**
 * 弹出游戏状态事件（空壳事件），用于从状态栈中移除当前状态。
 * <p>
 * 仅记录切换意图，不携带动画组件；淡入淡出动画由过渡链按页获取
 * （淡出取当前页动画，淡入取切换后目标页动画）。
 */
public class PopGameState implements Event
{
    private final EventAction eventAction = EventAction.POP_GAME_STATE;
    private GameState outState;
    private GameState inState;

    /**
     * 构造弹出游戏状态事件
     *
     * @param outState 弹出状态
     */
    public PopGameState (GameState outState)
    {
        this.outState = outState;
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
     * 获取进入状态
     *
     * @return 进入状态
     */
    public GameState getInState ()
    {
        return inState;
    }

    /**
     * 设置进入状态
     *
     * @param inState 进入状态
     */
    public void setInState (GameState inState)
    {
        this.inState = inState;
    }

    /**
     * 获取事件名称
     *
     * @return 事件类型
     */
    @Override
    public EventAction getEventAction ()
    {
        return eventAction;
    }

}
