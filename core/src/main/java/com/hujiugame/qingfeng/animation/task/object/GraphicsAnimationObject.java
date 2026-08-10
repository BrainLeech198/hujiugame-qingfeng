package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
import com.hujiugame.qingfeng.type.key.UiKey;
import com.hujiugame.qingfeng.ui.info.GraphicsObject;

/**
 * graphics 元素类动画目标，持有 {@link GraphicsObject} 定位目标。
 */
public class GraphicsAnimationObject extends AnimationObject
{
    private final GraphicsObject target;

    public GraphicsAnimationObject (GraphicsObject target)
    {
        this.target = target;
        this.valid = target != null && target.getGraphicsKind() != null;
        if (valid)
        {
            buildJson();
        }
    }

    /**
     * 从 object 节点（class=graphics）解析 GraphicsObject 目标构造。
     *
     * @param objectNode 包含 type + tag 字段的目标节点
     */
    public GraphicsAnimationObject (JsonEntity objectNode)
    {
        this.target = new GraphicsObject(objectNode);
        this.valid = target != null && target.getGraphicsKind() != null;
        this.json = objectNode;
    }

    /**
     * 获取 graphics 元素目标
     */
    public GraphicsObject getTarget ()
    {
        return target;
    }

    /**
     * 依据目标生成 object 节点 JSON
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Target.CLASS, AnimationKey.Target.CLASS_GRAPHICS);
        json.put(UiKey.UiObject.TYPE, target.getGraphicsKind().getDisplayString());
        json.put(UiKey.UiObject.TAG, target.getTag());
    }

    @Override
    public String toString ()
    {
        return "GraphicsAnimationObject{target=" + target + '}';
    }
}
