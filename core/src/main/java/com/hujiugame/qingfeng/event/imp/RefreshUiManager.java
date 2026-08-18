package com.hujiugame.qingfeng.event.imp;

import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.ui.UiManager;

/**
 * 刷新 UI 管理器事件，用于替换全局 UiManager 并重进当前场景。
 */
public class RefreshUiManager implements Event
{
    private final EventAction eventAction = EventAction.REFRESH_UI_MANAGER;
    private final UiManager uiManager;

    /**
     * 构造 UI 管理器刷新事件
     *
     * @param uiManager 已用目标主题创建并 init 的新 UiManager
     */
    public RefreshUiManager (UiManager uiManager)
    {
        this.uiManager = uiManager;
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
