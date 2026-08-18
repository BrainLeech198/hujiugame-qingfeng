package com.hujiugame.qingfeng.type.key.animation;

/**
 * 动画配置 JSON 键。
 * <p>
 * 嵌套类按实际文件/包层级组织，与 {@code animation/} 包结构一一对应：
 * {@code component} 包 → {@link Component}，{@code task} 包 → {@link Task}，
 * {@code task/object} 包 → {@link Task.Object}，{@code task/action} 包 → {@link Task.Action}，
 * {@code task/action/param} 包 → {@link Task.Action.Param}。
 */
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
    // component 包（animation/component）：FadeIn / FadeOut 组件节点

    public static final class Component
    {
        private Component ()
        {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        /** fade_in 切入动画节点 */
        public static final String FADE_IN = "fade_in";

        /** fade_out 切出动画节点 */
        public static final String FADE_OUT = "fade_out";

        // ====================================================================================================
        // fade_in 切入动画节点窗口键（FadeInObject）

        public static final class FadeIn
        {
            private FadeIn ()
            {
                throw new UnsupportedOperationException("Utility class cannot be instantiated");
            }

            /** 通用动画窗口键（default） */
            public static final String DEFAULT = "default";

            /** 来源页特例键前缀（from_page.<layoutDirName>） */
            public static final String FROM_PAGE = "from_page";

            /** 整段动画窗口时长（秒） */
            public static final String DURATION = "duration";

            /** 任务分组节点（含 synchronization/schedule） */
            public static final String TASK = "task";

            /** 同步任务组（全部同时启动） */
            public static final String SYNCHRONIZATION = "synchronization";

            /** 串行任务组（按列表顺序执行） */
            public static final String SCHEDULE = "schedule";
        }

        // ====================================================================================================
        // fade_out 切出动画节点窗口键（FadeOutObject）

        public static final class FadeOut
        {
            private FadeOut ()
            {
                throw new UnsupportedOperationException("Utility class cannot be instantiated");
            }

            /** 通用动画窗口键（default） */
            public static final String DEFAULT = "default";

            /** 来源页特例键前缀（from_page.<layoutDirName>） */
            public static final String FROM_PAGE = "from_page";

            /** 整段动画窗口时长（秒） */
            public static final String DURATION = "duration";

            /** 任务分组节点（含 synchronization/schedule） */
            public static final String TASK = "task";

            /** 同步任务组（全部同时启动） */
            public static final String SYNCHRONIZATION = "synchronization";

            /** 串行任务组（按列表顺序执行） */
            public static final String SCHEDULE = "schedule";
        }
    }

    // ====================================================================================================
    // task 包（animation/task）：AnimationTask 任务节点

    public static final class Task
    {
        private Task ()
        {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        /** 动画目标对象（AnimationObject） */
        public static final String OBJECT = "object";

        /** 动画动作（AnimationAction） */
        public static final String ACTION = "action";

        // ====================================================================================================
        // task/object 包（animation/task/object）：AnimationObject 目标节点

        public static final class Object
        {
            private Object ()
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

        // ====================================================================================================
        // task/action 包（animation/task/action）：AnimationAction 动作节点

        public static final class Action
        {
            private Action ()
            {
                throw new UnsupportedOperationException("Utility class cannot be instantiated");
            }


            /** 动作类型字段 */
            public static final String TYPE = "type";

            /** 相对前一个动作结束的延迟（秒） */
            public static final String DELAY = "delay";

            /** 动作时长（秒） */
            public static final String DURATION = "duration";

            /** 动作参数对象 */
            public static final String PARAM = "param";

            // ====================================================================================================
            // 动作 type 字段值（AnimationActionType）

            public static final class Type
            {
                private Type ()
                {
                    throw new UnsupportedOperationException("Utility class cannot be instantiated");
                }

                /** 保持不动 */
                public static final String NONE = "none";

                /** 平滑位移 */
                public static final String SMOOTH_MOVE = "smooth_move";
            }

            // ====================================================================================================
            // task/action/param 包（animation/task/action/param）：AnimationActionParam 参数节点

            public static final class Param
            {
                private Param ()
                {
                    throw new UnsupportedOperationException("Utility class cannot be instantiated");
                }

                /** 位移方向（JsonKey.Position.X/Y） */
                public static final String ORIENTATION = "orientation";

                /** 位移量（相对原位置的移动距离） */
                public static final String SPEED = "speed";

                /** 动作时长（秒） */
                public static final String DURATION = "duration";
            }
        }
    }
}
