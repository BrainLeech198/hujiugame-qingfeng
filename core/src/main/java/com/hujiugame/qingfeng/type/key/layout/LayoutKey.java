package com.hujiugame.qingfeng.type.key.layout;

public final class LayoutKey
{
    private LayoutKey()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ====================================================================================================
    // layout.json 顶层字段

    /** audio 音频节点 */
    public static final String AUDIO = "audio";

    /** 背景图片文件名（旧版顶层格式；已收编至 graphics 节，此常量保留用于回退兼容） */
    public static final String BACKGROUND_PICTURE = "background_picture";

    /** 装饰图片/动图容器 */
    public static final String GRAPHICS = "graphics";

    /** 页面名称 */
    public static final String NAME = "name";

    /** 模板布局名 */
    public static final String TEMPLATE = "template";

    /** UI 组件容器 */
    public static final String UI = "ui";

    // ====================================================================================================
    // layout.json 的 ui 节内部元素引用字段

    public static final class Ui
    {
        private Ui()
        {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        /** 元素引用的 kind 名 */
        public static final String KIND = "kind";

        /** 元素是否显示 */
        public static final String SHOW = "show";
    }
}
