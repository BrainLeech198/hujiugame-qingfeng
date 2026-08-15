package com.hujiugame.qingfeng.scene;

import com.hujiugame.qingfeng.type.game.GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 游戏渲染器注册中心，管理和缓存不同游戏状态的渲染器实例。
 */
public final class GameRenderRegistry
{
    private final Map<GameState, Supplier<GameRender>> factories = new HashMap<>();

    /**
     * 注册指定游戏状态的渲染器工厂。
     *
     * @param state   游戏状态
     * @param factory 渲染器工厂，用于创建 GameRender 实例
     */
    public void register (GameState state, Supplier<GameRender> factory)
    {
        factories.put(state, factory);
    }

    /**
     * 获取指定游戏状态对应的渲染器（优先返回缓存的实例）。
     *
     * @param state 游戏状态
     * @return 渲染器实例，未注册时返回 null
     */
    @javax.annotation.Nullable
    public GameRender get (GameState state)
    {
        Supplier<GameRender> factory = factories.get(state);
        if (factory == null) return null;

        return factory.get();
    }
}
