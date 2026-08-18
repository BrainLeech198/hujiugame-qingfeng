package com.hujiugame.qingfeng.event;

/**
 * 事件对象接口，所有具体事件类型需实现此接口。
 */
public interface Event
{
    /**
     * 获取事件类型
     *
     * @return 事件类型枚举
     */
    EventAction getEventAction ();
}
