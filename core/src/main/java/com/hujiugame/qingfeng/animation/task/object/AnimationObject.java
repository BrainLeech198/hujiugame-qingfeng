package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;

/**
 * 动画目标基类。
 * <p>
 * 子类表示一类动画目标：{@link UiAnimationObject}（ui 控件）或 {@link GraphicsAnimationObject}（graphics 元素）。
 * 通过 {@link #fromJson(JsonEntity)} 按 object 节点的 class 字段分发解析。
 * 构造风格与 {@code script.data.Script} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json。
 * <p>
 * 解析失败时 {@link #fromJson(JsonEntity)} 抛出 {@link IllegalArgumentException}（fail-fast），
 * 由调用方（动画任务加载点）try-catch 降级跳过，符合"内层 throw、边界兜底"的错误处理策略。
 */
public abstract class AnimationObject
{
    /** 解析/构造是否成功 */
    protected boolean valid;

    /** 构造来源 JSON（字段构造时由 buildJson 生成） */
    protected JsonEntity json;

    /**
     * 从 object 节点解析动画目标。
     * <p>
     * 解析失败（非 Map 数据、class 未知、目标无效）时抛出 {@link IllegalArgumentException}，
     * 消息携带完整 json，便于调用方捕获后记录与降级。
     *
     * @param objectNode 包含 class/type/tag 字段的目标节点
     * @return 对应子类的动画目标
     * @throws IllegalArgumentException objectNode 非法、class 未知或目标解析无效
     */
    public static AnimationObject fromJson (JsonEntity objectNode)
    {
        if (objectNode == null || !objectNode.isMap())
        {
            throw new IllegalArgumentException("AnimationObject.fromJson 需要 Map 数据 (json): " + objectNode);
        }

        String targetClass = objectNode.getString(AnimationKey.Task.Object.CLASS);
        AnimationObject object;
        if (AnimationKey.Task.Object.CLASS_UI.equals(targetClass))
        {
            object = new UiAnimationObject(objectNode);
        }
        else if (AnimationKey.Task.Object.CLASS_GRAPHICS.equals(targetClass))
        {
            object = new GraphicsAnimationObject(objectNode);
        }
        else
        {
            throw new IllegalArgumentException("AnimationObject.fromJson 没有对应的动画目标类别 (class): " + targetClass + " (json): " + objectNode);
        }

        if (!object.isValid())
        {
            throw new IllegalArgumentException("AnimationObject.fromJson 解析动画目标无效 (object): " + object + " (json): " + objectNode);
        }
        return object;
    }

    /**
     * 获取解析/构造是否成功
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取构造来源 JSON（字段构造时为 buildJson 生成的 JSON）
     */
    public JsonEntity getJson ()
    {
        return json;
    }
}
