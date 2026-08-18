package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.game.GameState;

/**
 * 设置游戏状态执行事件，真正执行状态栈替换。
 */
public class SetGameStateExecute implements Event
{
    private final EventAction eventAction = EventAction.SET_GAME_STATE_EXECUTE;
    private final GameState state;

    /**
     * 构造设置游戏状态执行事件
     *
     * @param state 目标状态
     */
    public SetGameStateExecute (GameState state)
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
