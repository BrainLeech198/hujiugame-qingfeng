package com.hujiugame.qingfeng.animation.task.action;

import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

/**
 * 动画动作类型枚举。
 * <p>
 * 对应动画 action 节点中的 {@code type} 字段值。
 */
public enum AnimationActionType
{
    NONE(AnimationKey.Task.Action.Type.NONE),
    SMOOTH_MOVE(AnimationKey.Task.Action.Type.SMOOTH_MOVE);

    private final String displayString;

    AnimationActionType (String displayString)
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
     * 从 JSON 字符串解析 AnimationActionType
     *
     * @param jsonValue type 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static AnimationActionType fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (AnimationActionType t : values())
        {
            if (t.displayString.equals(jsonValue)) return t;
        }
        return null;
    }
}
