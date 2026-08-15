package com.hujiugame.qingfeng.animation.task.action.param;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
import com.hujiugame.qingfeng.type.key.JsonKey;

/**
 * smooth_move 动作参数。
 * <p>
 * 从控件原本位置出发做相对位移，位移向量为 orientation，距离为 speed，时长为 duration。
 */
public class SmoothMoveAnimationActionParam implements AnimationActionParam
{
    private boolean valid;
    private JsonEntity json;

    private float orientationX;
    private float orientationY;
    private float speed;
    private float duration;

    private void buildJson ()
    {
        if (!valid)
        {
            throw new IllegalStateException("An invalid animation action parameter cannot be built.");
        }
        json = new JsonEntity();
        JsonEntity orientation = new JsonEntity();
        orientation.put(JsonKey.Position.X, orientationX);
        orientation.put(JsonKey.Position.Y, orientationY);
        json.put(AnimationKey.Task.Action.Param.ORIENTATION, orientation);
        json.put(AnimationKey.Task.Action.Param.SPEED, speed);
        json.put(AnimationKey.Task.Action.Param.DURATION, duration);
    }

    public SmoothMoveAnimationActionParam (float orientationX, float orientationY, float speed, float duration)
    {
        valid = true;
        this.orientationX = orientationX;
        this.orientationY = orientationY;
        this.speed = speed;
        this.duration = duration;
        buildJson();
    }

    public SmoothMoveAnimationActionParam (JsonEntity json)
    {
        valid = false;
        if (json.isMap())
        {
            JsonEntity orientation = json.getJsonEntityByKey(AnimationKey.Task.Action.Param.ORIENTATION);
            if (orientation == null)
            {
                throw new IllegalArgumentException("Animation action parameter must have \"" + AnimationKey.Task.Action.Param.ORIENTATION + "\" field. (json): " + json);
            }
            orientationX = orientation.getFloat(JsonKey.Position.X);
            orientationY = orientation.getFloat(JsonKey.Position.Y);
            speed = json.getFloat(AnimationKey.Task.Action.Param.SPEED);
            duration = json.getFloat(AnimationKey.Task.Action.Param.DURATION);
            this.json = json;
            valid = true;
        }
        else
        {
            throw new IllegalArgumentException("Animation action parameter must be a map.");
        }
    }

    @Override
    public boolean isValid ()
    {
        return valid;
    }

    @Override
    public JsonEntity getJson ()
    {
        return json;
    }

    @Override
    public String toString ()
    {
        return "SmoothMoveAnimationActionParam{" +
            "valid=" + valid +
            ", orientationX=" + orientationX +
            ", orientationY=" + orientationY +
            ", speed=" + speed +
            ", duration=" + duration +
            ", json=" + json +
            '}';
    }

    public float getOrientationX ()
    {
        return orientationX;
    }

    public float getOrientationY ()
    {
        return orientationY;
    }

    public float getSpeed ()
    {
        return speed;
    }

    public float getDuration ()
    {
        return duration;
    }
}
