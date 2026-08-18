package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.game.GameState;

/**
 * 推入游戏状态执行事件，真正执行状态栈压入。
 */
public class PushGameStateExecute implements Event
{
    private final EventAction eventAction = EventAction.PUSH_GAME_STATE_EXECUTE;
    private final GameState state;

    /**
     * 构造推入游戏状态执行事件
     *
     * @param state 目标状态
     */
    public PushGameStateExecute (GameState state)
    {
        this.state = state;
    }

    /**
     * 获取目标状态
     *
     * @return 目标状态
     */
    public GameState getState ()
    {
        return state;
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
