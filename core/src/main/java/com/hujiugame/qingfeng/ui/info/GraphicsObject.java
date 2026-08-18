package com.hujiugame.qingfeng.ui.info;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.ui.UiKey;
import com.hujiugame.qingfeng.type.ui.GraphicsKind;

/**
 * graphics 元素标识数据类，以"类别 + tag"唯一定位一个 graphics 元素。
 */
public class GraphicsObject
{
    /** graphics 元素类别 */
    private final GraphicsKind graphicsKind;

    /** graphics 元素标签 */
    private final String tag;

    public GraphicsObject (GraphicsKind graphicsKind, String tag)
    {
        this.graphicsKind = graphicsKind;
        this.tag = tag;
    }

    /**
     * 从 JsonEntity 解析 graphics 元素标识构造。
     * <p>
     * 读取 type 字段解析 GraphicsKind、tag 字段作为标签；字段缺失或解析失败时对应字段为 null，由调用方校验。
     *
     * @param jsonEntity 包含 type + tag 字段的 JSON 数据
     */
    public GraphicsObject (JsonEntity jsonEntity)
    {
        this.graphicsKind = GraphicsKind.fromString(jsonEntity.getString(UiKey.UiObject.TYPE));
        this.tag = jsonEntity.getString(UiKey.UiObject.TAG);
    }

    /**
     * 获取 graphics 元素类别
     */
    public GraphicsKind getGraphicsKind ()
    {
        return graphicsKind;
    }

    /**
     * 获取 graphics 元素标签
     */
    public String getTag ()
    {
        return tag;
    }

    @Override
    public String toString ()
    {
        return "GraphicsObject{" +
            "graphicsKind=" + graphicsKind +
            ", tag='" + tag + '\'' +
            '}';
    }
}
