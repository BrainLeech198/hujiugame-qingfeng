package com.hujiugame.qingfeng.script.data.command.action;

import com.hujiugame.qingfeng.script.data.command.ScriptCommandType;
import com.hujiugame.qingfeng.type.key.script.ScriptKey;

/**
 * 具体指令枚举。
 * <p>
 * 对应 ScriptCommand JSON 中的 {@code command} 字段值（原名 action）。
 * 每个指令关联其所属的 {@link ScriptCommandType}。
 */
public enum ScriptCommandAction
{
    // ===== control =====

    IF(ScriptCommandType.CONTROL, ScriptKey.Script.Action.IF),
    WHILE(ScriptCommandType.CONTROL, ScriptKey.Script.Action.WHILE),
    BREAK(ScriptCommandType.CONTROL, ScriptKey.Script.Action.BREAK),
    CONTINUE(ScriptCommandType.CONTROL, ScriptKey.Script.Action.CONTINUE),
    RETURN(ScriptCommandType.CONTROL, ScriptKey.Script.Action.RETURN),
    WAIT(ScriptCommandType.CONTROL, ScriptKey.Script.Action.WAIT),
    CALL(ScriptCommandType.CONTROL, ScriptKey.Script.Action.CALL),

    // ===== variable =====

    CREATE(ScriptCommandType.VARIABLE, ScriptKey.Script.Action.CREATE),
    ASSIGNMENT(ScriptCommandType.VARIABLE, ScriptKey.Script.Action.ASSIGNMENT),

    // ===== story =====

    FORWARD_PAGE(ScriptCommandType.STORY, ScriptKey.Script.Action.FORWARD_PAGE),
    GOTO_PAGE(ScriptCommandType.STORY, ScriptKey.Script.Action.GOTO_PAGE);

    // ===== 字段 =====

    private final ScriptCommandType scriptCommandType;
    private final String displayString;

    // ===== 构造器 =====

    ScriptCommandAction (ScriptCommandType scriptCommandType, String displayString)
    {
        this.scriptCommandType = scriptCommandType;
        this.displayString = displayString;
    }

    // ===== Getter =====

    /**
     * 获取所属指令大类
     */
    public ScriptCommandType getCommandType ()
    {
        return scriptCommandType;
    }

    /**
     * 获取 JSON 中使用的字符串值
     */
    public String getDisplayString ()
    {
        return displayString;
    }

    // ===== 工厂方法 =====

    /**
     * 从 JSON 字符串解析 ScriptCommandAction
     *
     * @param displayString command 字段值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static ScriptCommandAction fromString (String displayString)
    {
        if (displayString == null) return null;
        for (ScriptCommandAction n : values())
        {
            if (n.displayString.equals(displayString)) return n;
        }
        return null;
    }
}
