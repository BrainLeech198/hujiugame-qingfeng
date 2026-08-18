package com.hujiugame.qingfeng.scene;

import com.hujiugame.qingfeng.data.game.GameStateDataContainer;

/**
 * 游戏渲染器抽象基类
 * <p>
 * 统一持有游戏状态数据容器（init 注入）并提供 {@link #getGameStateDataContainer()}，
 * 消除各渲染机重复的字段声明、init 保存与 getter 实现。
 * init 为 final：先保存数据容器，再调用子类 {@link #onInit(GameStateDataContainer)} 钩子完成页面初始化。
 */
public abstract class AbstractGameRender implements GameRender
{
    /** 游戏状态数据容器（init 时注入） */
    protected GameStateDataContainer gameStateDataContainer;

    /**
     * 初始化渲染器：保存数据容器后交由子类钩子完成页面初始化
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    @Override
    public final void init (GameStateDataContainer gameStateDataContainer)
    {
        this.gameStateDataContainer = gameStateDataContainer;
        onInit(gameStateDataContainer);
    }

    /**
     * 获取游戏状态数据容器
     *
     * @return 游戏状态数据容器
     */
    @Override
    public GameStateDataContainer getGameStateDataContainer ()
    {
        return gameStateDataContainer;
    }

    /**
     * 子类初始化钩子：基类已保存数据容器，子类在此完成页面初始化
     *
     * @param gameStateDataContainer 游戏状态数据容器
     */
    protected abstract void onInit (GameStateDataContainer gameStateDataContainer);
}
