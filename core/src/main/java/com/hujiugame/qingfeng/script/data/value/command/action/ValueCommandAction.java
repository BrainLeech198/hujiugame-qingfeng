package com.hujiugame.qingfeng.script.data.value.command.action;

import com.hujiugame.qingfeng.script.data.value.command.ValueCommandType;
import com.hujiugame.qingfeng.type.key.ScriptKey;

public enum ValueCommandAction
{
    // 数学运算指令 MathValueCommand
    ADD(ValueCommandType.MATH, ScriptKey.Value.Action.ADD),
    SUB(ValueCommandType.MATH, ScriptKey.Value.Action.SUB),
    MUL(ValueCommandType.MATH, ScriptKey.Value.Action.MUL),
    DIV(ValueCommandType.MATH, ScriptKey.Value.Action.DIV),
    NEG(ValueCommandType.MATH, ScriptKey.Value.Action.NEG),
    RANDOM(ValueCommandType.MATH, ScriptKey.Value.Action.RANDOM),

    // 比较运算指令 CompareValueCommand
    EQUAL(ValueCommandType.COMPARE, ScriptKey.Value.Action.EQUAL),
    NOT_EQUAL(ValueCommandType.COMPARE, ScriptKey.Value.Action.NOT_EQUAL),
    GREATER(ValueCommandType.COMPARE, ScriptKey.Value.Action.GREATER),
    LESS(ValueCommandType.COMPARE, ScriptKey.Value.Action.LESS),
    GREATER_EQUAL(ValueCommandType.COMPARE, ScriptKey.Value.Action.GREATER_EQUAL),
    LESS_EQUAL(ValueCommandType.COMPARE, ScriptKey.Value.Action.LESS_EQUAL),

    // 标准逻辑运算指令 LogicValueCommand
    AND(ValueCommandType.LOGIC, ScriptKey.Value.Action.AND),
    OR(ValueCommandType.LOGIC, ScriptKey.Value.Action.OR),
    NOT(ValueCommandType.LOGIC, ScriptKey.Value.Action.NOT),

    // 原子值指令 AtomicValueCommand
    CONST(ValueCommandType.ATOMIC, ScriptKey.Value.Action.CONST),
    VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Value.Action.VARIABLE),
    SCOPE_VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Value.Action.SCOPE_VARIABLE),
    GAME_VARIABLE(ValueCommandType.ATOMIC, ScriptKey.Value.Action.GAME_VARIABLE),
    TRUE(ValueCommandType.ATOMIC, ScriptKey.Value.Action.TRUE),
    FALSE(ValueCommandType.ATOMIC, ScriptKey.Value.Action.FALSE),
    CALL(ValueCommandType.ATOMIC, ScriptKey.Value.Action.CALL);

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
