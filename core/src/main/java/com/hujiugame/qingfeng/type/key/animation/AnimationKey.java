package com.hujiugame.qingfeng.type.key.animation;

/**
 * 动画配置 JSON 键。
 * <p>
 * 嵌套类按实际文件/包层级组织，与 {@code animation/} 包结构一一对应：
 * {@code component} 包 → {@link Component}，{@code task} 包 → {@link Task}，
 * {@code task/object} 包 → {@link Task.Object}，{@code task/command} 包 → {@link Task.Command}，
 * {@code task/command/param} 包 → {@link Task.Command.Param}。
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

            /** 来源页特例键（from_page.<game_page_id>） */
            public static final String FROM_PAGE = "from_page";

            /** 整段动画窗口时长（秒） */
            public static final String DURATION = "duration";

            /** 任务分组节点（含 graphics / ui） */
            public static final String TASK = "task";

            /** graphics 元素任务数组 */
            public static final String GRAPHICS = "graphics";

            /** ui 控件任务数组 */
            public static final String UI = "ui";
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

            /** 来源页特例键（from_page.<game_page_id>） */
            public static final String FROM_PAGE = "from_page";

            /** 整段动画窗口时长（秒） */
            public static final String DURATION = "duration";

            /** 任务分组节点（含 graphics / ui） */
            public static final String TASK = "task";

            /** graphics 元素任务数组 */
            public static final String GRAPHICS = "graphics";

            /** ui 控件任务数组 */
            public static final String UI = "ui";
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

        /** 动画指令（AnimationCommand） */
        public static final String COMMAND = "command";

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
        // task/command 包（animation/task/command）：AnimationCommand 指令节点

        public static final class Command
        {
            private Command ()
            {
                throw new UnsupportedOperationException("Utility class cannot be instantiated");
            }

            /** 动作起始时间（秒，相对窗口起点） */
            public static final String START_TIME = "start_time";

            /** 动作结束时间（秒，相对窗口起点） */
            public static final String END_TIME = "end_time";

            /** 动作时长（秒） */
            public static final String DURATION = "duration";

            /** 指令大类（AnimationCommandType） */
            public static final String TYPE = "type";

            /** 具体动作（AnimationCommandAction） */
            public static final String ACTION = "action";

            /** 动作参数对象 */
            public static final String PARAM = "param";

            // ====================================================================================================
            // 指令大类 type 字段值（AnimationCommandType）

            public static final class Type
            {
                private Type ()
                {
                    throw new UnsupportedOperationException("Utility class cannot be instantiated");
                }

                /** 普通动画 */
                public static final String NORMAL = "normal";
            }

            // ====================================================================================================
            // 具体动作 action 字段值（AnimationCommandAction）

            public static final class Action
            {
                private Action ()
                {
                    throw new UnsupportedOperationException("Utility class cannot be instantiated");
                }

                /** 保持不动 */
                public static final String NONE = "none";
            }

            // ====================================================================================================
            // task/command/param 包（animation/task/command/param）：AnimationCommandParam 参数节点

            public static final class Param
            {
                private Param ()
                {
                    throw new UnsupportedOperationException("Utility class cannot be instantiated");
                }
            }
        }
    }
}
