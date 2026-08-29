package com.hujiugame.qingfeng.engine;

import com.hujiugame.qingfeng.animation.AnimationManager;
import com.hujiugame.qingfeng.audio.AudioManager;
import com.hujiugame.qingfeng.graphic.GraphicsManager;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 引擎上下文：打包 AudioManager、GraphicsManager、AnimationManager，
 * 统一生命周期管理，简化参数传递
 */
public final class EngineContext
{
    private AudioManager audioManager;
    private GraphicsManager graphicsManager;
    private AnimationManager animationManager;

    /**
     * 创建空的引擎上下文（后续通过 setter 注入各管理器）
     */
    public EngineContext ()
    {
    }

    /**
     * 创建引擎上下文并注入所有管理器
     *
     * @param audioManager     音频管理器
     * @param graphicsManager  图形管理器
     * @param animationManager 动画管理器
     */
    public EngineContext (AudioManager audioManager,
                          GraphicsManager graphicsManager,
                          AnimationManager animationManager)
    {
        this.audioManager = audioManager;
        this.graphicsManager = graphicsManager;
        this.animationManager = animationManager;
    }

    // ===================================================================================================================
    // Getters & Setters
    // ===================================================================================================================

    public AudioManager getAudioManager ()
    {
        return audioManager;
    }

    public void setAudioManager (AudioManager audioManager)
    {
        this.audioManager = audioManager;
    }

    public GraphicsManager getGraphicsManager ()
    {
        return graphicsManager;
    }

    public void setGraphicsManager (GraphicsManager graphicsManager)
    {
        this.graphicsManager = graphicsManager;
    }

    public AnimationManager getAnimationManager ()
    {
        return animationManager;
    }

    public void setAnimationManager (AnimationManager animationManager)
    {
        this.animationManager = animationManager;
    }

    // ===================================================================================================================
    // 销毁
    // ===================================================================================================================

    /**
     * 销毁引擎上下文中的所有管理器资源
     *
     * @return 是否全部销毁成功
     */
    public boolean dispose ()
    {
        try
        {
            if (animationManager != null)
            {
                animationManager.dispose();
                LogUtils.debug(EngineContext.class, "dispose animation销毁成功");
            }

            if (graphicsManager != null)
            {
                if (!graphicsManager.dispose())
                {
                    LogUtils.error(EngineContext.class, "dispose graphics销毁失败");
                    return false;
                }
                LogUtils.debug(EngineContext.class, "dispose graphics销毁成功");
            }

            if (audioManager != null)
            {
                if (!audioManager.dispose())
                {
                    LogUtils.error(EngineContext.class, "dispose audio销毁失败");
                    return false;
                }
                LogUtils.debug(EngineContext.class, "dispose audio销毁成功");
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(EngineContext.class, "dispose", e);
            return false;
        }
    }
}
