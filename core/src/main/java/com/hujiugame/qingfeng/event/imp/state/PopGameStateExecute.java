package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;

/**
 * 弹出游戏状态执行事件，真正执行状态栈弹出。
 */
public class PopGameStateExecute implements Event
{
    private final EventAction eventAction = EventAction.POP_GAME_STATE_EXECUTE;

    /**
     * 构造弹出游戏状态执行事件
     */
    public PopGameStateExecute ()
    {
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
