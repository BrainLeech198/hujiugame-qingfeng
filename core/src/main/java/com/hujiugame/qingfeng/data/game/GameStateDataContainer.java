package com.hujiugame.qingfeng.data.game;

import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.game.GameState;

public final class GameStateDataContainer
{
    private final GameState gameState;
    private final Layout layout;
    private final JsonEntity configJson;

    /**
     * 创建游戏状态数据容器
     *
     * @param gameState  游戏状态
     * @param layout   布局配置
     */
    public GameStateDataContainer (GameState gameState,
                                   Layout layout,
                                   JsonEntity configJson
    )
    {
        this.gameState = gameState;
        this.layout = layout;
        this.configJson = configJson;
    }

    /**
     * 获取游戏状态
     */
    public GameState getGameState ()
    {
        return gameState;
    }

    /**
     * 获取布局配置
     */
    public Layout getLayoutConfig ()
    {
        return layout;
    }

    /**
     * 获取配置数据
     */
    public JsonEntity getConfigJson ()
    {
        return configJson;
    }
}
