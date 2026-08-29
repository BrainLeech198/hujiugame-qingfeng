package com.hujiugame.qingfeng.animation.task.command;

import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

/**
 * 动画指令大类枚举。
 * <p>
 * 对应动画 command 节点中的 {@code type} 字段值，
 * 对标 Script 的 {@link com.hujiugame.qingfeng.script.data.command.ScriptCommandType}。
 */
public enum AnimationCommandType
{
    /** 普通动画 */
    NORMAL(AnimationKey.Task.Command.Type.NORMAL);

    private final String displayString;

    AnimationCommandType (String displayString)
    {
        this.displayString = displayString;
    }

    /**
     * 获取 JSON 中使用的字符串值
     */
    public String getDisplayString ()
    {
        return displayString;
    }

    /**
     * 从 JSON 字符串解析 AnimationCommandType
     *
     * @param jsonValue type 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static AnimationCommandType fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (AnimationCommandType t : values())
        {
            if (t.displayString.equals(jsonValue)) return t;
        }
        return null;
    }
}
