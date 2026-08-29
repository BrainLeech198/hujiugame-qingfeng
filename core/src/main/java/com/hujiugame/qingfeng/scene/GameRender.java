package com.hujiugame.qingfeng.scene;

import com.hujiugame.qingfeng.data.game.GameStateDataContainer;

/**
 * 游戏渲染器接口，定义每个游戏状态的渲染生命周期。
 */
public interface GameRender
{
    /**
     * 初始化渲染器，注入游戏状态数据。
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    void init_ (GameStateDataContainer gameStateDataContainer);

    /**
     * 获取游戏状态数据容器。
     *
     * @return 游戏状态数据容器
     */
    GameStateDataContainer getGameStateDataContainer ();

    /**
     * 更新渲染器的逻辑状态。
     *
     * @param deltaTime 距上一帧的时间差（秒）
     */
    void update (float deltaTime);

    /**
     * 渲染当前帧的图形内容。
     *
     * @param deltaTime 距上一帧的时间差（秒）
     */
    void render (float deltaTime);

    /**
     * 过渡渲染，用于状态切换时的过渡效果。
     *
     * @param deltaTime 距上一帧的时间差（秒）
     */
    void transitionRender (float deltaTime);

    /**
     * 销毁渲染器，释放所有资源。
     */
    void dispose ();
}
