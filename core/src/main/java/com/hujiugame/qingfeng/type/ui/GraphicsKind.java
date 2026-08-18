package com.hujiugame.qingfeng.type.ui;

import com.hujiugame.qingfeng.type.key.layout.GraphicsKey;

/**
 * graphics 元素类型枚举。
 * <p>
 * 对应 layout.json 的 graphics 节子分类（backgroundPicture/picture/gif）。
 * 字符串值唯一来源为 {@link GraphicsKey} 各分类常量。
 */
public enum GraphicsKind
{
    BACKGROUND_PICTURE(GraphicsKey.BACKGROUND_PICTURE),
    PICTURE(GraphicsKey.PICTURE),
    GIF(GraphicsKey.GIF);

    private final String displayString;

    GraphicsKind (String displayString)
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
     * 从 JSON 字符串解析 GraphicsKind
     *
     * @param jsonValue 分类 key 值
     * @return 对应的枚举，不匹配时返回 null
     */
    public static GraphicsKind fromString (String jsonValue)
    {
        if (jsonValue == null) return null;
        for (GraphicsKind t : values())
        {
            if (t.displayString.equals(jsonValue)) return t;
        }
        return null;
    }
}
