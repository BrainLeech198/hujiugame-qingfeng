package com.hujiugame.qingfeng.animation.component.FadeIn;

import com.hujiugame.qingfeng.animation.task.AnimationTask;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.key.AnimationKey;
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
 * {@code {duration, task: {synchronization: [...], schedule: [...]}}}，
 * 持有整段动画窗口时长与同步/串行两组动画任务。
 * 构造风格与 {@link AnimationTask} 一致：字段构造 + JsonEntity 构造双构造器，携带 valid 与 json；
 * 解析失败标记 valid=false（fail-soft），由上层动画加载点跳过该窗口。
 */
public class FadeInObject
{
    private boolean valid;
    private final float duration;
    private final Set<AnimationTask> synchronizationTasks;
    private final List<AnimationTask> scheduleTasks;
    private JsonEntity json;

    // ==============================================================================

    /**
     * 依据窗口内容生成 JSON（有任务时才写 task 分组节点）
     */
    private void buildJson ()
    {
        json = new JsonEntity();
        json.put(AnimationKey.Component.FadeIn.DURATION, duration);
        if (!synchronizationTasks.isEmpty() || !scheduleTasks.isEmpty())
        {
            JsonEntity taskJson = new JsonEntity();
            taskJson.put(AnimationKey.Component.FadeIn.SYNCHRONIZATION, taskToJsonList(synchronizationTasks));
            taskJson.put(AnimationKey.Component.FadeIn.SCHEDULE, taskToJsonList(scheduleTasks));
            json.put(AnimationKey.Component.FadeIn.TASK, taskJson);
        }
    }

    /**
     * 将任务集合转为 JSON 列表（仅保留有效任务）
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
     * 解析同步任务组（全部同时启动，Set 去重保序）
     */
    private static Set<AnimationTask> parseSynchronizationTasks (JsonEntity syncNode)
    {
        Set<AnimationTask> tasks = new LinkedHashSet<>();
        if (syncNode.isList())
        {
            for (int i = 0; i < syncNode.size(); i++)
            {
                AnimationTask task = new AnimationTask(syncNode.getJsonEntityByIndex(i));
                if (task.isValid())
                {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * 解析串行任务组（按列表顺序执行）
     */
    private static List<AnimationTask> parseScheduleTasks (JsonEntity scheduleNode)
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
     * @param synchronizationTasks 同步任务组（全部同时启动），可空
     * @param scheduleTasks       串行任务组（按列表顺序执行），可空
     */
    public FadeInObject (float duration, Set<AnimationTask> synchronizationTasks, List<AnimationTask> scheduleTasks)
    {
        this.duration = duration;
        this.synchronizationTasks = synchronizationTasks == null ? new LinkedHashSet<>() : new LinkedHashSet<>(synchronizationTasks);
        this.scheduleTasks = scheduleTasks == null ? new ArrayList<>() : new ArrayList<>(scheduleTasks);
        if (duration < 0)
        {
            LogUtils.error(FadeInObject.class, "构造失败 动画窗口时长为负数 (duration): " + duration);
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
    public FadeInObject (JsonEntity json)
    {
        if (json.isMap() && json.containsKey(AnimationKey.Component.FadeIn.DURATION))
        {
            this.duration = json.getFloat(AnimationKey.Component.FadeIn.DURATION);
            this.json = json;
            if (duration < 0)
            {
                LogUtils.error(FadeInObject.class, "解析失败 动画窗口时长为负数 (json): " + json);
                this.synchronizationTasks = new LinkedHashSet<>();
                this.scheduleTasks = new ArrayList<>();
                valid = false;
                return;
            }
            // 解析任务分组节点（task 缺失时 getJsonEntityByKey 返回空容器）
            JsonEntity taskJson = json.getJsonEntityByKey(AnimationKey.Component.FadeIn.TASK);
            this.synchronizationTasks = parseSynchronizationTasks(taskJson.getJsonEntityByKey(AnimationKey.Component.FadeIn.SYNCHRONIZATION));
            this.scheduleTasks = parseScheduleTasks(taskJson.getJsonEntityByKey(AnimationKey.Component.FadeIn.SCHEDULE));
            valid = true;
        }
        else
        {
            LogUtils.error(FadeInObject.class, "解析失败 需要包含 " + AnimationKey.Component.FadeIn.DURATION + " 字段的 Map 数据 (json): " + json);
            this.duration = 0;
            this.synchronizationTasks = new LinkedHashSet<>();
            this.scheduleTasks = new ArrayList<>();
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
     * 获取同步任务组（全部同时启动）
     */
    public Set<AnimationTask> getSynchronizationTasks ()
    {
        return synchronizationTasks;
    }

    /**
     * 获取串行任务组（按列表顺序执行）
     */
    public List<AnimationTask> getScheduleTasks ()
    {
        return scheduleTasks;
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
        return "FadeInObject{" +
            "valid=" + valid +
            ", duration=" + duration +
            ", synchronizationTasks=" + synchronizationTasks +
            ", scheduleTasks=" + scheduleTasks +
            ", json=" + json +
            '}';
    }
}
