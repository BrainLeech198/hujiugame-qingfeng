package com.hujiugame.qingfeng.type.key.layout;

/**
 * audio 分类下的子字段常量，对应 layout.json → audio 内部的背景音乐列表与音乐映射字段
 */
public final class AudioKey
{
    private AudioKey ()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** audio → background_music 背景音乐列表（支持单条字符串或数组） */
    public static final String BACKGROUND_MUSIC = "background_music";

    /** audio → music 音乐映射（tag → 文件名） */
    public static final String MUSIC = "music";
}
