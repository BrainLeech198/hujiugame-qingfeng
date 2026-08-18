package com.hujiugame.qingfeng.animation.task.action;

import com.hujiugame.qingfeng.animation.task.action.param.AnimationActionParam;
import com.hujiugame.qingfeng.animation.task.action.param.NoneAnimationActionParam;
import com.hujiugame.qingfeng.animation.task.action.param.SmoothMoveAnimationActionParam;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;

import java.util.HashMap;
import java.util.Map;

/**
 * 动画动作信封。
 * <p>
 * 对应 config 的 animation 节点下单个动作对象 {@code {type, delay, duration, param}}。
 * 持有动作类型、相对延迟、动作时长与动作参数，构造时按类型校验参数实现类匹配。
 */
public class AnimationAction
{
    private boolean valid;
    private final AnimationActionType actionType;
    private final float delay;
    private final float duration;          // 新增：动作持续时长（秒）
    private final AnimationActionParam actionParam;
    private JsonEntity json;

    // ==============================================================================

    private static final Map<AnimationActionType, Class<? extends AnimationActionParam>> ACTION_PARAM_MAP;

    static
    {
        ACTION_PARAM_MAP = new HashMap<>();
        ACTION_PARAM_MAP.put(AnimationActionType.NONE, NoneAnimationActionParam.class);
        ACTION_PARAM_MAP.put(AnimationActionType.SMOOTH_MOVE, SmoothMoveAnimationActionParam.class);
    }

    // ============================================================================

    public AnimationAction(AnimationActionType actionType, float delay, float duration, AnimationActionParam actionParam)
    {
        this.actionType = actionType;
        this.delay = delay;
        this.duration = duration;
        // 检查参数是否符合类型
        if (!ACTION_PARAM_MAP.get(actionType).isInstance(actionParam))
        {
            throw new IllegalArgumentException(
                "Animation action parameter type : " + actionParam.getClass().getName()
                    + " does not match animation action type : " + actionType
            );
        }
        else
        {
            this.actionParam = actionParam;
        }
        this.valid = true;
        buildJson();
    }

    private void buildJson()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Task.Action.TYPE, actionType.getDisplayString());
        json.put(AnimationKey.Task.Action.DELAY, delay);
        json.put(AnimationKey.Task.Action.DURATION, duration);   // 新增
        json.put(AnimationKey.Task.Action.PARAM, actionParam.getJson());
    }

    // ========== getters ==========

    public boolean isValid()
    {
        return valid;
    }

    public AnimationActionType getActionType()
    {
        return actionType;
    }

    public float getDelay()
    {
        return delay;
    }

    /**
     * 获取动作持续时长（秒）
     */
    public float getDuration()
    {
        return duration;
    }

    public AnimationActionParam getActionParam()
    {
        return actionParam;
    }

    public JsonEntity getJson()
    {
        return json;
    }

    @Override
    public String toString()
    {
        return "AnimationAction{" +
            "valid=" + valid +
            ", actionType=" + actionType +
            ", delay=" + delay +
            ", duration=" + duration +
            ", actionParam=" + actionParam +
            ", json=" + json +
            '}';
    }
}
