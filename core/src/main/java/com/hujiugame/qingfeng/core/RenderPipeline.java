package com.hujiugame.qingfeng.core;

import com.hujiugame.qingfeng.data.game.GameStateDataContainer;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.scene.GameRender;
import com.hujiugame.qingfeng.scene.GameRenderRegistry;
import com.hujiugame.qingfeng.type.game.GameRenderPipeLineState;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.function.Consumer;

public final class RenderPipeline
{
    private final GameRenderRegistry registry;
    private final Consumer<Float> inputUpdater;

    private GameRenderPipeLineState state;
    private GameRender gameRender;

    public RenderPipeline (GameRenderRegistry registry, Consumer<Float> inputUpdater)
    {
        this.registry = registry;
        this.inputUpdater = inputUpdater;
        this.state = GameRenderPipeLineState.NORMAL;
    }

    /**
     * 获取渲染器状态
     *
     * @return 渲染器状态
     */
    public GameRenderPipeLineState getState ()
    {
        return state;
    }

    /**
     * 设置渲染器状态
     *
     * @param state 渲染器状态
     */
    public void setState (GameRenderPipeLineState state)
    {
        this.state = state;
    }

    /**
     * 初始化渲染器，清空当前渲染引用
     *
     * @param playLocalData 游戏数据容器
     * @return 是否初始化成功
     */
    public boolean init (PlayLocalData playLocalData)
    {
        try
        {
            this.gameRender = null;
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(RenderPipeline.class, "init", e);
            return false;
        }
    }

    // ===================================================================================================================

    /**
     * 根据状态结构从注册表获取对应的渲染机并初始化
     *
     * @param gameStateDataContainer 游戏状态数据容器
     * @return 是否成功获取并初始化渲染机
     */
    public boolean register (GameStateDataContainer gameStateDataContainer)
    {
        try
        {
            LogUtils.debug(RenderPipeline.class, "register 开始创建游戏渲染机");

            // 创建游戏渲染机
            this.gameRender = registry.get(gameStateDataContainer.getGameState());
            LogUtils.debug(RenderPipeline.class, "register 获取游戏渲染机成功");

            // 初始化游戏渲染机
            if (this.gameRender != null)
            {
                this.gameRender.init_(gameStateDataContainer);
                LogUtils.debug(RenderPipeline.class, "register 游戏渲染机初始化成功" +
                    " (gameState): " + gameStateDataContainer.getGameState());
                return true;
            }
            else
            {
                LogUtils.error(RenderPipeline.class, "register 游戏渲染机为null" +
                    " (gameState): " + gameStateDataContainer.getGameState());
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(RenderPipeline.class, "register", e);
            return false;
        }
    }

    // ===================================================================================================================

    /**
     * 调用当前渲染机的帧更新（包含输入处理）
     * <p>
     * 性能：每帧调用 gameRender.update 与 inputUpdater（输入处理），是帧逻辑更新链的入口，其耗时全部计入帧耗时。
     * 单帧内只应做必要的更新推进，避免在更新链中插入加载、解析等一次性重活。
     *
     * @param deltaTime 距上一帧的时间差
     */
    public void update (float deltaTime)
    {
        try
        {
            // 调用当前渲染机的帧更新（包含输入处理）
            if (gameRender != null)
            {
                switch (state)
                {
                    // 调用当前渲染机的帧更新
                    case NORMAL:
                        updateNormal(deltaTime);
                        break;

                    case TRANSITION:
                        updateFading(deltaTime);
                        break;

                    default:
                        break;
                }
            }
            else
            {
                LogUtils.error(RenderPipeline.class, "update 游戏渲染机为null");
            }
        }
        catch (Exception e)
        {
            LogUtils.error(RenderPipeline.class, "update", e);
            throw e;
        }
    }

    /**
     * 渲染机正常更新
     *
     * @param deltaTime 距上一帧的时间差
     */
    private void updateNormal (float deltaTime)
    {
        // 调用当前渲染机的帧更新
        gameRender.update(deltaTime);
        if (inputUpdater != null) inputUpdater.accept(deltaTime); // 调用输入处理
    }

    /**
     * 渲染机 fading 更新
     *
     * @param deltaTime 距上一帧的时间差
     */
    private void updateFading (float deltaTime)
    {
    }

    // ===================================================================================================================

    /**
     * 渲染当前渲染机的内容
     * <p>
     * 性能：每帧提交绘制，耗时直接影响帧率。绘制所需的纹理、字体、批处理资源应提前加载，
     * 避免在渲染路径内做纹理解码、资源创建等重活。
     *
     * @param deltaTime 距上一帧的时间差
     */
    public void render (float deltaTime)
    {
        try
        {
            // 判断渲染机是否为空
            if (gameRender != null)
            {
                // 根据渲染机状态调用不同的渲染方法
                switch (state)
                {
                    case NORMAL:
                        renderNormal(deltaTime);
                        break;

                    case TRANSITION:
                        renderFading(deltaTime);
                        break;
                    default:
                        break;
                }
            }
            else
            {
                LogUtils.error(RenderPipeline.class, "render 游戏渲染机为null");
            }
        }
        catch (Exception e)
        {
            LogUtils.error(RenderPipeline.class, "render", e);
            throw e;
        }
    }

    /**
     * 渲染机正常渲染
     *
     * @param deltaTime 距上一帧的时间差
     */
    private void renderNormal (float deltaTime)
    {
        // 调用当前渲染机的渲染
        gameRender.render(deltaTime);
    }

    /**
     * 渲染机 fading 渲染
     *
     * @param deltaTime 距上一帧的时间差
     */
    private void renderFading (float deltaTime)
    {
        // 调用过度动画
        gameRender.transitionRender(deltaTime);
    }

    // ===================================================================================================================

    /**
     * 销毁当前渲染机（不报错的重置版本）
     *
     * @return 始终返回 true
     */
    public boolean clear ()
    {
        try
        {
            if (gameRender != null)
            {
                gameRender.dispose();
                gameRender = null;
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.debug(RenderPipeline.class, "clear 删除渲染机出现意外 问题不大", e);
            return true;
        }
    }

    // ===================================================================================================================

    /**
     * 销毁当前渲染机并释放资源
     *
     * @return 是否销毁成功
     */
    public boolean dispose ()
    {
        try
        {
            if (gameRender != null)
            {
                gameRender.dispose();
                gameRender = null;
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(RenderPipeline.class, "dispose", e);
            return false;
        }
    }
}
