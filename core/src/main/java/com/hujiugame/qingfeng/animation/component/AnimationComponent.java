package com.hujiugame.qingfeng.animation.component;

import com.hujiugame.qingfeng.data.JsonEntity;

/**
 * 动画组件接口。
 * <p>
 * 动画组件（FadeIn / FadeOut）统一暴露有效性判断与构造来源 JSON，
 * 供 {@code Animation} 容器无差别收集与序列化（json 化双向读写）。
 */
public interface AnimationComponent
{
    /**
     * 组件是否有效
     */
    boolean isValid ();

    /**
     * 获取构造来源 JSON（字段构造时为 buildJson 生成的 JSON）
     */
    JsonEntity getJson ();
}
