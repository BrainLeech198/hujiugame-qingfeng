package com.hujiugame.qingfeng.animation.component;

import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

/**
 * 动画组件类型枚举
 * <p>
 * 对应 {@code animation} 节点下的组件类型（fade_in / fade_out），
 * 每个常量引用 {@link AnimationKey.Component} 的组件节点键建立联动，
 * 供 {@code Animation} 容器按类型存储与序列化组件：
 * {@link #getKey()} 取组件节点键，{@link #fromKey(String)} 将节点键反查为组件类型。
 */
public enum AnimationComponentType
{
    FADE_IN(AnimationKey.Component.FADE_IN),
    FADE_OUT(AnimationKey.Component.FADE_OUT);

    /** 对应的组件节点键 */
    private final String key;

    /**
     * 构造动画组件类型枚举常量
     *
     * @param key 组件节点键
     */
    AnimationComponentType (String key)
    {
        this.key = key;
    }

    /**
     * 获取对应的组件节点键
     *
     * @return 组件节点键
     */
    public String getKey ()
    {
        return key;
    }

    /**
     * 将组件节点键转换为动画组件类型
     *
     * @param key 组件节点键
     * @return 对应的动画组件类型；未知键或 null 返回 null
     */
    public static AnimationComponentType fromKey (String key)
    {
        if (key == null)
        {
            return null;
        }
        for (AnimationComponentType type : values())
        {
            if (type.key.equals(key))
            {
                return type;
            }
        }
        return null;
    }
}
