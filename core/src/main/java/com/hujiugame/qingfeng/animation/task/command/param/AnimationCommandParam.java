package com.hujiugame.qingfeng.animation.task.command.param;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * 动画指令参数接口。
 * <p>
 * 对应动画 command 节点中的 {@code param} 字段，每种 {@link com.hujiugame.qingfeng.animation.task.command.AnimationCommandAction}
 * 有其对应实现类。
 */
public interface AnimationCommandParam
{
    boolean isValid ();

    JsonEntity getJson ();
}
