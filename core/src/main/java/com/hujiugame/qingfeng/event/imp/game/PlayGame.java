package com.hujiugame.qingfeng.event.imp.game;

import com.hujiugame.qingfeng.data.story.Role;
import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventAction;
import com.hujiugame.qingfeng.type.play.Hoster;

/**
 * 开始游戏事件
 */
public class PlayGame implements Event
{
    private final EventAction eventAction = EventAction.PLAY_GAME;
    private final Hoster hoster;
    private final Role role;

    /**
     * 构造游戏事件对象
     *
     * @param hoster 游戏主持类型
     * @param role   角色对象
     */
    public PlayGame (Hoster hoster, Role role)
    {
        this.hoster = hoster;
        this.role = role;
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
     * 获取游戏主持类型
     *
     * @return 游戏主持类型
     */
    public Hoster getHoster ()
    {
        return hoster;
    }

    /**
     * 获取角色对象
     *
     * @return 角色对象
     */
    public Role getRole ()
    {
        return role;
    }

}

