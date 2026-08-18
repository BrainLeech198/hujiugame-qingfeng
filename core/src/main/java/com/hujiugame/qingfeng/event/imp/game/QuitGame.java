package com.hujiugame.qingfeng.event.imp.game;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;

/**
 * 退出游戏事件
 */
public class QuitGame implements Event
{
    private final EventAction eventAction = EventAction.QUIT_GAME;

    /**
     * 构造退出游戏事件
     */
    public QuitGame ()
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
