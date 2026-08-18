package com.hujiugame.qingfeng.event.imp;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;

public class RecoverNormalRenderPipeLine implements Event
{
    private final EventAction eventAction = EventAction.RECOVER_NORMAL_RENDER_PIPELINE;

    /**
     * 构造恢复正常渲染管线事件
     */
    public RecoverNormalRenderPipeLine ()
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
