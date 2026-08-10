package com.hujiugame.qingfeng.type.key;

public final class AnimationKey
{
    private AnimationKey ()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ====================================================================================================
    // Config 顶层字段

    public static final String ANIMATION_KEY = "animation";

    // ====================================================================================================
    // object 节点（动画目标定位）

    public static final class Target
    {
        private Target ()
        {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        /** 目标类别字段 */
        public static final String CLASS = "class";

        /** 目标类别值：ui 控件 */
        public static final String CLASS_UI = "ui";

        /** 目标类别值：graphics 元素 */
        public static final String CLASS_GRAPHICS = "graphics";
    }
}
