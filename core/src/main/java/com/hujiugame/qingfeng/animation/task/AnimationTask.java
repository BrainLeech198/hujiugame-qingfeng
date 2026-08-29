package com.hujiugame.qingfeng.animation.task;

import com.hujiugame.qingfeng.animation.task.command.AnimationCommand;
import com.hujiugame.qingfeng.animation.task.object.AnimationObject;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 动画任务信封。
 * <p>
 * 对应 config 的 animation 节下 graphics[] / ui[] 数组中的单个任务对象
 * {@code {object, command}}，持有动画目标与动画指令。
 * 构造风格与 {@link AnimationObject} / {@link AnimationCommand} 一致：字段构造 + JsonEntity 构造双构造器，
 * 携带 valid 与 json；解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该任务。
 */
public final class AnimationTask
{
    private boolean valid;
    private final AnimationObject animationObject;
    private final AnimationCommand animationCommand;
    private JsonEntity json;

    // ==============================================================================

    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Task.OBJECT, animationObject.getJson());
        json.put(AnimationKey.Task.COMMAND, animationCommand.getJson());
    }

    /**
     * 字段构造
     *
     * @param animationObject  动画目标
     * @param animationCommand 动画指令
     */
    public AnimationTask (AnimationObject animationObject, AnimationCommand animationCommand)
    {
        this.animationObject = animationObject;
        this.animationCommand = animationCommand;
        if (animationObject == null || !animationObject.isValid() || animationCommand == null || !animationCommand.isValid())
        {
            LogUtils.error(AnimationTask.class, "构造失败 动画目标或动画指令无效 (animationObject): " + animationObject + " (animationCommand): " + animationCommand);
            valid = false;
            return;
        }
        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析动画任务节点
     *
     * @param json         包含 object 和 command 字段的 Map 数据
     * @param objectClass  目标类别（UI / GRAPHICS），由调用方按数组位置注入
     */
    public AnimationTask (JsonEntity json, com.hujiugame.qingfeng.animation.task.object.AnimationObjectClass objectClass)
    {
        if (json != null && json.isMap() && json.containsKey(AnimationKey.Task.OBJECT) && json.containsKey(AnimationKey.Task.COMMAND))
        {
            // 解析动画目标（AnimationObject.fromJson 为 fail-fast，捕获异常后降级为无效任务）
            AnimationObject object = null;
            try
            {
                object = AnimationObject.fromJson(json.getJsonEntityByKey(AnimationKey.Task.OBJECT), objectClass);
            }
            catch (IllegalArgumentException e)
            {
                LogUtils.error(AnimationTask.class, "解析失败 动画目标异常 (json): " + json + " (message): " + e.getMessage());
            }
            if (object == null)
            {
                this.animationObject = null;
                this.animationCommand = null;
                valid = false;
                return;
            }
            // 解析动画指令（AnimationCommand 构造为 fail-soft，标记 valid=false）
            AnimationCommand command = new AnimationCommand(json.getJsonEntityByKey(AnimationKey.Task.COMMAND));
            if (!command.isValid())
            {
                this.animationObject = object;
                this.animationCommand = null;
                valid = false;
                return;
            }
            this.animationObject = object;
            this.animationCommand = command;
            this.json = json;
            valid = true;
            LogUtils.debug(AnimationTask.class, "AnimationTask(JsonEntity) 解析动画任务成功 (object): " + object + " (command): " + command);
        }
        else
        {
            LogUtils.error(AnimationTask.class, "解析失败 需要包含 " + AnimationKey.Task.OBJECT + " 与 " + AnimationKey.Task.COMMAND + " 字段的 Map 数据 (json): " + json);
            this.animationObject = null;
            this.animationCommand = null;
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
     * 获取动画指令
     */
    public AnimationCommand getAnimationCommand ()
    {
        return animationCommand;
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
            ", animationCommand=" + animationCommand +
            ", json=" + json +
            '}';
    }
}
