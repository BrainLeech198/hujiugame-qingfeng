package com.hujiugame.qingfeng.type.ui;

import com.hujiugame.qingfeng.type.key.UiKey;

/**
 * UI 组件类型枚举。
 * <p>
 * 对应 layout.json 的 ui 节第一层 key，也是 ui_config.json 的组件类型 key。
 * 字符串值唯一来源为 {@link UiKey} 各组件 KEY 常量。
 */
public enum UiKind
{
    BUTTON(UiKey.Button.KEY),
    LABEL(UiKey.Label.KEY),
    IMAGE(UiKey.Image.KEY),
    FONT(UiKey.Font.KEY),
    MESSAGE_BOX(UiKey.MessageBox.KEY);

    private final String displayString;

    UiKind (String displayString)
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
     * 从 JSON 字符串解析 UiKind
     *
     * @param jsonValue 组件类型 key 值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static UiKind fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (UiKind t : values())
        {
            if (t.displayString.equals(jsonValue)) return t;
        }
        return null;
    }
}
