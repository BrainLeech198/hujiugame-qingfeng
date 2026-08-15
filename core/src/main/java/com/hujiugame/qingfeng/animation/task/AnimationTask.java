package com.hujiugame.qingfeng.animation.task;

import com.hujiugame.qingfeng.animation.task.action.AnimationAction;
import com.hujiugame.qingfeng.animation.task.action.AnimationActionParser;
import com.hujiugame.qingfeng.animation.task.object.AnimationObject;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 动画任务信封。
 * <p>
 * 对应 config 的 animation 节点下 synchronization / schedule 列表中的单个任务对象
 * {@code {object, action}}，持有动画目标与动画动作。
 * 构造风格与 {@link AnimationObject} / {@link AnimationAction} 一致：字段构造 + JsonEntity 构造双构造器，
 * 携带 valid 与 json；解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该任务。
 */
public final class AnimationTask
{
    private boolean valid;
    private final AnimationObject animationObject;
    private final AnimationAction animationAction;
    private JsonEntity json;

    // ==============================================================================

    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Task.OBJECT, animationObject.getJson());
        json.put(AnimationKey.Task.ACTION, animationAction.getJson());
    }

    public AnimationTask (AnimationObject animationObject, AnimationAction animationAction)
    {
        this.animationObject = animationObject;
        this.animationAction = animationAction;
        if (animationObject == null || !animationObject.isValid() || animationAction == null || !animationAction.isValid())
        {
            LogUtils.error(AnimationTask.class, "构造失败 动画目标或动画动作无效 (animationObject): " + animationObject + " (animationAction): " + animationAction);
            valid = false;
            return;
        }
        valid = true;
        buildJson();
    }

    public AnimationTask (JsonEntity json)
    {
        if (json.isMap() && json.containsKey(AnimationKey.Task.OBJECT) && json.containsKey(AnimationKey.Task.ACTION))
        {
            // 解析动画目标（AnimationObject.fromJson 为 fail-fast，捕获异常后降级为无效任务）
            AnimationObject object = null;
            try
            {
                object = AnimationObject.fromJson(json.getJsonEntityByKey(AnimationKey.Task.OBJECT));
            }
            catch (IllegalArgumentException e)
            {
                LogUtils.error(AnimationTask.class, "解析失败 动画目标异常 (json): " + json + " (message): " + e.getMessage());
            }
            if (object == null)
            {
                this.animationObject = null;
                this.animationAction = null;
                valid = false;
                return;
            }
            // 解析动画动作（AnimationActionParser.parse 为 fail-soft，返回 null）
            AnimationAction action = AnimationActionParser.parse(json.getJsonEntityByKey(AnimationKey.Task.ACTION));
            if (action == null)
            {
                this.animationObject = object;
                this.animationAction = null;
                valid = false;
                return;
            }
            this.animationObject = object;
            this.animationAction = action;
            this.json = json;
            valid = true;
        }
        else
        {
            LogUtils.error(AnimationTask.class, "解析失败 需要包含 " + AnimationKey.Task.OBJECT + " 与 " + AnimationKey.Task.ACTION + " 字段的 Map 数据 (json): " + json);
            this.animationObject = null;
            this.animationAction = null;
            valid = false;
        }
    }

    /**
     * 任务是否有效
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取动画目标
     */
    public AnimationObject getAnimationObject ()
    {
        return animationObject;
    }

    /**
     * 获取动画动作
     */
    public AnimationAction getAnimationAction ()
    {
        return animationAction;
    }

    public JsonEntity getJson ()
    {
        return json;
    }

    @Override
    public String toString ()
    {
        return "AnimationTask{" +
            "valid=" + valid +
            ", animationObject=" + animationObject +
            ", animationAction=" + animationAction +
            ", json=" + json +
            '}';
    }
}
