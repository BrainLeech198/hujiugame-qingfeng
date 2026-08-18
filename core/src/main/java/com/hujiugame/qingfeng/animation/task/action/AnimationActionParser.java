package com.hujiugame.qingfeng.animation.task.action;

import com.hujiugame.qingfeng.animation.task.action.param.NoneAnimationActionParam;
import com.hujiugame.qingfeng.animation.task.action.param.SmoothMoveAnimationActionParam;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnimationActionParser
{
    public static AnimationAction parse(JsonEntity json)
    {
        if (json != null && json.isMap())
        {
            LogUtils.debug(AnimationActionParser.class, "parse 尝试解析动画动作 (json): " + json);

            // 检查必填字段
            if (!json.containsKey(AnimationKey.Task.Action.TYPE))
            {
                LogUtils.error(AnimationActionParser.class, "parse 此json缺少" + AnimationKey.Task.Action.TYPE + "字段 (json): " + json);
                return null;
            }
            if (!json.containsKey(AnimationKey.Task.Action.DELAY))
            {
                LogUtils.error(AnimationActionParser.class, "parse 此json缺少" + AnimationKey.Task.Action.DELAY + "字段 (json): " + json);
                return null;
            }
            // 新增：检查 duration 字段（必填）
            if (!json.containsKey(AnimationKey.Task.Action.DURATION))
            {
                LogUtils.error(AnimationActionParser.class, "parse 此json缺少" + AnimationKey.Task.Action.DURATION + "字段 (json): " + json);
                return null;
            }
            if (!json.containsKey(AnimationKey.Task.Action.PARAM))
            {
                LogUtils.error(AnimationActionParser.class, "parse 此json缺少" + AnimationKey.Task.Action.PARAM + "字段 (json): " + json);
                return null;
            }

            // 读取字段值
            String type = json.getString(AnimationKey.Task.Action.TYPE);
            float delay = json.getFloat(AnimationKey.Task.Action.DELAY);
            float duration = json.getFloat(AnimationKey.Task.Action.DURATION);
            JsonEntity paramJson = json.getJsonEntityByKey(AnimationKey.Task.Action.PARAM);

            // 尝试解析动画动作（传入 duration）
            AnimationAction action = dispatchAnimationActionJson(type, delay, duration, paramJson);
            if (action == null)
            {
                LogUtils.error(AnimationActionParser.class, "parse 解析动画动作失败 (json): " + json);
                return null;
            }
            else
            {
                if (action.isValid())
                {
                    LogUtils.debug(AnimationActionParser.class, "parse 解析动画动作成功"
                        + " (type): " + action.getActionType()
                        + " (delay): " + action.getDelay()
                        + " (duration): " + action.getDuration()
                        + " (param): " + action.getActionParam()
                        + " (json): " + json);
                    return action;
                }
                else
                {
                    LogUtils.error(AnimationActionParser.class, "parse 解析动画动作不可用 (action): " + action + " (json): " + json);
                    return null;
                }
            }
        }
        else
        {
            LogUtils.error(AnimationActionParser.class, "parse 此json不是字典对象 (json): " + json);
            return null;
        }
    }

    public static List<AnimationAction> parseList(JsonEntity json)
    {
        if (json != null && json.isList())
        {
            LogUtils.debug(AnimationActionParser.class, "parseList 尝试解析动画动作列表 (json): " + json);
            List<AnimationAction> actionList = new ArrayList<>();
            List<Object> jsonList = json.getObjectList();
            for (int i = 0; i < jsonList.size(); i++)
            {
                Object jsonObject = jsonList.get(i);
                JsonEntity itemJson;
                if (jsonObject instanceof JsonEntity)
                {
                    itemJson = (JsonEntity) jsonObject;
                }
                else if (jsonObject instanceof Map)
                {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) jsonObject;
                    itemJson = new JsonEntity(map);
                }
                else
                {
                    LogUtils.error(AnimationActionParser.class, "parseList 第" + i + " 个对象不是json对象 (json): " + json);
                    return null;
                }
                AnimationAction action = parse(itemJson);
                if (action != null)
                {
                    actionList.add(action);
                    LogUtils.debug(AnimationActionParser.class, "parseList 第" + i + " 个动画动作解析成功");
                }
                else
                {
                    LogUtils.error(AnimationActionParser.class, "parseList 第" + i + " 个动画动作解析失败 (json): " + jsonObject);
                    return null;
                }
            }
            LogUtils.debug(AnimationActionParser.class, "parseList 解析动画动作列表成功 (actionList): " + actionList);
            return actionList;
        }
        else
        {
            LogUtils.error(AnimationActionParser.class, "parseList 此json不是列表对象 (json): " + json);
            return null;
        }
    }

    /**
     * 分发解析具体类型的动画动作
     *
     * @param type     动作类型字符串
     * @param delay    延迟时间
     * @param duration 动作持续时长（秒）
     * @param paramJson 参数 JSON
     * @return 解析后的 AnimationAction，失败返回 null
     */
    private static AnimationAction dispatchAnimationActionJson(String type, float delay, float duration, JsonEntity paramJson)
    {
        switch (type)
        {
            case AnimationKey.Task.Action.Type.NONE:
                return new AnimationAction(AnimationActionType.NONE, delay, duration, new NoneAnimationActionParam());

            case AnimationKey.Task.Action.Type.SMOOTH_MOVE:
                return new AnimationAction(AnimationActionType.SMOOTH_MOVE, delay, duration, new SmoothMoveAnimationActionParam(paramJson));

            default:
                LogUtils.error(AnimationActionParser.class, "dispatchAnimationActionJson 没有对应的动画动作类型 (type): " + type);
                return null;
        }
    }
}
