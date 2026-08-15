package com.hujiugame.qingfeng.animation;

import com.hujiugame.qingfeng.animation.component.AnimationComponent;
import com.hujiugame.qingfeng.animation.component.FadeIn.FadeIn;
import com.hujiugame.qingfeng.animation.component.FadeOut.FadeOut;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 动画容器。
 * <p>
 * 对应 config 的 animation 节点 {@code {fade_in: {...}, fade_out: {...}}}，
 * 持有整组动画组件（FadeIn / FadeOut）。
 * 构造风格与动画信封类一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该容器。
 */
public final class Animation
{
    private boolean valid;
    private final Set<AnimationComponent> components;
    private JsonEntity json;

    // ==============================================================================

    /**
     * 依据组件内容生成 JSON（按组件类型写入 fade_in / fade_out 节点）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        for (AnimationComponent component : components)
        {
            if (!component.isValid())
            {
                continue;
            }
            if (component instanceof FadeIn)
            {
                json.put(AnimationKey.Component.FADE_IN, component.getJson());
            }
            else if (component instanceof FadeOut)
            {
                json.put(AnimationKey.Component.FADE_OUT, component.getJson());
            }
        }
    }

    /**
     * 是否存在任一有效组件
     */
    private boolean hasValidComponent ()
    {
        for (AnimationComponent component : components)
        {
            if (component.isValid())
            {
                return true;
            }
        }
        return false;
    }

    // ==============================================================================

    /**
     * 字段构造
     *
     * @param components 动画组件集合（FadeIn / FadeOut），可空
     */
    public Animation (Set<AnimationComponent> components)
    {
        this.components = components == null ? new LinkedHashSet<>() : new LinkedHashSet<>(components);
        if (!hasValidComponent())
        {
            LogUtils.error(Animation.class, "构造失败 无有效动画组件 (components): " + components);
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
        this.components = new LinkedHashSet<>();
        if (json.isMap())
        {
            this.json = json;
            if (json.containsKey(AnimationKey.Component.FADE_IN))
            {
                components.add(new FadeIn(json.getJsonEntityByKey(AnimationKey.Component.FADE_IN)));
            }
            if (json.containsKey(AnimationKey.Component.FADE_OUT))
            {
                components.add(new FadeOut(json.getJsonEntityByKey(AnimationKey.Component.FADE_OUT)));
            }
            if (hasValidComponent())
            {
                valid = true;
                return;
            }
        }
        LogUtils.error(Animation.class, "解析失败 需要包含有效的动画组件 (json): " + json);
        valid = false;
    }

    /**
     * 容器是否有效
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取动画组件集合
     */
    public Set<AnimationComponent> getComponents ()
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
