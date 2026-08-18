package com.hujiugame.qingfeng.type.game;

import com.hujiugame.qingfeng.event.EventAction;

/**
 * 游戏状态事件类型枚举
 * <p>
 * 仅收录与游戏状态栈操作相关的事件（{@link EventAction} 枚举的子集：PUSH/POP/SET/RESET 及其 *_EXECUTE 执行版本），
 * 每个常量引用对应的 {@link EventAction} 枚举常量建立联动，支持与 {@link EventAction} 双向转换：
 * {@link #getEvent()} 取全量事件类型，{@link #fromEvent(EventAction)} 将全量事件类型反查为游戏状态事件类型。
 */
public enum GameStateEventAction
{
    PUSH_GAME_STATE(EventAction.PUSH_GAME_STATE),
    POP_GAME_STATE(EventAction.POP_GAME_STATE),
    SET_GAME_STATE(EventAction.SET_GAME_STATE),
    RESET_GAME_STATE(EventAction.RESET_GAME_STATE),

    PUSH_GAME_STATE_EXECUTE(EventAction.PUSH_GAME_STATE_EXECUTE),
    POP_GAME_STATE_EXECUTE(EventAction.POP_GAME_STATE_EXECUTE),
    SET_GAME_STATE_EXECUTE(EventAction.SET_GAME_STATE_EXECUTE),
    RESET_GAME_STATE_EXECUTE(EventAction.RESET_GAME_STATE_EXECUTE);

    /** 对应的全量事件类型 */
    private final EventAction eventAction;

    /**
     * 构造游戏状态事件枚举常量
     *
     * @param eventAction 对应的全量事件类型
     */
    GameStateEventAction (EventAction eventAction)
    {
        this.eventAction = eventAction;
    }

    /**
     * 获取对应的全量事件类型
     *
     * @return 全量事件类型
     */
    public EventAction getEvent ()
    {
        return eventAction;
    }

    /**
     * 将全量事件类型转换为游戏状态事件类型
     *
     * @param eventAction 全量事件类型
     * @return 对应的游戏状态事件类型；非状态操作事件或 null 返回 null
     */
    public static GameStateEventAction fromEvent (EventAction eventAction)
    {
        if (eventAction == null)
        {
            return null;
        }
        for (GameStateEventAction stateEvent : values())
        {
            if (stateEvent.eventAction == eventAction)
            {
                return stateEvent;
            }
        }
        return null;
    }
}
