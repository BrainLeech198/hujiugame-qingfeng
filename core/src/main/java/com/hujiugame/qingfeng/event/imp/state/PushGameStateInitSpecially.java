package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.game.GameState;

public class PushGameStateInitSpecially implements Event
{
    private final EventAction eventAction = EventAction.PUSH_GAME_STATE_INIT_SPECIALLY;
    private GameState outState = GameState.INIT;
    private GameState inState;

    /**
     * Init专用的推入游戏状态事件
     *
     * @param inState 进入状态
     */
    public PushGameStateInitSpecially (GameState inState)
    {
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
