package com.hujiugame.qingfeng.animation.manager;

import com.hujiugame.qingfeng.animation.Animation;
import com.hujiugame.qingfeng.animation.component.FadeIn.FadeIn;
import com.hujiugame.qingfeng.animation.component.FadeIn.FadeInObject;
import com.hujiugame.qingfeng.animation.component.FadeOut.FadeOut;
import com.hujiugame.qingfeng.animation.component.FadeOut.FadeOutObject;
import com.hujiugame.qingfeng.animation.task.AnimationTask;
import com.hujiugame.qingfeng.animation.task.action.AnimationAction;
import com.hujiugame.qingfeng.animation.task.action.AnimationActionType;
import com.hujiugame.qingfeng.animation.task.action.param.SmoothMoveAnimationActionParam;
import com.hujiugame.qingfeng.animation.task.object.GraphicsAnimationObject;
import com.hujiugame.qingfeng.animation.task.object.UiAnimationObject;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.data.game.Layout;
import com.hujiugame.qingfeng.event.Event;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.event.imp.RecoverNormalRenderPipeLine;
import com.hujiugame.qingfeng.event.imp.state.*;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.graphic.model.GifInfo;
import com.hujiugame.qingfeng.graphic.model.PictureInfo;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.game.GameStateEventAction;
import com.hujiugame.qingfeng.type.ui.GraphicsKind;
import com.hujiugame.qingfeng.ui.UiManager;
import com.hujiugame.qingfeng.ui.info.GraphicsObject;
import com.hujiugame.qingfeng.ui.kind.InteractableObject;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.List;
import java.util.Set;

public final class TransitionManager
{
    private EventQueue eventQueue;

    private boolean isReady = false; // 是否准备启动过渡效果
    private boolean isFadingOut = false; // 是否正在执行淡入
    private boolean fadedOut = false; // 是否已完成淡入（新页渲染机 init 后执行淡入，完成后过渡结束）
    private boolean isFadingIn = false; // 是否正在执行淡出
    private boolean fadedIn = false; // 是否已完成淡出（旧页渲染机 dispose 前执行淡出，完成后过渡结束）

    private Layout layout; // 拷贝的布局

    private FadeOut fadeOut; // 淡入动画配置
    private FadeIn fadeIn; // 淡出动画配置

    private float fadeOutElapsed; // 淡出已用时间
    private float fadeOutDuration; // 淡出动画窗口总时长
    private long fadeOutStartTimeMillis; // 淡出开始时间戳（毫秒）
    private Set<AnimationTask> fadeOutSynchronizationGraphicsTasks; // 淡出同步Graphics任务集合
    private Set<AnimationTask> fadeOutSynchronizationUiTasks; // 淡出同步UI任务集合
    private List<AnimationTask> fadeOutScheduleTasks; // 淡出调度任务列表
    private int fadeOutScheduleTaskIndex; // 淡出调度任务索引
    private boolean isFadingOutScheduleTask; // 是否正在执行淡出调度任务
    private float fadeOutScheduleTaskElapsed; // 淡出调度任务已用时间

    private float fadeInElapsed; // 淡入已用时间
    private float fadeInDuration; // 淡入动画窗口总时长
    private long fadeInStartTimeMillis; // 淡入开始时间戳（毫秒）
    private Set<AnimationTask> fadeInSynchronizationGraphicsTasks; // 淡入同步Graphics任务集合
    private Set<AnimationTask> fadeInSynchronizationUiTasks; // 淡入同步UI任务集合
    private List<AnimationTask> fadeInScheduleTasks; // 淡入调度任务列表
    private int fadeInScheduleTaskIndex; // 淡入调度任务索引
    private boolean isFadingInScheduleTask; // 是否正在执行淡入调度任务
    private float fadeInScheduleTaskElapsed; // 淡入调度任务已用时间

    private GameStateEventAction gameStateEventAction;
    private GameState outState;
    private GameState inState;

    public TransitionManager (EventQueue eventQueue)
    {
        this.eventQueue = eventQueue;
    }

    /**
     * 设置待切换的游戏状态
     *
     * @param event 事件
     */
    public void setPendingTask (Event event)
    {
        // 识别事件是否属于游戏状态操作事件：EventAction → GameStateEventAction 联动转换，
        // 非状态操作事件（REFRESH_UI_MANAGER/ENTER_GAME/QUIT_GAME/PLAY_GAME）转换结果为 null，直接忽略
        GameStateEventAction stateEventAction = GameStateEventAction.fromEvent(event.getEventAction());

        // 如果是状态操作事件，则设置待切换的游戏状态
        if (stateEventAction != null)
        {
            // 根据状态操作事件类型，设置待切换的游戏状态
            gameStateEventAction = stateEventAction;
            switch (gameStateEventAction)
            {
                case PUSH_GAME_STATE:
                    outState = ((PushGameState) event).getOutState();
                    inState = ((PushGameState) event).getInState();
                    break;

                case SET_GAME_STATE:
                    outState = ((SetGameState) event).getOutState();
                    inState = ((SetGameState) event).getInState();
                    break;

                case POP_GAME_STATE:
                    outState = ((PopGameState) event).getOutState();
                    inState = ((PopGameState) event).getInState();
                    break;

                case RESET_GAME_STATE:
                    outState = ((ResetGameState) event).getOutState();
                    inState = GameState.UNKNOWN;
                    break;

                default:
                    // *_EXECUTE 执行事件由事件循环直接切换状态栈，不在此记录待切换意图
                    outState = GameState.UNKNOWN;
                    inState = GameState.UNKNOWN;
                    LogUtils.debug(this.getClass(), "setPendingTask 忽略 *_EXECUTE 执行事件 (event): " + event.getEventAction());
                    break;

            }
            LogUtils.debug(this.getClass(), "setPendingTask 成功设置动画过度 后待实施的状态事件 " +
                "(event): " + event.getEventAction() + " (outState): " + outState + " (inState): " + inState);
        }
        else
        {
            LogUtils.debug(this.getClass(), "setPendingTask 忽略非状态操作事件 (event): " + event.getEventAction());
        }
    }

    /**
     * 执行待切换任务
     */
    public void executePendingTask ()
    {
        if (inState != GameState.UNKNOWN)
        {
            LogUtils.debug(this.getClass(), "executePendingTask 执行待切换任务 (state): " + inState);
            switch (gameStateEventAction)
            {
                case PUSH_GAME_STATE:
                    eventQueue.addEvent(new PushGameStateExecute(inState));
                    break;

                case POP_GAME_STATE:
                    eventQueue.addEvent(new PopGameStateExecute());
                    break;

                case SET_GAME_STATE:
                    eventQueue.addEvent(new SetGameStateExecute(inState));
                    break;

                case RESET_GAME_STATE:
                    eventQueue.addEvent(new ResetGameStateExecute());
                    break;

                default:
                    LogUtils.debug(this.getClass(), "executePendingTask 错误的事件操作类型 (event): " + gameStateEventAction);
                    break;
            }
            LogUtils.debug(this.getClass(), "executePendingTask 执行待切换任务完成 (state): " + inState);
        }
        else
        {
            LogUtils.debug(this.getClass(), "executePendingTask 没有设置待切换任务");
        }
    }

    // ===================================================================================================================

    /**
     * 初始化淡出
     *
     * @param animation 动画配置
     */
    public void initFadeOut (Layout layout, Animation animation)
    {
        // 先拷贝布局
        this.layout = new Layout(layout);

        // 获取淡出动画配置
        FadeOut fadeOut = animation.getFadeOutComponent();

        // 如果有淡出动画配置，则设置淡出动画配置
        if (fadeOut != null && fadeOut.isValid())
        {
            setFadeOut(fadeOut);
            LogUtils.debug(this.getClass(), "initFadeOut 设置淡出动画配置 (fadeOut): " + fadeOut);
        }
        else
        {
            finishFadingOut();
            LogUtils.debug(this.getClass(), "initFadeOut 没有设置淡出动画配置");
            return;
        }

        // 获取动画
        FadeOutObject fadeOutObject = fadeOut.getObject(outState);
        LogUtils.debug(this.getClass(), "initFadeOut 获取淡出动画配置 (fadeOutObject): " + fadeOutObject);

        // 预处理数据
        fadeOutElapsed = -1.0f;
        fadeOutStartTimeMillis = System.currentTimeMillis();
        fadeOutDuration = fadeOutObject.getDuration();
        fadeOutSynchronizationGraphicsTasks = fadeOutObject.getSynchronizationGraphicsTasks();
        fadeOutSynchronizationUiTasks = fadeOutObject.getSynchronizationUiTasks();
        fadeOutScheduleTasks = fadeOutObject.getScheduleTasks();
        fadeOutScheduleTaskIndex = 0;
        isFadingOutScheduleTask = false;
        fadeOutScheduleTaskElapsed = 0.0f;
        setFadingOut(true);
        LogUtils.error(this.getClass(), "initFadeOut 预处理数据完成");
    }

    // ===================================================================================================================

    /**
     * 执行淡出
     *
     * @param audioManager    音效管理器
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     */
    public void fadingOut (Layout refLayout, AudioManager audioManager, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        // 用时间戳计算已用时间，规避切页顿卡时 delta 突变导致动画跳帧
        fadeOutElapsed = (System.currentTimeMillis() - fadeOutStartTimeMillis) / 1000.0f;

        // 音频不打断
        audioManager.playLayout(layout);

        // 同时序任务
        fadingOutSynchronizationTasks(refLayout, graphicsManager, uiManager, delta);

        // 顺序任务
        fadingOutScheduleTasks(refLayout, graphicsManager, uiManager, delta);

        // 最后根据layout绘画
        graphicsManager.putLayout(layout, delta);

        // 淡出总时长结束，完成淡出
        if (fadeOutElapsed >= fadeOutDuration)
        {
            LogUtils.error(this.getClass(), "finishFadingOut 淡出完成 " +
                "(fadeOutElapsed): " + fadeOutElapsed + " (fadeOutDuration): " + fadeOutDuration);
            finishFadingOut();
        }
    }

    // ===================================================================================================================

    /**
     * 执行淡出同时序任务
     *
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     */
    private void fadingOutSynchronizationTasks (Layout refLayout, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        // graphics
        for (AnimationTask task : fadeOutSynchronizationGraphicsTasks)
        {
            assert task.getAnimationObject() != null;
            assert task.getAnimationAction() != null;
            if (task.getAnimationObject() instanceof GraphicsAnimationObject)
            {
                GraphicsAnimationObject graphicsAnimationObject = (GraphicsAnimationObject) task.getAnimationObject();
                AnimationAction animationAction = task.getAnimationAction();
                if (fadeOutElapsed >= task.getAnimationAction().getDelay()
                    && fadeOutElapsed <= task.getAnimationAction().getDelay() + task.getAnimationAction().getDuration())
                    fadingOutGraphicsTaskDispatcher(refLayout, graphicsAnimationObject, animationAction, graphicsManager, delta);
            }
        }

        // ui
        for (AnimationTask task : fadeOutSynchronizationUiTasks)
        {
            assert task.getAnimationObject() != null;
            assert task.getAnimationAction() != null;
            if (task.getAnimationObject() instanceof UiAnimationObject)
            {
                UiAnimationObject uiAnimationObject = (UiAnimationObject) task.getAnimationObject();
                AnimationAction animationAction = task.getAnimationAction();
                if (fadeOutElapsed >= task.getAnimationAction().getDelay()
                    && fadeOutElapsed <= task.getAnimationAction().getDelay() + task.getAnimationAction().getDuration())
                    fadingOutUiTaskDispatcher(refLayout, uiAnimationObject, animationAction, uiManager, delta);
            }
        }
    }

    /**
     * 执行淡出顺序任务
     *
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     */
    private void fadingOutScheduleTasks (Layout refLayout, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        // 存在待执行的顺序任务
        if (fadeOutScheduleTaskIndex >= fadeOutScheduleTasks.size())
            return;

        // 获取任务引用
        AnimationTask task = fadeOutScheduleTasks.get(fadeOutScheduleTaskIndex);
        assert task.getAnimationObject() != null;
        assert task.getAnimationAction() != null;

        // 未耗尽延迟
        if (!isFadingOutScheduleTask)
            if (fadeOutElapsed < fadeOutScheduleTaskElapsed + task.getAnimationAction().getDelay())
                return;

        // 执行顺序任务
        isFadingOutScheduleTask = true;

        // 任务时间耗尽
        float maxElapsed = fadeOutScheduleTaskElapsed + task.getAnimationAction().getDelay() + task.getAnimationAction().getDuration();
        if (fadeOutElapsed >= maxElapsed)
        {
            fadeOutScheduleTaskIndex++;
            isFadingOutScheduleTask = false;
            fadeOutScheduleTaskElapsed = maxElapsed;
        }

        // 执行任务
        if (task.getAnimationObject() instanceof GraphicsAnimationObject)
        {
            fadingOutGraphicsTaskDispatcher(refLayout, (GraphicsAnimationObject) task.getAnimationObject(), task.getAnimationAction(), graphicsManager, delta);
        }
        else if (task.getAnimationObject() instanceof UiAnimationObject)
        {
            fadingOutUiTaskDispatcher(refLayout, (UiAnimationObject) task.getAnimationObject(), task.getAnimationAction(), uiManager, delta);
        }
    }

    /**
     * 执行淡出图形任务分发器
     *
     * @param graphicsManager 图形管理器
     */
    private void fadingOutGraphicsTaskDispatcher (Layout refLayout,
                                                  GraphicsAnimationObject graphicsAnimationObject, AnimationAction animationAction,
                                                  GraphicsManager graphicsManager, float delta)
    {
        switch (animationAction.getActionType())
        {
            case SMOOTH_MOVE:
                fadingOutGraphicsSmoothMove(refLayout, graphicsAnimationObject, animationAction, graphicsManager, delta);
                break;

            case NONE:
            default:
                break;
        }
    }

    /**
     * 执行淡出图形平滑移动
     *
     * @param graphicsManager 图形管理器
     */
    private void fadingOutGraphicsSmoothMove (Layout refLayout,
                                              GraphicsAnimationObject graphicsAnimationObject, AnimationAction animationAction,
                                              GraphicsManager graphicsManager, float delta)
    {
        GraphicsObject target = graphicsAnimationObject.getTarget();
        String tag = target.getTag();
        GraphicsKind kind = target.getGraphicsKind();

        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) animationAction.getActionParam();
        float dx = param.getOrientationX() * param.getSpeed() * delta;
        float dy = param.getOrientationY() * param.getSpeed() * delta;

        switch (kind)
        {
            case PICTURE:
                PictureInfo pic = layout.getPictureMap().get(tag);
                if (pic != null)
                {
                    pic.setX((int) (pic.getX() + dx));
                    pic.setY((int) (pic.getY() + dy));
                }
                else
                {
                    LogUtils.debug(this.getClass(), "fadingOutGraphicsSmoothMove 找不到 Picture: " + tag);
                }
                break;

            case GIF:
                GifInfo gif = layout.getGifMap().get(tag);
                if (gif != null)
                {
                    gif.setX((int) (gif.getX() + dx));
                    gif.setY((int) (gif.getY() + dy));
                }
                else
                {
                    LogUtils.debug(this.getClass(), "fadingOutGraphicsSmoothMove 找不到 Gif: " + tag);
                }
                break;

            default:
                LogUtils.debug(this.getClass(), "fadingOutGraphicsSmoothMove 未知 GraphicsKind: " + kind);
                break;
        }
    }

    /**
     * 执行淡出UI任务分发器
     *
     * @param uiManager UI管理器
     */
    private void fadingOutUiTaskDispatcher (Layout refLayout,
                                            UiAnimationObject uiAnimationObject, AnimationAction animationAction,
                                            UiManager uiManager, float delta)
    {
        // 预处理
        if (uiAnimationObject.getInteractableObject() == null) uiAnimationObject.setInteractableObject(uiManager.findObject(uiAnimationObject.getTarget()));
        switch (animationAction.getActionType())
        {
            case SMOOTH_MOVE:
                fadingOutUiSmoothMove(refLayout, uiAnimationObject, animationAction, uiManager, delta);
                break;

            case NONE:
            default:
                break;
        }
    }

    /**
     * 执行淡出UI平滑移动
     *
     * @param uiManager UI管理器
     */
    private void fadingOutUiSmoothMove (Layout refLayout,
                                        UiAnimationObject uiAnimationObject, AnimationAction animationAction,
                                        UiManager uiManager, float delta)
    {
        InteractableObject obj = uiAnimationObject.getInteractableObject();

        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) animationAction.getActionParam();
        float dx = param.getOrientationX() * param.getSpeed() * delta;
        float dy = param.getOrientationY() * param.getSpeed() * delta;

        obj.setPosition(obj.getX() + dx, obj.getY() + dy);
    }

    // ===================================================================================================================

    /**
     * 完成淡出
     */
    public void finishFadingOut ()
    {
        // 先执行跳页任务
        executePendingTask();

        // 设置淡出完成状态
        setFadingOut(false);
        setFadedOut(true);

        LogUtils.error(this.getClass(), "finishFadingOut 淡出完成");
    }

    // ===================================================================================================================

    /**
     * 初始化淡入
     *
     * @param animation 动画配置
     */
    public void initFadeIn (Layout layout, Animation animation, UiManager uiManager)
    {
        // 先拷贝布局
        this.layout = new Layout(layout);

        // 先获取淡入动画配置
        FadeIn fadeIn = animation.getFadeInComponent();

        // 如果有淡入动画配置，则设置淡入动画配置
        if (fadeIn != null && fadeIn.isValid())
        {
            setFadeIn(fadeIn);
            LogUtils.debug(this.getClass(), "initFadeIn 设置淡入动画配置 (fadeIn): " + fadeIn);
        }
        else
        {
            finishFadingIn();
            LogUtils.debug(this.getClass(), "initFadeIn 没有设置淡入动画配置");
            return;
        }

        // 获取动画
        FadeInObject fadeInObject = fadeIn.getObject(inState);
        LogUtils.debug(this.getClass(), "initFadeIn 获取淡入动画配置 (fadeInObject): " + fadeInObject);

        // 预处理数据
        fadeInElapsed = -1.0f;
        fadeInStartTimeMillis = System.currentTimeMillis();
        fadeInDuration = fadeInObject.getDuration();
        fadeInSynchronizationGraphicsTasks = fadeInObject.getSynchronizationGraphicsTasks();
        fadeInSynchronizationUiTasks = fadeInObject.getSynchronizationUiTasks();
        fadeInScheduleTasks = fadeInObject.getScheduleTasks();
        fadeInScheduleTaskIndex = 0;
        isFadingInScheduleTask = false;
        fadeInScheduleTaskElapsed = 0.0f;
        setFadingIn(true);

        // 预偏移：把有 smooth_move 的对象提前置到起始位置（原始坐标 - 偏移量）
        preOffsetFadeInTasks(uiManager);
        LogUtils.error(this.getClass(), "initFadeIn 预处理数据完成");
    }

    /**
     * 预偏移：把淡入任务中所有 smooth_move 对象提前置到起始位置（原始坐标 - 偏移量），
     * 使淡入首次画面即从外部滑入目标位置，避免控件先显示在终点再位移的跳变。
     *
     * @param uiManager UI管理器（用于解析 UI 控件并设置预偏移位置）
     */
    private void preOffsetFadeInTasks (UiManager uiManager)
    {
        // 同时序图形任务
        for (AnimationTask task : fadeInSynchronizationGraphicsTasks)
        {
            preOffsetFadeInGraphicsTask(task);
        }
        // 同时序 UI 任务
        for (AnimationTask task : fadeInSynchronizationUiTasks)
        {
            preOffsetFadeInUiTask(task, uiManager);
        }
        // 顺序任务
        for (AnimationTask task : fadeInScheduleTasks)
        {
            preOffsetFadeInGraphicsTask(task);
            preOffsetFadeInUiTask(task, uiManager);
        }
    }

    /**
     * 预偏移单个图形任务（仅 smooth_move）
     *
     * @param task 淡入任务
     */
    private void preOffsetFadeInGraphicsTask (AnimationTask task)
    {
        if (!(task.getAnimationObject() instanceof GraphicsAnimationObject)) return;
        if (task.getAnimationAction().getActionType() != AnimationActionType.SMOOTH_MOVE) return;

        GraphicsAnimationObject graphicsAnimationObject = (GraphicsAnimationObject) task.getAnimationObject();
        GraphicsObject target = graphicsAnimationObject.getTarget();
        String tag = target.getTag();
        GraphicsKind kind = target.getGraphicsKind();
        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) task.getAnimationAction().getActionParam();
        float duration = task.getAnimationAction().getDuration();
        float offsetX = param.getOrientationX() * param.getSpeed() * duration;
        float offsetY = param.getOrientationY() * param.getSpeed() * duration;

        // 显示稿按目标类型置为「原始坐标 - 偏移量」
        switch (kind)
        {
            case PICTURE:
            {
                PictureInfo pic = this.layout.getPictureMap().get(tag);
                if (pic != null)
                {
                    pic.setX((int) (pic.getX() - offsetX));
                    pic.setY((int) (pic.getY() - offsetY));
                    LogUtils.debug(this.getClass(), "preOffsetFadeInGraphicsTask PICTURE (tag): " + tag
                        + " (offset): " + offsetX + "," + offsetY
                        + " -> (x,y): " + pic.getX() + "," + pic.getY());
                }
                break;
            }
            case GIF:
            {
                GifInfo gif = this.layout.getGifMap().get(tag);
                if (gif != null)
                {
                    gif.setX((int) (gif.getX() - offsetX));
                    gif.setY((int) (gif.getY() - offsetY));
                    LogUtils.debug(this.getClass(), "preOffsetFadeInGraphicsTask GIF (tag): " + tag
                        + " (offset): " + offsetX + "," + offsetY
                        + " -> (x,y): " + gif.getX() + "," + gif.getY());
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * 预偏移单个 UI 任务（仅 smooth_move）
     *
     * @param task      淡入任务
     * @param uiManager UI管理器（控件未缓存时按目标 findObject 获取）
     */
    private void preOffsetFadeInUiTask (AnimationTask task, UiManager uiManager)
    {
        if (!(task.getAnimationObject() instanceof UiAnimationObject)) return;
        if (task.getAnimationAction().getActionType() != AnimationActionType.SMOOTH_MOVE) return;

        UiAnimationObject uiAnimationObject = (UiAnimationObject) task.getAnimationObject();

        // 控件未缓存则按目标查找（淡入前控件应已由 addLayout 建立）
        InteractableObject obj = uiAnimationObject.getInteractableObject();
        if (obj == null)
        {
            obj = uiManager.findObject(uiAnimationObject.getTarget());
            if (obj == null) return;
            uiAnimationObject.setInteractableObject(obj);
        }

        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) task.getAnimationAction().getActionParam();
        float duration = task.getAnimationAction().getDuration();
        float offsetX = param.getOrientationX() * param.getSpeed() * duration;
        float offsetY = param.getOrientationY() * param.getSpeed() * duration;

        // 记录精确的起始/目标位置，避免淡入插值重算时原始位被预偏移污染
        float originalX = obj.getX();
        float originalY = obj.getY();
        uiAnimationObject.setStartPosition(originalX - offsetX, originalY - offsetY);
        uiAnimationObject.setTargetPosition(originalX, originalY);

        // 控件置为「原始坐标 - 偏移量」（动画起始位）
        obj.setPosition(originalX - offsetX, originalY - offsetY);
        LogUtils.debug(this.getClass(), "preOffsetFadeInUiTask 预偏移UI控件 (tag): " + uiAnimationObject.getTarget().getTag()
            + " (raw): " + originalX + "," + originalY
            + " (offset): " + offsetX + "," + offsetY
            + " -> (start): " + (originalX - offsetX) + "," + (originalY - offsetY));
    }

    // ===================================================================================================================

    /**
     * 执行淡入
     *
     * @param audioManager    音效管理器
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     */
    public void fadingIn (Layout refLayout, AudioManager audioManager, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        // 用时间戳计算已用时间，规避切页顿卡时 delta 突变导致动画跳帧
        fadeInElapsed = (System.currentTimeMillis() - fadeInStartTimeMillis) / 1000.0f;

        // 音频不打断
        audioManager.playLayout(refLayout);

        // 同时序任务
        fadingInSynchronizationTasks(refLayout, graphicsManager, uiManager, delta);

        // 顺序任务
        fadingInScheduleTasks(refLayout, graphicsManager, uiManager, delta);

        // 最后根据layout绘画
        graphicsManager.putLayout(layout, delta);

        // 淡入总时长结束，完成淡入
        if (fadeInElapsed >= fadeInDuration)
        {
            LogUtils.debug(this.getClass(), "finishFadingIn 淡入完成 " +
                "(fadeInElapsed): " + fadeInElapsed + " (fadeInDuration): " + fadeInDuration);
            finishFadingIn();
        }
    }

    /**
     * 执行淡入同时序任务
     *
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     * @param delta           帧时间
     */
    private void fadingInSynchronizationTasks (Layout refLayout, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        // graphics
        for (AnimationTask task : fadeInSynchronizationGraphicsTasks)
        {
            assert task.getAnimationObject() != null;
            assert task.getAnimationAction() != null;
            if (task.getAnimationObject() instanceof GraphicsAnimationObject)
            {
                GraphicsAnimationObject graphicsAnimationObject = (GraphicsAnimationObject) task.getAnimationObject();
                AnimationAction animationAction = task.getAnimationAction();
                if (fadeInElapsed >= task.getAnimationAction().getDelay()
                    && fadeInElapsed <= task.getAnimationAction().getDelay() + task.getAnimationAction().getDuration())
                    fadingInGraphicsTaskDispatcher(refLayout, graphicsAnimationObject, animationAction, graphicsManager, delta);
            }
        }

        // ui
        for (AnimationTask task : fadeInSynchronizationUiTasks)
        {
            assert task.getAnimationObject() != null;
            assert task.getAnimationAction() != null;
            if (task.getAnimationObject() instanceof UiAnimationObject)
            {
                UiAnimationObject uiAnimationObject = (UiAnimationObject) task.getAnimationObject();
                AnimationAction animationAction = task.getAnimationAction();
                if (fadeInElapsed >= task.getAnimationAction().getDelay()
                    && fadeInElapsed <= task.getAnimationAction().getDelay() + task.getAnimationAction().getDuration())
                    fadingInUiTaskDispatcher(refLayout, uiAnimationObject, animationAction, uiManager, delta);
            }
        }
    }

    /**
     * 执行淡入顺序任务
     *
     * @param graphicsManager 图形管理器
     * @param uiManager       UI管理器
     * @param delta           帧时间
     */
    private void fadingInScheduleTasks (Layout refLayout, GraphicsManager graphicsManager, UiManager uiManager, float delta)
    {
        if (fadeInScheduleTaskIndex >= fadeInScheduleTasks.size())
            return;

        AnimationTask task = fadeInScheduleTasks.get(fadeInScheduleTaskIndex);
        assert task.getAnimationObject() != null;
        assert task.getAnimationAction() != null;

        if (!isFadingInScheduleTask)
        {
            if (fadeInElapsed < fadeInScheduleTaskElapsed + task.getAnimationAction().getDelay())
                return;
            isFadingInScheduleTask = true;
            LogUtils.debug(this.getClass(), "淡入顺序任务 首次执行 (task): " + task + " (delay): " + task.getAnimationAction().getDelay());
        }

        // 计算任务最大已用时间
        float maxElapsed = fadeInScheduleTaskElapsed + task.getAnimationAction().getDelay()
            + task.getAnimationAction().getDuration();
        if (fadeInElapsed >= maxElapsed)
        {
            LogUtils.debug(this.getClass(), "淡入顺序任务 任务结束 (task): " + task + " (duration): " + task.getAnimationAction().getDuration());
            fadeInScheduleTaskIndex++;
            isFadingInScheduleTask = false;
            fadeInScheduleTaskElapsed = maxElapsed;
        }

        if (task.getAnimationObject() instanceof GraphicsAnimationObject)
        {
            fadingInGraphicsTaskDispatcher(refLayout, (GraphicsAnimationObject) task.getAnimationObject(),
                task.getAnimationAction(), graphicsManager, delta);
        }
        else if (task.getAnimationObject() instanceof UiAnimationObject)
        {
            fadingInUiTaskDispatcher(refLayout, (UiAnimationObject) task.getAnimationObject(),
                task.getAnimationAction(), uiManager, delta);
        }
    }

    /**
     * 执行淡入图形任务分发器
     *
     * @param refLayout               原稿布局
     * @param graphicsAnimationObject 图形动画目标
     * @param animationAction         动画动作
     * @param graphicsManager         图形管理器
     * @param delta                   帧时间
     */
    private void fadingInGraphicsTaskDispatcher (Layout refLayout,
                                                 GraphicsAnimationObject graphicsAnimationObject,
                                                 AnimationAction animationAction,
                                                 GraphicsManager graphicsManager,
                                                 float delta)
    {
        switch (animationAction.getActionType())
        {
            case SMOOTH_MOVE:
                fadingInGraphicsSmoothMove(refLayout, graphicsAnimationObject, animationAction, graphicsManager, delta);
                break;
            case NONE:
            default:
                break;
        }
    }

    /**
     * 执行淡入图形平滑移动（插值方式，从外部回到原始位置）
     *
     * @param refLayout               原稿布局（提供原始坐标）
     * @param graphicsAnimationObject 图形动画目标
     * @param animationAction         动画动作
     * @param graphicsManager         图形管理器
     * @param delta                   帧时间（本方法未使用，保留扩展）
     */
    private void fadingInGraphicsSmoothMove (Layout refLayout,
                                             GraphicsAnimationObject graphicsAnimationObject,
                                             AnimationAction animationAction,
                                             GraphicsManager graphicsManager,
                                             float delta)
    {
        GraphicsObject target = graphicsAnimationObject.getTarget();
        String tag = target.getTag();
        GraphicsKind kind = target.getGraphicsKind();

        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) animationAction.getActionParam();
        float duration = animationAction.getDuration();
        float delay = animationAction.getDelay();

        // 计算任务已用时间
        float taskStartTime = isFadingInScheduleTask ?
            fadeInScheduleTaskElapsed + delay : 0f;
        float taskElapsed = fadeInElapsed - taskStartTime;
        float progress = Math.min(1f, taskElapsed / duration);

        // 从原稿获取原始坐标（目标位置）
        float originalX, originalY;
        switch (kind)
        {
            case PICTURE:
            {
                PictureInfo refPic = refLayout.getPictureMap().get(tag);
                if (refPic == null) return;
                originalX = refPic.getX();
                originalY = refPic.getY();
                break;
            }
            case GIF:
            {
                GifInfo refGif = refLayout.getGifMap().get(tag);
                if (refGif == null) return;
                originalX = refGif.getX();
                originalY = refGif.getY();
                break;
            }
            default:
                return;
        }

        // 计算偏移量
        float offsetX = param.getOrientationX() * param.getSpeed() * duration;
        float offsetY = param.getOrientationY() * param.getSpeed() * duration;

        // 起始位置：原始位置 - 偏移量（外部）
        float startX = originalX - offsetX;
        float startY = originalY - offsetY;
        // 目标位置：原始位置
        float targetX = originalX;
        float targetY = originalY;

        // 插值计算当前位置
        float currentX = startX + (targetX - startX) * progress;
        float currentY = startY + (targetY - startY) * progress;

        // 更新显示稿
        switch (kind)
        {
            case PICTURE:
            {
                PictureInfo pic = layout.getPictureMap().get(tag);
                if (pic != null)
                {
                    pic.setX((int) currentX);
                    pic.setY((int) currentY);
                }
                else
                {
                    LogUtils.debug(this.getClass(), "fadingInGraphicsSmoothMove 找不到 Picture: " + tag);
                }
                break;
            }
            case GIF:
            {
                GifInfo gif = layout.getGifMap().get(tag);
                if (gif != null)
                {
                    gif.setX((int) currentX);
                    gif.setY((int) currentY);
                }
                else
                {
                    LogUtils.debug(this.getClass(), "fadingInGraphicsSmoothMove 找不到 Gif: " + tag);
                }
                break;
            }
            default:
                LogUtils.debug(this.getClass(), "fadingInGraphicsSmoothMove 未知 GraphicsKind: " + kind);
                break;
        }
    }

    /**
     * 执行淡入UI任务分发器
     *
     * @param refLayout         原稿布局
     * @param uiAnimationObject UI动画目标
     * @param animationAction   动画动作
     * @param uiManager         UI管理器
     * @param delta             帧时间
     */
    private void fadingInUiTaskDispatcher (Layout refLayout,
                                           UiAnimationObject uiAnimationObject,
                                           AnimationAction animationAction,
                                           UiManager uiManager,
                                           float delta)
    {
        // 确保控件已缓存
        if (uiAnimationObject.getInteractableObject() == null)
        {
            uiAnimationObject.setInteractableObject(uiManager.findObject(uiAnimationObject.getTarget()));
        }
        switch (animationAction.getActionType())
        {
            case SMOOTH_MOVE:
                fadingInUiSmoothMove(refLayout, uiAnimationObject, animationAction, uiManager, delta);
                break;
            case NONE:
            default:
                break;
        }
    }

    /**
     * 执行淡入UI平滑移动（插值方式，从外部回到原始位置）
     *
     * @param refLayout         原稿布局（仅用于占位，UI坐标由Actor自身维护）
     * @param uiAnimationObject UI动画目标
     * @param animationAction   动画动作
     * @param uiManager         UI管理器
     * @param delta             帧时间（本方法未使用，保留扩展）
     */
    private void fadingInUiSmoothMove (Layout refLayout,
                                       UiAnimationObject uiAnimationObject,
                                       AnimationAction animationAction,
                                       UiManager uiManager,
                                       float delta)
    {
        InteractableObject obj = uiAnimationObject.getInteractableObject();
        if (obj == null)
        {
            LogUtils.debug(this.getClass(), "fadingInUiSmoothMove 控件未缓存: " + uiAnimationObject.getTarget());
            return;
        }

        SmoothMoveAnimationActionParam param = (SmoothMoveAnimationActionParam) animationAction.getActionParam();
        float duration = animationAction.getDuration();
        float delay = animationAction.getDelay();

        // 计算任务已用时间
        float taskStartTime = isFadingInScheduleTask ?
            fadeInScheduleTaskElapsed + delay : 0f;
        float taskElapsed = fadeInElapsed - taskStartTime;
        float progress = Math.min(1f, taskElapsed / duration);

        // 首次执行时记录起始位置（外部位置）和目标位置（原始位置）
        if (!uiAnimationObject.hasStartPosition())
        {
            float originalX = obj.getX();
            float originalY = obj.getY();
            float offsetX = param.getOrientationX() * param.getSpeed() * duration;
            float offsetY = param.getOrientationY() * param.getSpeed() * duration;
            // 起始位置：原始位置 - 偏移量（外部）
            uiAnimationObject.setStartPosition(originalX - offsetX, originalY - offsetY);
            // 目标位置：原始位置
            uiAnimationObject.setTargetPosition(originalX, originalY);
        }

        // 插值计算当前位置
        float currentX = uiAnimationObject.getStartX()
            + (uiAnimationObject.getTargetX() - uiAnimationObject.getStartX()) * progress;
        float currentY = uiAnimationObject.getStartY()
            + (uiAnimationObject.getTargetY() - uiAnimationObject.getStartY()) * progress;

        obj.setPosition(currentX, currentY);
        LogUtils.debug(this.getClass(), "fadingInUiSmoothMove (tag): " + uiAnimationObject.getTarget().getTag()
            + " (progress): " + progress
            + " (start): " + uiAnimationObject.getStartX() + "," + uiAnimationObject.getStartY()
            + " (target): " + uiAnimationObject.getTargetX() + "," + uiAnimationObject.getTargetY()
            + " -> (cur): " + currentX + "," + currentY);
    }

    // ===================================================================================================================

    /**
     * 完成淡入
     */
    public void finishFadingIn ()
    {
        // 停止过渡效果
        stopTransition();
        LogUtils.debug(this.getClass(), "finishFadingIn 淡入完成");
    }

    // ===================================================================================================================

    /**
     * 启动过渡效果
     */
    public void startTransition ()
    {
        setFadingOut(false);
        setFadedOut(false);
        setFadingIn(false);
        setFadedIn(false);
        setReady(true);
    }

    /**
     * 停止过渡效果
     */
    public void stopTransition ()
    {
        // 正常渲染模式
        eventQueue.addEvent(new RecoverNormalRenderPipeLine());

        setFadingOut(false);
        setFadedOut(false);
        setFadingIn(false);
        setFadedIn(false);
        setReady(false);
    }

    /**
     * 强制停止过渡效果，很危险!!!
     */
    public void forceStop ()
    {
        setReady(false);
    }

    /**
     * 是否启用过渡效果
     *
     * @return 是否启用过渡效果
     */
    public void setReady (boolean ready)
    {
        this.isReady = ready;
        // TODO:后期调成设置可调
    }

    /**
     * 是否启用过渡效果
     *
     * @return 是否启用过渡效果
     */
    public boolean isReady ()
    {
        return isReady;
    }

    /**
     * 是否正在执行淡入
     *
     * @return 是否正在执行淡入
     */
    public boolean isFadingOut ()
    {
        return isFadingOut;
    }

    /**
     * 设置淡入状态
     *
     * @param isFadingOut 是否正在执行淡出
     */
    public void setFadingOut (boolean isFadingOut)
    {
        this.isFadingOut = isFadingOut;
    }

    /**
     * 是否已完成淡入
     *
     * @return 是否已完成淡入
     */
    public boolean isFadedOut ()
    {
        return fadedOut;
    }

    /**
     * 设置淡入完成状态
     *
     * @param fadedOut 是否已完成淡入
     */
    public void setFadedOut (boolean fadedOut)
    {
        this.fadedOut = fadedOut;
    }

    /**
     * 是否正在执行淡出
     *
     * @return 是否正在执行淡出
     */
    public boolean isFadingIn ()
    {
        return isFadingIn;
    }

    /**
     * 设置淡出状态
     *
     * @param isFadingIn 是否正在执行淡出
     */
    public void setFadingIn (boolean isFadingIn)
    {
        this.isFadingIn = isFadingIn;
    }

    /**
     * 是否已完成淡出
     *
     * @return 是否已完成淡出
     */
    public boolean isFadedIn ()
    {
        return fadedIn;
    }

    /**
     * 设置淡出完成状态
     *
     * @param fadedIn 是否已完成淡出
     */
    public void setFadedIn (boolean fadedIn)
    {
        this.fadedIn = fadedIn;
    }

    // ===================================================================================================================

    /**
     * 获取淡出动画配置
     */
    public FadeOut getFadeOut ()
    {
        return fadeOut;
    }

    /**
     * 设置淡出动画配置
     *
     * @param fadeOut 淡出动画配置
     */
    public void setFadeOut (FadeOut fadeOut)
    {
        this.fadeOut = fadeOut;
    }

    /**
     * 获取淡入动画配置
     */
    public FadeIn getFadeIn ()
    {
        return fadeIn;
    }

    /**
     * 设置淡入动画配置
     *
     * @param fadeIn 淡入动画配置
     */
    public void setFadeIn (FadeIn fadeIn)
    {
        this.fadeIn = fadeIn;
    }

    // ===================================================================================================================

    /**
     * 获取待切换的游戏状态
     */
    public GameStateEventAction getGameStateEventAction ()
    {
        return gameStateEventAction;
    }

    /**
     * 设置待切换的游戏状态
     *
     * @param inState 待切换的游戏状态
     */
    public void setInState (GameState inState)
    {
        this.inState = inState;
    }

    /**
     * 获取待切换的游戏状态
     */
    public GameState getInState ()
    {
        return inState;
    }
}
