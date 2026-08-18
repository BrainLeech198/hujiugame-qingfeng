package com.hujiugame.qingfeng.animation.component.FadeOut;

import com.hujiugame.qingfeng.animation.component.AnimationComponent;
import com.hujiugame.qingfeng.animation.component.AnimationComponentType;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 淡出动画组件。
 * <p>
 * 对应 config 的 animation 节点下 fade_out 子节点
 * {@code {default: {...}, from_page: {<layoutDirName>: {...}}}}，
 * 持有通用动画窗口（default）与来源页特例窗口（from_page.xxx）两组 {@link FadeOutObject}。
 * 构造风格与 {@link FadeOutObject} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该组件。
 */
public final class FadeOut implements AnimationComponent
{
    private boolean valid;
    private final FadeOutObject defaultObject;
    private final Map<GameState, FadeOutObject> objectMap;
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
            json.put(AnimationKey.Component.FadeOut.DEFAULT, defaultObject.getJson());
        }
        if (!objectMap.isEmpty())
        {
            JsonEntity fromPageJson = new JsonEntity();
            for (Map.Entry<GameState, FadeOutObject> entry : objectMap.entrySet())
            {
                if (entry.getValue().isValid())
                {
                    fromPageJson.put(entry.getKey().getLayoutDirName(), entry.getValue().getJson());
                }
            }
            json.put(AnimationKey.Component.FadeOut.FROM_PAGE, fromPageJson);
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
        for (FadeOutObject object : objectMap.values())
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
    public FadeOut (FadeOutObject defaultObject, Map<GameState, FadeOutObject> objectMap)
    {
        this.defaultObject = defaultObject;
        this.objectMap = objectMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(objectMap);
        if (!hasValidObject())
        {
            LogUtils.error(FadeOut.class, "构造失败 无有效动画窗口 (defaultObject): " + defaultObject);
            valid = false;
            return;
        }
        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析 fade_out 组件节点
     *
     * @param json 包含 default（及可选 from_page.xxx）窗口节点的 Map 数据
     */
    public FadeOut (JsonEntity json)
    {
        FadeOutObject defaultObject = null;
        Map<GameState, FadeOutObject> objectMap = new LinkedHashMap<>();
        LogUtils.debug(FadeOut.class, "FadeOut(JsonEntity) 尝试解析淡出组件 (json): " + json);
        if (json != null && json.isMap())
        {
            this.json = json;
            for (String key : json.keySet())
            {
                if (AnimationKey.Component.FadeOut.DEFAULT.equals(key))
                {
                    defaultObject = new FadeOutObject(json.getJsonEntityByKey(key));
                    if (!defaultObject.isValid())
                    {
                        LogUtils.error(FadeOut.class, "FadeOut(JsonEntity) default 窗口解析无效 (json): " + json.getJsonEntityByKey(key));
                    }
                }
                else if (AnimationKey.Component.FadeOut.FROM_PAGE.equals(key))
                {
                    JsonEntity fromPageJson = json.getJsonEntityByKey(key);
                    if (fromPageJson.isMap())
                    {
                        for (String layoutDirName : fromPageJson.keySet())
                        {
                            GameState state = findStateByLayoutDirName(layoutDirName);
                            if (state != null)
                            {
                                FadeOutObject object = new FadeOutObject(fromPageJson.getJsonEntityByKey(layoutDirName));
                                if (object.isValid())
                                {
                                    objectMap.put(state, object);
                                }
                                else
                                {
                                    LogUtils.error(FadeOut.class, "FadeOut(JsonEntity) from_page." + layoutDirName + " 窗口解析无效 (json): " + fromPageJson.getJsonEntityByKey(layoutDirName));
                                }
                            }
                            else
                            {
                                LogUtils.error(FadeOut.class, "FadeOut(JsonEntity) from_page 含未知页面 " + layoutDirName + " (json): " + fromPageJson);
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
            LogUtils.debug(FadeOut.class, "FadeOut(JsonEntity) 解析淡出组件成功 (defaultObject): " + defaultObject + " (objectMap): " + objectMap);
            return;
        }
        LogUtils.error(FadeOut.class, "解析失败 需要包含有效的动画窗口 (json): " + json);
        valid = false;
    }

    /**
     * 获取组件类型
     *
     * @return 淡出组件类型
     */
    @Override
    public AnimationComponentType getType ()
    {
        return AnimationComponentType.FADE_OUT;
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
    public FadeOutObject getDefaultObject ()
    {
        return defaultObject;
    }

    /**
     * 获取来源页特例窗口映射（from_page.xxx）
     */
    public Map<GameState, FadeOutObject> getObjectMap ()
    {
        return objectMap;
    }

    /**
     * 获取指定状态对应的动画窗口（特例优先，无特例时返回通用窗口）
     *
     * @param state 目标状态
     * @return 对应窗口，可能为 null（该状态既无特例也无有效通用窗口）
     */
    public FadeOutObject getObject (GameState state)
    {
        FadeOutObject object = objectMap.get(state);
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
        return "FadeOut{" +
            "valid=" + valid +
            ", defaultObject=" + defaultObject +
            ", objectMap=" + objectMap +
            ", json=" + json +
            '}';
    }
}
