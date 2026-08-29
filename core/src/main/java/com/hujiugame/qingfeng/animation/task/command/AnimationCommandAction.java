package com.hujiugame.qingfeng.animation.task.command;

import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

/**
 * 动画指令具体动作枚举。
 * <p>
 * 对应动画 command 节点中的 {@code action} 字段值，
 * 在大类（{@link AnimationCommandType}）之下进一步区分具体行为。
 * <p>
 * 对标 Script 的 {@link com.hujiugame.qingfeng.script.data.command.action.ScriptCommandAction}。
 */
public enum AnimationCommandAction
{
    /** 保持不动 */
    NONE(AnimationKey.Task.Command.Action.NONE);

    private final String displayString;

    AnimationCommandAction (String displayString)
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
     * 从 JSON 字符串解析 AnimationCommandAction
     *
     * @param jsonValue action 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static AnimationCommandAction fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (AnimationCommandAction a : values())
        {
            if (a.displayString.equals(jsonValue)) return a;
        }
        return null;
    }
}
