package com.hujiugame.qingfeng.event.imp.state;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;

/**
 * 重置游戏状态执行事件，真正执行状态栈重置。
 */
public class ResetGameStateExecute implements Event
{
    private final EventAction eventAction = EventAction.RESET_GAME_STATE_EXECUTE;

    /**
     * 构造重置游戏状态执行事件
     */
    public ResetGameStateExecute ()
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
