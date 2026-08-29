package com.hujiugame.qingfeng.animation;

import com.hujiugame.qingfeng.animation.component.AnimationComponent;
import com.hujiugame.qingfeng.animation.component.AnimationComponentType;
import com.hujiugame.qingfeng.animation.component.FadeIn.FadeIn;
import com.hujiugame.qingfeng.animation.component.FadeOut.FadeOut;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动画容器。
 * <p>
 * 对应 config 的 animation 节点 {@code {fade_in: {...}, fade_out: {...}}}，
 * 按 {@link AnimationComponentType} 持有整组动画组件（FadeIn / FadeOut）。
 * 构造风格与动画信封类一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该容器。
 */
public final class Animation
{
    private boolean valid;
    private final Map<AnimationComponentType, AnimationComponent> components;
    private JsonEntity json;

    // ===================================================================================================================

    /**
     * 依据组件内容生成 JSON（按组件类型写入 fade_in / fade_out 节点）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        for (Map.Entry<AnimationComponentType, AnimationComponent> entry : components.entrySet())
        {
            if (!entry.getValue().isValid())
            {
                continue;
            }
            json.put(entry.getKey().getKey(), entry.getValue().getJson());
        }
    }

    /**
     * 判断组件是否全部有效（无组件时也视为有效）
     * <p>
     * valid 条件：1. 不存在组件 或 2. 存在的组件全部 valid
     */
    private boolean hasValidComponent ()
    {
        if (components == null || components.isEmpty())
        {
            LogUtils.debug(Animation.class, "Animation(hasValidComponent) 组件为空，视为有效 (components): " + components);
            return true;
        }
        for (AnimationComponent component : components.values())
        {
            if (!component.isValid())
            {
                LogUtils.debug(Animation.class, "Animation(hasValidComponent) 存在无效组件 (component): " + component);
                return false;
            }
        }
        return true;
    }

    // ===================================================================================================================

    /**
     * 字段构造。
     * <p>
     * valid 条件：无组件时 valid；有组件时全部必须 valid。
     *
     * @param components 动画组件映射（类型 → 组件，FadeIn / FadeOut），可空
     */
    public Animation (Map<AnimationComponentType, AnimationComponent> components)
    {
        this.components = components == null ? new LinkedHashMap<>() : new LinkedHashMap<>(components);
        if (!hasValidComponent())
        {
            LogUtils.error(Animation.class, "构造失败 有无效动画组件 (components): " + components);
            valid = false;
            return;
        }
        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析 animation 容器节点
     *
     * @param json 包含 fade_in / fade_out 组件节点的 Map 数据
     */
    public Animation (JsonEntity json)
    {
        this.components = new LinkedHashMap<>();
        LogUtils.debug(Animation.class, "Animation(JsonEntity) 尝试解析动画容器 (json): " + json);
        if (json != null && json.isMap())
        {
            // 多套了一层 解耦
            if (json.containsKey(AnimationKey.ANIMATION_KEY))
            {
                json = json.getJsonEntityByKey(AnimationKey.ANIMATION_KEY);
            }

            this.json = json;
            if (json.containsKey(AnimationKey.Component.FADE_IN))
            {
                FadeIn fadeIn = new FadeIn(json.getJsonEntityByKey(AnimationKey.Component.FADE_IN));
                if (fadeIn.isValid())
                {
                    components.put(AnimationComponentType.FADE_IN, fadeIn);
                }
                else
                {
                    LogUtils.error(Animation.class, "Animation(JsonEntity) fade_in 组件解析无效 (json): " + json.getJsonEntityByKey(AnimationKey.Component.FADE_IN));
                }
            }
            if (json.containsKey(AnimationKey.Component.FADE_OUT))
            {
                FadeOut fadeOut = new FadeOut(json.getJsonEntityByKey(AnimationKey.Component.FADE_OUT));
                if (fadeOut.isValid())
                {
                    components.put(AnimationComponentType.FADE_OUT, fadeOut);
                }
                else
                {
                    LogUtils.error(Animation.class, "Animation(JsonEntity) fade_out 组件解析无效 (json): " + json.getJsonEntityByKey(AnimationKey.Component.FADE_OUT));
                }
            }
            if (hasValidComponent())
            {
                valid = true;
                LogUtils.debug(Animation.class, "Animation(JsonEntity) 解析动画容器成功 (components): " + components);
                return;
            }
        }
        LogUtils.error(Animation.class, "Animation(JsonEntity) 解析失败 需要包含有效的动画组件 (json): " + json);
        valid = false;
    }

    // ===================================================================================================================

    /**
     * 添加动画组件
     *
     * @param component 动画组件
     */
    public void addComponent (AnimationComponent component)
    {
        components.put(component.getType(), component);
    }

    /**
     * 移除指定类型的动画组件
     *
     * @param type 动画组件类型
     */
    public void removeComponent (AnimationComponentType type)
    {
        components.remove(type);
    }

    /**
     * 是否存在指定类型的动画组件
     *
     * @param type 动画组件类型
     */
    public boolean hasComponent (AnimationComponentType type)
    {
        return components.containsKey(type);
    }

    /**
     * 获取指定类型的动画组件
     *
     * @param type 动画组件类型
     */
    public AnimationComponent getComponent (AnimationComponentType type)
    {
        return components.get(type);
    }

    // ===================================================================================================================

    /**
     * 添加 fade_in 组件
     *
     * @param fadeIn fade_in 组件
     */
    public void addFadeInComponent (FadeIn fadeIn)
    {
        components.put(AnimationComponentType.FADE_IN, fadeIn);
    }

    /**
     * 移除 fade_in 组件
     */
    public void removeFadeInComponent ()
    {
        components.remove(AnimationComponentType.FADE_IN);
    }

    /**
     * 是否存在有效 fade_in 组件
     */
    public boolean hasFadeInComponent ()
    {
        return components.containsKey(AnimationComponentType.FADE_IN);
    }

    /**
     * 获取 fade_in 组件
     */
    public FadeIn getFadeInComponent ()
    {
        return (FadeIn) components.get(AnimationComponentType.FADE_IN);
    }

    // ===================================================================================================================

    /**
     * 添加 fade_out 组件
     *
     * @param fadeOut fade_out 组件
     */
    public void addFadeOutComponent (FadeOut fadeOut)
    {
        components.put(AnimationComponentType.FADE_OUT, fadeOut);
    }

    /**
     * 是否存在有效 fade_out 组件
     */
    public boolean hasFadeOutComponent ()
    {
        return components.containsKey(AnimationComponentType.FADE_OUT);
    }

    /**
     * 获取 fade_out 组件
     */
    public FadeOut getFadeOutComponent ()
    {
        return (FadeOut) components.get(AnimationComponentType.FADE_OUT);
    }

    // ===================================================================================================================

    /**
     * 容器是否有效
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取动画组件映射（类型 → 组件）
     */
    public Map<AnimationComponentType, AnimationComponent> getComponents ()
    {
        return components;
    }

    /**
     * 获取构造来源 JSON（字段构造时为 buildJson 生成的 JSON）
     */
    public JsonEntity getJson ()
    {
        return json;
    }

    @Override
    public String toString ()
    {
        return "Animation{" +
            "valid=" + valid +
            ", components=" + components +
            ", json=" + json +
            '}';
    }
}
