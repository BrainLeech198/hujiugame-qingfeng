package com.hujiugame.qingfeng.script.data.trigger.command;

import com.hujiugame.qingfeng.type.key.script.ScriptKey;

public enum TriggerType
{
    IMAGE(ScriptKey.Trigger.Type.IMAGE),
    LABEL(ScriptKey.Trigger.Type.LABEL),
    BUTTON(ScriptKey.Trigger.Type.BUTTON);

    private final String displayString;

    TriggerType (String displayString)
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
     * 从 JSON 字符串解析 ScriptCommandType
     *
     * @param jsonValue type 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static TriggerType fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (TriggerType t : TriggerType.values())
        {
            if (t.displayString.equals(jsonValue)) return t;
        }
        return null;
    }
}
