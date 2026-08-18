package com.hujiugame.qingfeng.animation.component.FadeIn;

import com.hujiugame.qingfeng.animation.component.AnimationComponent;
import com.hujiugame.qingfeng.animation.component.AnimationComponentType;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 淡入动画组件。
 * <p>
 * 对应 config 的 animation 节点下 fade_in 子节点
 * {@code {default: {...}, from_page: {<layoutDirName>: {...}}}}，
 * 持有通用动画窗口（default）与来源页特例窗口（from_page.xxx）两组 {@link FadeInObject}。
 * 构造风格与 {@link FadeInObject} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该组件。
 */
public final class FadeIn implements AnimationComponent
{
    private boolean valid;
    private final FadeInObject defaultObject;
    private final Map<GameState, FadeInObject> objectMap;
    private JsonEntity json;

    // ==============================================================================

    /**
     * 依据窗口内容生成 JSON（只写入有效窗口）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        if (defaultObject != null && defaultObject.isValid())
        {
            json.put(AnimationKey.Component.FadeIn.DEFAULT, defaultObject.getJson());
        }
        if (!objectMap.isEmpty())
        {
            JsonEntity fromPageJson = new JsonEntity();
            for (Map.Entry<GameState, FadeInObject> entry : objectMap.entrySet())
            {
                if (entry.getValue().isValid())
                {
                    fromPageJson.put(entry.getKey().getLayoutDirName(), entry.getValue().getJson());
                }
            }
            json.put(AnimationKey.Component.FadeIn.FROM_PAGE, fromPageJson);
        }
    }

    /**
     * 是否存在任一有效窗口（default 或特例均可）
     */
    private boolean hasValidObject ()
    {
        if (defaultObject != null && defaultObject.isValid())
        {
            return true;
        }
        for (FadeInObject object : objectMap.values())
        {
            if (object.isValid())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 按布局目录名查找对应状态（仅匹配带页面的状态）
     */
    private static GameState findStateByLayoutDirName (String layoutDirName)
    {
        for (GameState state : GameState.values())
        {
            if (layoutDirName.equals(state.getLayoutDirName()))
            {
                return state;
            }
        }
        return null;
    }

    // ==============================================================================

    /**
     * 字段构造
     *
     * @param defaultObject 通用动画窗口（default），可空
     * @param objectMap     来源页特例窗口（from_page.xxx），可空
     */
    public FadeIn (FadeInObject defaultObject, Map<GameState, FadeInObject> objectMap)
    {
        this.defaultObject = defaultObject;
        this.objectMap = objectMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(objectMap);
        if (!hasValidObject())
        {
            LogUtils.error(FadeIn.class, "构造失败 无有效动画窗口 (defaultObject): " + defaultObject);
            valid = false;
            return;
        }
        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析 fade_in 组件节点
     *
     * @param json 包含 default（及可选 from_page.xxx）窗口节点的 Map 数据
     */
    public FadeIn (JsonEntity json)
    {
        FadeInObject defaultObject = null;
        Map<GameState, FadeInObject> objectMap = new LinkedHashMap<>();
        LogUtils.debug(FadeIn.class, "FadeIn(JsonEntity) 尝试解析淡入组件 (json): " + json);
        if (json != null && json.isMap())
        {
            this.json = json;
            for (String key : json.keySet())
            {
                if (AnimationKey.Component.FadeIn.DEFAULT.equals(key))
                {
                    defaultObject = new FadeInObject(json.getJsonEntityByKey(key));
                    if (!defaultObject.isValid())
                    {
                        LogUtils.error(FadeIn.class, "FadeIn(JsonEntity) default 窗口解析无效 (json): " + json.getJsonEntityByKey(key));
                    }
                }
                else if (AnimationKey.Component.FadeIn.FROM_PAGE.equals(key))
                {
                    JsonEntity fromPageJson = json.getJsonEntityByKey(key);
                    if (fromPageJson.isMap())
                    {
                        for (String layoutDirName : fromPageJson.keySet())
                        {
                            GameState state = findStateByLayoutDirName(layoutDirName);
                            if (state != null)
                            {
                                FadeInObject object = new FadeInObject(fromPageJson.getJsonEntityByKey(layoutDirName));
                                if (object.isValid())
                                {
                                    objectMap.put(state, object);
                                }
                                else
                                {
                                    LogUtils.error(FadeIn.class, "FadeIn(JsonEntity) from_page." + layoutDirName + " 窗口解析无效 (json): " + fromPageJson.getJsonEntityByKey(layoutDirName));
                                }
                            }
                            else
                            {
                                LogUtils.error(FadeIn.class, "FadeIn(JsonEntity) from_page 含未知页面 " + layoutDirName + " (json): " + fromPageJson);
                            }
                        }
                    }
                }
            }
        }
        this.defaultObject = defaultObject;
        this.objectMap = objectMap;
        if (json != null && json.isMap() && hasValidObject())
        {
            valid = true;
            LogUtils.debug(FadeIn.class, "FadeIn(JsonEntity) 解析淡入组件成功 (defaultObject): " + defaultObject + " (objectMap): " + objectMap);
            return;
        }
        LogUtils.error(FadeIn.class, "解析失败 需要包含有效的动画窗口 (json): " + json);
        valid = false;
    }

    /**
     * 获取组件类型
     *
     * @return 淡入组件类型
     */
    @Override
    public AnimationComponentType getType ()
    {
        return AnimationComponentType.FADE_IN;
    }

    /**
     * 组件是否有效
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取通用动画窗口（default）
     */
    public FadeInObject getDefaultObject ()
    {
        return defaultObject;
    }

    /**
     * 获取来源页特例窗口映射（from_page.xxx）
     */
    public Map<GameState, FadeInObject> getObjectMap ()
    {
        return objectMap;
    }

    /**
     * 获取指定状态对应的动画窗口（特例优先，无特例时返回通用窗口）
     *
     * @param state 目标状态
     * @return 对应窗口，可能为 null（该状态既无特例也无有效通用窗口）
     */
    public FadeInObject getObject (GameState state)
    {
        FadeInObject object = objectMap.get(state);
        return object != null ? object : defaultObject;
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
        return "FadeIn{" +
            "valid=" + valid +
            ", defaultObject=" + defaultObject +
            ", objectMap=" + objectMap +
            ", json=" + json +
            '}';
    }
}
