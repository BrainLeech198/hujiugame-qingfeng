package com.hujiugame.qingfeng.animation.task.command;

import com.hujiugame.qingfeng.animation.task.command.param.AnimationCommandParam;
import com.hujiugame.qingfeng.animation.task.command.param.NoneAnimationCommandParam;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 动画指令参数解析器。
 * <p>
 * 按 {@link AnimationCommandAction} 分派解析对应的 {@link AnimationCommandParam} 实现。
 */
public class AnimationCommandParser
{
    /**
     * 按具体动作解析参数
     *
     * @param commandAction 具体动作类型
     * @param paramJson     参数 JSON 节点
     * @return 对应的参数实现，失败返回 null
     */
    public static AnimationCommandParam parseParam (AnimationCommandAction commandAction, JsonEntity paramJson)
    {
        if (commandAction == null)
        {
            LogUtils.error(AnimationCommandParser.class, "parseParam 具体动作为空");
            return null;
        }

        switch (commandAction)
        {
            case NONE:
                return new NoneAnimationCommandParam(paramJson);

            default:
                LogUtils.error(AnimationCommandParser.class, "parseParam 没有对应的具体动作 (commandAction): " + commandAction);
                return null;
        }
    }
}
