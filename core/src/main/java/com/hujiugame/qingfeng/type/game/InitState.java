package com.hujiugame.qingfeng.type.game;

/**
 * 初始化阶段状态枚举
 * <p>
 * 表示启动流程的阶段推进状态（用户配置→音频→图形→UI→完成），
 * value 为阶段序号（0-4），用于进度条百分比计算；{@link #next()} 用于状态机推进。
 */
public enum InitState
{
    USER_CONFIG(0),
    AUDIO(1),
    GRAPHICS(2),
    UI(3),
    ANIMATION(4),
    TOTAL(5);

    /** 阶段序号 */
    private final int value;

    /**
     * 构造初始化阶段枚举常量
     *
     * @param value 阶段序号
     */
    InitState (int value)
    {
        this.value = value;
    }

    /**
     * 获取阶段序号
     *
     * @return 阶段序号
     */
    public int getValue ()
    {
        return value;
    }

    /**
     * 获取下一个初始化阶段，用于状态机推进；已是最后阶段时返回自身
     *
     * @return 下一阶段
     */
    public InitState next ()
    {
        InitState[] values = values();
        int nextIndex = ordinal() + 1;
        if (nextIndex >= values.length)
        {
            return this;
        }
        return values[nextIndex];
    }
}
