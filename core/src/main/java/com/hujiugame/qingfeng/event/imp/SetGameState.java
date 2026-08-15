package com.hujiugame.qingfeng.event.imp;

import com.hujiugame.qingfeng.event.EventObject;
import com.hujiugame.qingfeng.type.game.Event;
import com.hujiugame.qingfeng.type.game.GameState;

/**
 * 设置游戏状态事件，用于直接替换当前游戏状态。
 */
public class SetGameState implements EventObject
{
    private final String eventName;
    private final GameState state;

    /**
     * 构造设置游戏状态事件
     *
     * @param state 目标状态
     */
    public SetGameState (GameState state)
    {
        eventName = Event.SET_GAME_STATE;
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
     * @return 事件名称字符串
     */
    @Override
    public String getEventName ()
    {
        return eventName;
    }

}
