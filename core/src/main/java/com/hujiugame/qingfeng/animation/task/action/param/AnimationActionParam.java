package com.hujiugame.qingfeng.animation.task.action.param;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * 动画动作参数接口。
 * <p>
 * 对应动画 action 节点中的 {@code param} 字段，每种 {@link com.hujiugame.qingfeng.animation.task.action.AnimationActionType}
 * 有其对应实现类。
 */
public interface AnimationActionParam
{
    boolean isValid ();

    JsonEntity getJson ();
}
