package com.hujiugame.qingfeng.event;

import com.hujiugame.qingfeng.type.game.GameStateEventAction;

/**
 * 游戏事件类型枚举
 * <p>
 * 每个枚举常量对应一类事件，携带事件名称字符串（snake_case，与旧版常量类取值一致）；
 * {@link #toString()} 返回事件名称字符串，供日志与外部字符串消费使用。
 * <p>
 * 事件名称是事件分发的唯一依据，新增事件类型时需同步更新本枚举与
 * {@link com.hujiugame.qingfeng.event.EventDispatcher#handleEvent} 的分支。
 */
public enum EventAction
{
    REFRESH_UI_MANAGER("refresh_ui_manager"),
    RECOVER_NORMAL_RENDER_PIPELINE("recover_normal_render_pipeline"),

    PUSH_GAME_STATE("push_game_state"),
    PUSH_GAME_STATE_INIT_SPECIALLY("push_game_state_init_specially"),
    POP_GAME_STATE("pop_game_state"),
    SET_GAME_STATE("set_game_state"),
    RESET_GAME_STATE("reset_game_state"),

    PUSH_GAME_STATE_EXECUTE("push_game_state_execute"),
    POP_GAME_STATE_EXECUTE("pop_game_state_execute"),
    SET_GAME_STATE_EXECUTE("set_game_state_execute"),
    RESET_GAME_STATE_EXECUTE("reset_game_state_execute"),

    ENTER_GAME("enter_game"),
    QUIT_GAME("quit_game"),
    PLAY_GAME("play_game");

    /** 事件名称字符串（snake_case） */
    private final String eventName;

    /**
     * 构造事件枚举常量
     *
     * @param eventName 事件名称字符串
     */
    EventAction (String eventName)
    {
        this.eventName = eventName;
    }

    /**
     * 获取事件名称字符串
     *
     * @return 事件名称字符串（snake_case）
     */
    public String getEventName ()
    {
        return eventName;
    }

    /**
     * 返回事件名称字符串，与 {@link #getEventName()} 一致，便于日志直接拼接
     *
     * @return 事件名称字符串（snake_case）
     */
    @Override
    public String toString ()
    {
        return eventName;
    }

}
