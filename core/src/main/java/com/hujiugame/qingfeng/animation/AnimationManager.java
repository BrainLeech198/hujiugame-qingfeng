package com.hujiugame.qingfeng.animation;

import com.hujiugame.qingfeng.animation.component.AnimationComponent;
import com.hujiugame.qingfeng.animation.manager.TransitionManager;
import com.hujiugame.qingfeng.event.EventQueue;

/**
 * 动画执行管理器
 * <p>
 *     动画执行管理器
 * <p>
 * 当前为空壳：生命周期接口仅占位，执行逻辑待设计实现后填充。
 */
public final class AnimationManager
{
    private Animation animation;
    private TransitionManager transitionManager;

    // ===================================================================================================================

    public AnimationManager (EventQueue eventQueue)
    {
        transitionManager = new TransitionManager(eventQueue);
    }

    /**
     * 初始化动画管理器
     */
    public boolean init ()
    {
        return true;
    }

    /**
     * 设置当前动画
     *
     * @param animation 动画
     */
    public void setAnimation (Animation animation)
    {
        this.animation = animation;
    }

    /**
     * 合并动画：把传入动画（目标页）的组件合并进当前动画
     * <p>
     * 过渡期间需要同时持有当前页 fade_out 与目标页 fade_in（获取方式一致：均从 {@link #getAnimation()} 读取），
     * 切换完成后新渲染机解析出目标页动画时调用本方法合并，使一份动画同时包含两页的过渡组件。
     *
     * @param animation 目标页动画，可为空（空则忽略）
     */
    public void combineAnimation (Animation animation)
    {
        if (animation == null)
        {
            return;
        }
        if (this.animation == null)
        {
            this.animation = animation;
            return;
        }
        // 通用遍历合并目标页的动画组件到当前动画：组件自报类型（getType），同名组件覆盖为传入的
        for (AnimationComponent component : animation.getComponents().values())
        {
            if (component.isValid())
            {
                this.animation.addComponent(component);
            }
        }
    }

    /**
     * 获取当前动画
     *
     * @return 动画
     */
    public Animation getAnimation ()
    {
        return animation;
    }

    /**
     * 设置过渡管理器
     *
     * @param transitionManager 过渡管理器
     */
    public void setTransitionManager (TransitionManager transitionManager)
    {
        this.transitionManager = transitionManager;
    }

    /**
     * 获取过渡管理器
     *
     * @return 过渡管理器
     */
    public TransitionManager getTransitionManager ()
    {
        return transitionManager;
    }

    /**
     * 销毁动画管理器，清空动画状态与引用
     */
    public void dispose ()
    {
        animation = null;
        transitionManager = null;
    }
}
