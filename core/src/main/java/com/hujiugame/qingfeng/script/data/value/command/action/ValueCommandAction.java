package com.hujiugame.qingfeng.script.data.value.command.action;

import com.hujiugame.qingfeng.script.data.value.command.ValueCommandType;
import com.hujiugame.qingfeng.type.key.ScriptKey;

public enum ValueCommandAction
{
    // 数学运算指令 MathValueCommand
    ADD(ValueCommandType.MATH, ScriptKey.Command.Action.ADD),
    SUB(ValueCommandType.MATH, ScriptKey.Command.Action.SUB),
    MUL(ValueCommandType.MATH, ScriptKey.Command.Action.MUL),
    DIV(ValueCommandType.MATH, ScriptKey.Command.Action.DIV),
    NEG(ValueCommandType.MATH, ScriptKey.Command.Action.NEG),
    RANDOM(ValueCommandType.MATH, ScriptKey.Command.Action.RANDOM),

    // 比较运算指令 CompareValueCommand
    EQUAL(ValueCommandType.COMPARE, ScriptKey.Command.Action.EQUAL),
    NOT_EQUAL(ValueCommandType.COMPARE, ScriptKey.Command.Action.NOT_EQUAL),
    GREATER(ValueCommandType.COMPARE, ScriptKey.Command.Action.GREATER),
    LESS(ValueCommandType.COMPARE, ScriptKey.Command.Action.LESS),
    GREATER_EQUAL(ValueCommandType.COMPARE, ScriptKey.Command.Action.GREATER_EQUAL),
    LESS_EQUAL(ValueCommandType.COMPARE, ScriptKey.Command.Action.LESS_EQUAL),

    // 标准逻辑运算指令 LogicValueCommand
    AND(ValueCommandType.LOGIC, ScriptKey.Command.Action.AND),
    OR(ValueCommandType.LOGIC, ScriptKey.Command.Action.OR),
    NOT(ValueCommandType.LOGIC, ScriptKey.Command.Action.NOT),

    // 原子值指令 AtomicValueCommand
    CONST(ValueCommandType.ATOMIC, ScriptKey.Command.Action.CONST),
    VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Command.Action.VARIABLE),
    SCOPE_VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Command.Action.SCOPE_VARIABLE),
    GAME_VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Command.Action.GAME_VARIABLE),
    TRUE(ValueCommandType.ATOMIC, ScriptKey.Command.Action.TRUE),
    FALSE(ValueCommandType.ATOMIC, ScriptKey.Command.Action.FALSE),
    CALL(ValueCommandType.ATOMIC, ScriptKey.Command.Action.CALL);

    private final ValueCommandType valueCommandType;
    private final String displayString;

    // ===== 构造器 =====

    ValueCommandAction (ValueCommandType valueCommandType, String displayString)
    {
        this.valueCommandType = valueCommandType;
        this.displayString = displayString;
    }

    // ===== Getter =====

    /**
     * 获取所属指令大类
     */
    public ValueCommandType getCommandType ()
    {
        return valueCommandType;
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
    public static ValueCommandAction fromString (String displayString)
    {
        if (displayString == null) return null;
        for (ValueCommandAction n : ValueCommandAction.values())
        {
            if (n.displayString.equals(displayString)) return n;
        }
        return null;
    }
}
