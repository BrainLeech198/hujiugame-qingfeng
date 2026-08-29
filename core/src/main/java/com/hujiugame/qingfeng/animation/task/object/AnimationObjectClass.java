package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

/**
 * 动画目标类别枚举。
 * <p>
 * 区分动画目标是 ui 控件还是 graphics 元素，由调用方按数组位置注入，
 * 不从 JSON 字段读取。
 */
public enum AnimationObjectClass
{
    /** ui 控件（对应 task.ui[] 数组） */
    UI(AnimationKey.Task.Object.CLASS_UI),

    /** graphics 元素（对应 task.graphics[] 数组） */
    GRAPHICS(AnimationKey.Task.Object.CLASS_GRAPHICS);

    private final String displayString;

    AnimationObjectClass (String displayString)
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
     * 从 JSON 字符串解析 AnimationObjectClass
     *
     * @param jsonValue class 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static AnimationObjectClass fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (AnimationObjectClass c : values())
        {
            if (c.displayString.equals(jsonValue)) return c;
        }
        return null;
    }
}
