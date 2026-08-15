package com.hujiugame.qingfeng.animation.task.action.param;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * none 动作参数。
 * <p>
 * 无实际内容，保持不动，仅为对齐 {@link com.hujiugame.qingfeng.animation.task.action.AnimationActionType}
 * 与 {@link AnimationActionParam} 的一一对应结构。
 */
public class NoneAnimationActionParam implements AnimationActionParam
{
    private boolean valid;
    private JsonEntity json;

    private void buildJson ()
    {
        if (!valid)
        {
            throw new IllegalStateException("An invalid animation action parameter cannot be built.");
        }
        json = new JsonEntity();
    }

    public NoneAnimationActionParam ()
    {
        valid = true;
        buildJson();
    }

    public NoneAnimationActionParam (JsonEntity json)
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
        return "NoneAnimationActionParam{" +
            "valid=" + valid +
            ", json=" + json +
            '}';
    }
}
