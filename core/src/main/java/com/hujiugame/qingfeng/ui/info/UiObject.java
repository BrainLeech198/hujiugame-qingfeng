package com.hujiugame.qingfeng.ui.info;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.UiKey;
import com.hujiugame.qingfeng.type.ui.UiKind;

/**
 * UI 对象标识数据类，以"类型 + tag"唯一定位一个 UI 组件。
 */
public class UiObject
{
    /** UI 组件类型 */
    private final UiKind uiKind;

    /** UI 组件标签 */
    private final String tag;

    public UiObject (UiKind uiKind, String tag)
    {
        this.uiKind = uiKind;
        this.tag = tag;
    }

    /**
     * 从 JsonEntity 解析 UI 对象标识构造。
     * <p>
     * 读取 type 字段解析 UiKind、tag 字段作为标签；字段缺失或解析失败时对应字段为 null，由调用方校验。
     *
     * @param jsonEntity 包含 type + tag 字段的 JSON 数据
     */
    public UiObject (JsonEntity jsonEntity)
    {
        this.uiKind = UiKind.fromString(jsonEntity.getString(UiKey.UiObject.TYPE));
        this.tag = jsonEntity.getString(UiKey.UiObject.TAG);
    }

    /**
     * 获取 UI 组件类型
     */
    public UiKind getUiKind ()
    {
        return uiKind;
    }

    /**
     * 获取 UI 组件标签
     */
    public String getTag ()
    {
        return tag;
    }

    @Override
    public String toString ()
    {
        return "UiObject{" +
            "uiKind=" + uiKind +
            ", tag='" + tag + '\'' +
            '}';
    }
}
