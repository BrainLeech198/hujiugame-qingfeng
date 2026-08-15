package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
import com.hujiugame.qingfeng.type.key.UiKey;
import com.hujiugame.qingfeng.ui.info.UiObject;

/**
 * ui 控件类动画目标，持有 {@link UiObject} 定位目标。
 */
public class UiAnimationObject extends AnimationObject
{
    private final UiObject target;

    public UiAnimationObject (UiObject target)
    {
        this.target = target;
        this.valid = target != null && target.getUiKind() != null;
        if (valid)
        {
            buildJson();
        }
    }

    /**
     * 从 object 节点（class=ui）解析 UiObject 目标构造。
     *
     * @param objectNode 包含 type + tag 字段的目标节点
     */
    public UiAnimationObject (JsonEntity objectNode)
    {
        this.target = new UiObject(objectNode);
        this.valid = target.getUiKind() != null;
        this.json = objectNode;
    }

    /**
     * 获取 ui 控件目标
     */
    public UiObject getTarget ()
    {
        return target;
    }

    /**
     * 依据目标生成 object 节点 JSON
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Task.Object.CLASS, AnimationKey.Task.Object.CLASS_UI);
        json.put(UiKey.UiObject.TYPE, target.getUiKind().getDisplayString());
        json.put(UiKey.UiObject.TAG, target.getTag());
    }

    @Override
    public String toString ()
    {
        return "UiAnimationObject{target=" + target + '}';
    }
}
