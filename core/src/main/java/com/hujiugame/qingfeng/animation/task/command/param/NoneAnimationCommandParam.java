package com.hujiugame.qingfeng.animation.task.command.param;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * none 指令参数。
 * <p>
 * 无实际内容，保持不动，仅为对齐 {@link com.hujiugame.qingfeng.animation.task.command.AnimationCommandAction}
 * 与 {@link AnimationCommandParam} 的一一对应结构。
 */
public class NoneAnimationCommandParam implements AnimationCommandParam
{
    private boolean valid;
    private JsonEntity json;

    private void buildJson ()
    {
        if (!valid)
        {
            throw new IllegalStateException("An invalid animation command parameter cannot be built.");
        }
        json = new JsonEntity();
    }

    public NoneAnimationCommandParam ()
    {
        valid = true;
        buildJson();
    }

    public NoneAnimationCommandParam (JsonEntity json)
    {
        valid = true;
        this.json = new JsonEntity();
    }

    @Override
    public boolean isValid ()
    {
        return valid;
    }

    @Override
    public JsonEntity getJson ()
    {
        return json;
    }

    @Override
    public String toString ()
    {
        return "NoneAnimationCommandParam{" +
            "valid=" + valid +
            ", json=" + json +
            '}';
    }
}
