package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * 动画目标基类。
 * <p>
 * 子类表示一类动画目标：{@link UiAnimationObject}（ui 控件）或 {@link GraphicsAnimationObject}（graphics 元素）。
 * 通过 {@link #fromJson(JsonEntity, AnimationObjectClass)} 按调用方传入的目标类别分发解析。
 * 构造风格与 {@code script.data.Script} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json。
 * <p>
 * 解析失败时 {@link #fromJson(JsonEntity, AnimationObjectClass)} 抛出 {@link IllegalArgumentException}（fail-fast），
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
     * 由调用方按数组位置（graphics[] / ui[]）传入目标类别，不再从 JSON 读取 class 字段。
     *
     * @param objectNode    包含 type / tag 字段的目标节点
     * @param objectClass   目标类别（UI / GRAPHICS），由调用方注入
     * @return 对应子类的动画目标
     * @throws IllegalArgumentException objectNode 非法、objectClass 未知或目标解析无效
     */
    public static AnimationObject fromJson (JsonEntity objectNode, AnimationObjectClass objectClass)
    {
        if (objectNode == null || !objectNode.isMap())
        {
            throw new IllegalArgumentException("AnimationObject.fromJson 需要 Map 数据 (json): " + objectNode);
        }
        if (objectClass == null)
        {
            throw new IllegalArgumentException("AnimationObject.fromJson 目标类别不能为 null (objectNode): " + objectNode);
        }

        AnimationObject object;
        switch (objectClass)
        {
            case UI:
                object = new UiAnimationObject(objectNode);
                break;
            case GRAPHICS:
                object = new GraphicsAnimationObject(objectNode);
                break;
            default:
                throw new IllegalArgumentException("AnimationObject.fromJson 没有对应的动画目标类别 (objectClass): " + objectClass + " (json): " + objectNode);
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
