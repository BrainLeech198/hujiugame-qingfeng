package com.hujiugame.qingfeng.animation.component.FadeIn;

import com.hujiugame.qingfeng.animation.task.AnimationTask;
import com.hujiugame.qingfeng.animation.task.object.GraphicsAnimationObject;
import com.hujiugame.qingfeng.animation.task.object.UiAnimationObject;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.animation.AnimationKey;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 淡入动画窗口对象。
 * <p>
 * 对应 config 的 fade_in 节点下 default 或 from_page.xxx 动画窗口
 * {@code {duration, task: {synchronizationGraphics: [...], synchronizationUi: [...], schedule: [...]}}}，
 * 持有整段动画窗口时长与同步/串行两组动画任务。
 * 构造风格与 {@link AnimationTask} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该窗口。
 */
public class FadeInObject
{
    private boolean valid;
    private final float duration;
    private final Set<AnimationTask> synchronizationGraphicsTasks;
    private final Set<AnimationTask> synchronizationUiTasks;
    private final List<AnimationTask> scheduleTasks;
    private JsonEntity json;

    // ==============================================================================

    /**
     * 依据窗口内容生成 JSON（始终写入 task 节点，即使内部数组为空）
     */
    private void buildJson()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Component.FadeIn.DURATION, duration);

        // 始终构造 task 节点，保证结构完整
        JsonEntity taskJson = new JsonEntity();
        // 分别写入两个同步数组（建议在 AnimationKey 中定义常量）
        taskJson.put("synchronizationGraphics", taskToJsonList(synchronizationGraphicsTasks));
        taskJson.put("synchronizationUi", taskToJsonList(synchronizationUiTasks));
        taskJson.put(AnimationKey.Component.FadeIn.SCHEDULE, taskToJsonList(scheduleTasks));
        json.put(AnimationKey.Component.FadeIn.TASK, taskJson);
    }

    /**
     * 将任务集合转为 JSON 列表（仅保留有效任务）
     */
    private static List<JsonEntity> taskToJsonList(Collection<AnimationTask> tasks)
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
     * 解析同步任务组（分别从两个数组读取）
     */
    private static void parseSynchronizationTasks(JsonEntity taskJson,
                                                  Set<AnimationTask> graphicsOut,
                                                  Set<AnimationTask> uiOut)
    {
        // 读取 graphics 同步数组
        JsonEntity graphicsNode = taskJson.getJsonEntityByKey("synchronizationGraphics");
        if (graphicsNode.isList())
        {
            for (int i = 0; i < graphicsNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(graphicsNode.getJsonEntityByIndex(i));
                if (task.isValid() && task.getAnimationObject() instanceof GraphicsAnimationObject)
                {
                    graphicsOut.add(task);
                }
            }
        }

        // 读取 ui 同步数组
        JsonEntity uiNode = taskJson.getJsonEntityByKey("synchronizationUi");
        if (uiNode.isList())
        {
            for (int i = 0; i < uiNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(uiNode.getJsonEntityByIndex(i));
                if (task.isValid() && task.getAnimationObject() instanceof UiAnimationObject)
                {
                    uiOut.add(task);
                }
            }
        }
    }

    /**
     * 解析串行任务组（按列表顺序执行）
     */
    private static List<AnimationTask> parseScheduleTasks(JsonEntity scheduleNode)
    {
        List<AnimationTask> tasks = new ArrayList<>();
        if (scheduleNode.isList())
        {
            for (int i = 0; i < scheduleNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(scheduleNode.getJsonEntityByIndex(i));
                if (task.isValid())
                {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    // ==============================================================================

    /**
     * 字段构造
     *
     * @param duration            整段动画窗口时长（秒，非负）
     * @param synchronizationTasks 同步任务组（全部同时启动），可空，内部会自动分类
     * @param scheduleTasks       串行任务组（按列表顺序执行），可空
     */
    public FadeInObject(float duration, Set<AnimationTask> synchronizationTasks, List<AnimationTask> scheduleTasks)
    {
        this.duration = duration;
        this.synchronizationGraphicsTasks = new LinkedHashSet<>();
        this.synchronizationUiTasks = new LinkedHashSet<>();
        this.scheduleTasks = scheduleTasks == null ? new ArrayList<>() : new ArrayList<>(scheduleTasks);

        if (duration < 0)
        {
            LogUtils.error(FadeInObject.class, "构造失败 动画窗口时长为负数 (duration): " + duration);
            valid = false;
            return;
        }

        // 分类传入的同步任务
        if (synchronizationTasks != null)
        {
            for (AnimationTask task : synchronizationTasks)
            {
                if (!task.isValid()) continue;
                if (task.getAnimationObject() instanceof GraphicsAnimationObject)
                {
                    synchronizationGraphicsTasks.add(task);
                }
                else if (task.getAnimationObject() instanceof UiAnimationObject)
                {
                    synchronizationUiTasks.add(task);
                }
                else
                {
                    LogUtils.debug(FadeInObject.class, "字段构造 未知任务目标类型，忽略: " + task);
                }
            }
        }

        valid = true;
        buildJson();
    }

    /**
     * JsonEntity 构造：解析动画窗口节点
     *
     * @param json 包含 duration 字段（及可选 task 分组节点）的 Map 数据
     */
    public FadeInObject(JsonEntity json)
    {
        this.synchronizationGraphicsTasks = new LinkedHashSet<>();
        this.synchronizationUiTasks = new LinkedHashSet<>();
        this.scheduleTasks = new ArrayList<>();

        if (json != null && json.isMap() && json.containsKey(AnimationKey.Component.FadeIn.DURATION))
        {
            this.duration = json.getFloat(AnimationKey.Component.FadeIn.DURATION);
            this.json = json;
            if (duration < 0)
            {
                LogUtils.error(FadeInObject.class, "解析失败 动画窗口时长为负数 (json): " + json);
                valid = false;
                return;
            }

            // 解析任务分组节点（task 缺失时 getJsonEntityByKey 返回空容器）
            JsonEntity taskJson = json.getJsonEntityByKey(AnimationKey.Component.FadeIn.TASK);
            parseSynchronizationTasks(taskJson, synchronizationGraphicsTasks, synchronizationUiTasks);
            this.scheduleTasks.addAll(parseScheduleTasks(taskJson.getJsonEntityByKey(AnimationKey.Component.FadeIn.SCHEDULE)));

            valid = true;
            LogUtils.debug(FadeInObject.class, "FadeInObject(JsonEntity) 解析淡入窗口成功 (duration): " + duration
                + " (graphicsSync): " + synchronizationGraphicsTasks.size()
                + " (uiSync): " + synchronizationUiTasks.size()
                + " (schedule): " + scheduleTasks.size()
                + " (json): " + json);
        }
        else
        {
            LogUtils.error(FadeInObject.class, "解析失败 需要包含 " + AnimationKey.Component.FadeIn.DURATION + " 字段的 Map 数据 (json): " + json);
            this.duration = 0;
            valid = false;
        }
    }

    /**
     * 窗口是否有效
     */
    public boolean isValid()
    {
        return valid;
    }

    /**
     * 获取整段动画窗口时长（秒）
     */
    public float getDuration()
    {
        return duration;
    }

    /**
     * 获取所有同步任务（合并 Graphics 和 UI）
     */
    public Set<AnimationTask> getSynchronizationTasks()
    {
        Set<AnimationTask> all = new LinkedHashSet<>(synchronizationGraphicsTasks);
        all.addAll(synchronizationUiTasks);
        return all;
    }

    /**
     * 获取 Graphics 同步任务组
     */
    public Set<AnimationTask> getSynchronizationGraphicsTasks()
    {
        return synchronizationGraphicsTasks;
    }

    /**
     * 获取 UI 同步任务组
     */
    public Set<AnimationTask> getSynchronizationUiTasks()
    {
        return synchronizationUiTasks;
    }

    /**
     * 获取串行任务组（按列表顺序执行）
     */
    public List<AnimationTask> getScheduleTasks()
    {
        return scheduleTasks;
    }

    /**
     * 获取构造来源 JSON（字段构造时为 buildJson 生成的 JSON）
     */
    public JsonEntity getJson()
    {
        return json;
    }

    @Override
    public String toString()
    {
        return "FadeInObject{" +
            "valid=" + valid +
            ", duration=" + duration +
            ", synchronizationGraphicsTasks=" + synchronizationGraphicsTasks +
            ", synchronizationUiTasks=" + synchronizationUiTasks +
            ", scheduleTasks=" + scheduleTasks +
            ", json=" + json +
            '}';
    }
}
