package com.hujiugame.qingfeng.animation.task.object;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.type.key.ui.UiKey;
import com.hujiugame.qingfeng.ui.info.UiObject;
import com.hujiugame.qingfeng.ui.kind.InteractableObject;

/**
 * ui 控件类动画目标，持有 {@link UiObject} 定位目标。
 */
public class UiAnimationObject extends AnimationObject
{
    private final UiObject target;
    private InteractableObject interactableObject = null;

    private float startX, startY, targetX, targetY;
    private boolean hasStartPosition = false;

    public UiAnimationObject (UiObject target)
    {
        this.target = target;
        this.valid = target != null && target.getUiKind() != null;
        if (valid)
        {
            buildJson();
        }
    }

    public void setInteractableObject (InteractableObject interactableObject)
    {
        this.interactableObject = interactableObject;
    }

    public InteractableObject getInteractableObject ()
    {
        return interactableObject;
    }

    public void setStartPosition(float x, float y) {
        this.startX = x;
        this.startY = y;
        this.hasStartPosition = true;
    }

    public void setTargetPosition(float x, float y) {
        this.targetX = x;
        this.targetY = y;
    }

    public float getStartX() { return startX; }
    public float getStartY() { return startY; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public boolean hasStartPosition() { return hasStartPosition; }

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
     * 依据目标生成 object 节点 JSON（不含 class，由调用方按数组位置注入）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(UiKey.UiObject.TYPE, target.getUiKind().getDisplayString());
        json.put(UiKey.UiObject.TAG, target.getTag());
    }

    @Override
    public String toString ()
    {
        return "UiAnimationObject{target=" + target + '}';
    }
}
