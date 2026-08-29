package com.hujiugame.qingfeng.animation.component.FadeOut;

import com.hujiugame.qingfeng.animation.task.AnimationTask;
import com.hujiugame.qingfeng.animation.task.object.AnimationObjectClass;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 淡出动画窗口对象。
 * <p>
 * 对应 config 的 fade_out 节点下 default 或 from_page.xxx 动画窗口
 * {@code {duration, task: {graphics: [...], ui: [...]}}}，
 * 持有整段动画窗口时长与 graphics / ui 两组动画任务。
 * <p>
 * 执行顺序由每个 task 内部的 start_time / end_time 时序决定，不再区分同步/串行。
 * 构造风格与 {@link AnimationTask} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该窗口。
 */
public class FadeOutObject
{
    private boolean valid;
    private final float duration;
    private final List<AnimationTask> graphicsTasks;
    private final List<AnimationTask> uiTasks;
    private JsonEntity json;

    // ==============================================================================

    /**
     * 依据窗口内容生成 JSON（始终写入 task 节点，即使内部数组为空）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Component.FadeOut.DURATION, duration);

        JsonEntity taskJson = new JsonEntity();
        taskJson.put(AnimationKey.Component.FadeOut.GRAPHICS, taskToJsonList(graphicsTasks));
        taskJson.put(AnimationKey.Component.FadeOut.UI, taskToJsonList(uiTasks));
        json.put(AnimationKey.Component.FadeOut.TASK, taskJson);
    }

    /**
     * 将任务列表转为 JSON 列表（仅保留有效任务）
     */
    private static List<JsonEntity> taskToJsonList (Collection<AnimationTask> tasks)
    {
        List<JsonEntity> list = new ArrayList<>();
        for (AnimationTask task : tasks)
        {
            if (task.isValid())
            {
                list.add(task.getJson());
            }
        }
        return list;
    }

    /**
     * 从 task 节点解析 graphics[] 和 ui[] 任务数组
     */
    private static void parseTasks (JsonEntity taskJson,
                                    List<AnimationTask> graphicsOut,
                                    List<AnimationTask> uiOut)
    {
        // 解析 graphics 数组
        JsonEntity graphicsNode = taskJson.getJsonEntityByKey(AnimationKey.Component.FadeOut.GRAPHICS);
        if (graphicsNode.isList())
        {
            for (int i = 0; i < graphicsNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(graphicsNode.getJsonEntityByIndex(i), AnimationObjectClass.GRAPHICS);
                if (task.isValid())
                {
                    graphicsOut.add(task);
                }
                else
                {
                    LogUtils.error(FadeOutObject.class, "parseTasks graphics 任务解析无效 (index): " + i + " (json): " + graphicsNode.getJsonEntityByIndex(i));
                }
            }
        }

        // 解析 ui 数组
        JsonEntity uiNode = taskJson.getJsonEntityByKey(AnimationKey.Component.FadeOut.UI);
        if (uiNode.isList())
        {
            for (int i = 0; i < uiNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(uiNode.getJsonEntityByIndex(i), AnimationObjectClass.UI);
                if (task.isValid())
                {
                    uiOut.add(task);
                }
                else
                {
                    LogUtils.error(FadeOutObject.class, "parseTasks ui 任务解析无效 (index): " + i + " (json): " + uiNode.getJsonEntityByIndex(i));
                }
            }
        }
    }

    // ==============================================================================

    /**
     * 字段构造
     *
     * @param duration     整段动画窗口时长（秒，非负）
     * @param graphicsTasks graphics 元素任务列表，可空
     * @param uiTasks       ui 控件任务列表，可空
     */
    public FadeOutObject (float duration, List<AnimationTask> graphicsTasks, List<AnimationTask> uiTasks)
    {
        this.duration = duration;
        this.graphicsTasks = graphicsTasks == null ? new ArrayList<>() : new ArrayList<>(graphicsTasks);
        this.uiTasks = uiTasks == null ? new ArrayList<>() : new ArrayList<>(uiTasks);

        if (duration < 0)
        {
            LogUtils.error(FadeOutObject.class, "构造失败 动画窗口时长为负数 (duration): " + duration);
            valid = false;
            return;
        }

        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析动画窗口节点
     *
     * @param json 包含 duration 字段（及可选 task 分组节点）的 Map 数据
     */
    public FadeOutObject (JsonEntity json)
    {
        this.graphicsTasks = new ArrayList<>();
        this.uiTasks = new ArrayList<>();

        if (json != null && json.isMap() && json.containsKey(AnimationKey.Component.FadeOut.DURATION))
        {
            this.duration = json.getFloat(AnimationKey.Component.FadeOut.DURATION);
            this.json = json;
            if (duration < 0)
            {
                LogUtils.error(FadeOutObject.class, "解析失败 动画窗口时长为负数 (json): " + json);
                valid = false;
                return;
            }

            // 解析任务分组节点（task 缺失时 getJsonEntityByKey 返回空容器）
            JsonEntity taskJson = json.getJsonEntityByKey(AnimationKey.Component.FadeOut.TASK);
            parseTasks(taskJson, graphicsTasks, uiTasks);

            valid = true;
            LogUtils.debug(FadeOutObject.class, "FadeOutObject(JsonEntity) 解析淡出窗口成功 (duration): " + duration
                + " (graphics): " + graphicsTasks.size()
                + " (ui): " + uiTasks.size()
                + " (json): " + json);
        }
        else
        {
            LogUtils.error(FadeOutObject.class, "解析失败 需要包含 " + AnimationKey.Component.FadeOut.DURATION + " 字段的 Map 数据 (json): " + json);
            this.duration = 0;
            valid = false;
        }
    }

    /**
     * 窗口是否有效
     */
    public boolean isValid ()
    {
        return valid;
    }

    /**
     * 获取整段动画窗口时长（秒）
     */
    public float getDuration ()
    {
        return duration;
    }

    /**
     * 获取 graphics 元素任务列表
     */
    public List<AnimationTask> getGraphicsTasks ()
    {
        return graphicsTasks;
    }

    /**
     * 获取 ui 控件任务列表
     */
    public List<AnimationTask> getUiTasks ()
    {
        return uiTasks;
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
        return "FadeOutObject{" +
            "valid=" + valid +
            ", duration=" + duration +
            ", graphicsTasks=" + graphicsTasks +
            ", uiTasks=" + uiTasks +
            ", json=" + json +
            '}';
    }
}
