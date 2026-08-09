package com.hujiugame.qingfeng.event.imp;

import com.hujiugame.qingfeng.event.EventObject;
import com.hujiugame.qingfeng.type.game.Event;
import com.hujiugame.qingfeng.ui.UiManager;

public class RefreshUiManager implements EventObject
{
    private final String eventName;
    private final UiManager uiManager;

    /**
     * 构造 UI 管理器刷新事件
     *
     * @param uiManager 已用目标主题创建并 init 的新 UiManager
     */
    public RefreshUiManager (UiManager uiManager)
    {
        eventName = Event.REFRESH_UI_MANAGER;
        this.uiManager = uiManager;
    }

    /**
     * 获取事件名称
     *
     * @return 事件名称字符串
     */
    @Override
    public String getEventName ()
    {
        return eventName;
    }

    /**
     * 获取新的 UI 管理器
     *
     * @return 新 UI 管理器
     */
    public UiManager getUiManager ()
    {
        return uiManager;
    }
}
