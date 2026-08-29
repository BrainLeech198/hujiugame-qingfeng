package com.hujiugame.qingfeng.animation.task.command;

import com.hujiugame.qingfeng.animation.task.command.param.AnimationCommandParam;
import com.hujiugame.qingfeng.animation.task.command.param.NoneAnimationCommandParam;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 动画指令信封。
 * <p>
 * 对应 config 的 command 节点 {@code {start_time, end_time, duration, type, action, param}}。
 * 持有时间三字段、指令大类、细分行为与指令参数，构造时按类型校验参数实现类匹配。
 * <p>
 * 6 个字段全部必填，缺一不可。
 */
public final class AnimationCommand
{
    private boolean valid;
    private final float startTime;
    private final float endTime;
    private final float duration;
    private final AnimationCommandType commandType;
    private final AnimationCommandAction commandAction;
    private final AnimationCommandParam commandParam;
    private JsonEntity json;

    // ==============================================================================

    private static final Map<AnimationCommandAction, Class<? extends AnimationCommandParam>> COMMAND_PARAM_MAP;

    static
    {
        COMMAND_PARAM_MAP = new HashMap<>();
        COMMAND_PARAM_MAP.put(AnimationCommandAction.NONE, NoneAnimationCommandParam.class);
    }

    // ============================================================================

    /**
     * 字段构造
     *
     * @param startTime    动作起始时间（秒，相对窗口起点）
     * @param endTime      动作结束时间（秒，相对窗口起点）
     * @param duration     动作时长（秒）
     * @param commandType  指令大类
     * @param commandAction 具体动作
     * @param commandParam  指令参数
     */
    public AnimationCommand (float startTime, float endTime, float duration,
                             AnimationCommandType commandType, AnimationCommandAction commandAction,
                             AnimationCommandParam commandParam)
    {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.commandType = commandType;
        this.commandAction = commandAction;
        // 检查参数是否符合类型
        if (!COMMAND_PARAM_MAP.get(commandAction).isInstance(commandParam))
        {
            throw new IllegalArgumentException(
                "Animation command parameter type : " + commandParam.getClass().getName()
                    + " does not match animation command action : " + commandAction
            );
        }
        else
        {
            this.commandParam = commandParam;
        }
        this.valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析 command 节点
     *
     * @param json 包含 start_time / end_time / duration / type / action / param 的 Map 数据
     */
    public AnimationCommand (JsonEntity json)
    {
        if (json != null && json.isMap()
            && json.containsKey(AnimationKey.Task.Command.START_TIME)
            && json.containsKey(AnimationKey.Task.Command.END_TIME)
            && json.containsKey(AnimationKey.Task.Command.DURATION)
            && json.containsKey(AnimationKey.Task.Command.TYPE)
            && json.containsKey(AnimationKey.Task.Command.ACTION)
            && json.containsKey(AnimationKey.Task.Command.PARAM))
        {
            this.startTime = json.getFloat(AnimationKey.Task.Command.START_TIME);
            this.endTime = json.getFloat(AnimationKey.Task.Command.END_TIME);
            this.duration = json.getFloat(AnimationKey.Task.Command.DURATION);

            // 解析大类
            String typeStr = json.getString(AnimationKey.Task.Command.TYPE);
            this.commandType = AnimationCommandType.fromString(typeStr);
            if (this.commandType == null)
            {
                LogUtils.error(AnimationCommand.class, "解析失败 未知指令大类 (type): " + typeStr + " (json): " + json);
                this.commandAction = null;
                this.commandParam = null;
                this.valid = false;
                return;
            }

            // 解析具体动作
            String actionStr = json.getString(AnimationKey.Task.Command.ACTION);
            this.commandAction = AnimationCommandAction.fromString(actionStr);
            if (this.commandAction == null)
            {
                LogUtils.error(AnimationCommand.class, "解析失败 未知具体动作 (action): " + actionStr + " (json): " + json);
                this.commandParam = null;
                this.valid = false;
                return;
            }

            // 解析参数
            JsonEntity paramJson = json.getJsonEntityByKey(AnimationKey.Task.Command.PARAM);
            AnimationCommandParam param = AnimationCommandParser.parseParam(this.commandAction, paramJson);
            if (param == null || !param.isValid())
            {
                LogUtils.error(AnimationCommand.class, "解析失败 指令参数无效 (commandAction): " + commandAction + " (param): " + paramJson);
                this.commandParam = null;
                this.valid = false;
                return;
            }
            this.commandParam = param;

            this.json = json;
            this.valid = true;
            LogUtils.debug(AnimationCommand.class, "AnimationCommand(JsonEntity) 解析成功"
                + " (commandType): " + commandType
                + " (commandAction): " + commandAction
                + " (startTime): " + startTime
                + " (endTime): " + endTime
                + " (duration): " + duration);
        }
        else
        {
            LogUtils.error(AnimationCommand.class, "解析失败 需要包含 start_time / end_time / duration / type / action / param 字段 (json): " + json);
            this.startTime = 0;
            this.endTime = 0;
            this.duration = 0;
            this.commandType = null;
            this.commandAction = null;
            this.commandParam = null;
            valid = false;
        }
    }

    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Task.Command.START_TIME, startTime);
        json.put(AnimationKey.Task.Command.END_TIME, endTime);
        json.put(AnimationKey.Task.Command.DURATION, duration);
        json.put(AnimationKey.Task.Command.TYPE, commandType.getDisplayString());
        json.put(AnimationKey.Task.Command.ACTION, commandAction.getDisplayString());
        json.put(AnimationKey.Task.Command.PARAM, commandParam.getJson());
    }

    // ========== getters ==========

    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取动作起始时间（秒，相对窗口起点）
     */
    public float getStartTime ()
    {
        return startTime;
    }

    /**
     * 获取动作结束时间（秒，相对窗口起点）
     */
    public float getEndTime ()
    {
        return endTime;
    }

    /**
     * 获取动作持续时长（秒）
     */
    public float getDuration ()
    {
        return duration;
    }

    /**
     * 获取指令大类
     */
    public AnimationCommandType getCommandType ()
    {
        return commandType;
    }

    /**
     * 获取具体动作
     */
    public AnimationCommandAction getCommandAction ()
    {
        return commandAction;
    }

    /**
     * 获取指令参数
     */
    public AnimationCommandParam getCommandParam ()
    {
        return commandParam;
    }

    public JsonEntity getJson ()
    {
        return json;
    }

    @Override
    public String toString ()
    {
        return "AnimationCommand{" +
            "valid=" + valid +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", duration=" + duration +
            ", commandType=" + commandType +
            ", commandAction=" + commandAction +
            ", commandParam=" + commandParam +
            ", json=" + json +
            '}';
    }
}
