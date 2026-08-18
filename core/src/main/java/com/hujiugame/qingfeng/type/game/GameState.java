package com.hujiugame.qingfeng.type.game;

/**
 * 游戏状态枚举。
 * <p>
 * 扁平枚举，每个枚举值代表一个完整可流转状态，整合了原 {@code GameState}（主状态 int 常量）
 * 与 {@code GameSubState}（子状态 int 常量）与 {@code StateStructure}（双 int 数据类）三者。
 * 主状态本身没有独立页面，真正可流转的是 9 个叶子状态。
 * 枚举携带布局目录名、是否需要页面配置、是否处于游戏内、中文显示名等元数据，
 * 替代原 {@code GameStatePageInfo} 的两层 Map 与 {@code GameState.getGameStateName}。
 */
public enum GameState
{
    /** 未知状态 */
    UNKNOWN(null, false, false, "未知"),

    /** 启动初始化 */
    INIT(null, false, false, "初始化"),

    /** 主菜单 */
    MENU_MAIN("menu_main", true, false, "主菜单"),

    /** 游戏列表 */
    MENU_LIST("menu_list", true, false, "游戏列表"),

    /** 游戏加载 */
    MENU_LOAD("menu_load", true, false, "游戏加载"),

    /** 基础配置 */
    CONFIG_BASIC("config_basic", true, false, "基础配置"),

    /** 显示配置 */
    CONFIG_DISPLAY("config_display", true, false, "显示配置"),

    /** 游戏菜单 */
    GAME_MENU("game_menu", true, true, "游戏菜单"),

    /** 游戏角色 */
    GAME_ROLE("game_role", true, true, "游戏角色"),

    /** 游戏游玩 */
    GAME_PLAY(null, false, true, "游戏游玩");

    /** 布局目录名，null 表示该状态无需页面布局 */
    private final String layoutDirName;

    /** 是否需要页面配置文件 */
    private final boolean needConfig;

    /** 是否处于游戏内（走 playLocalData 主题与 IN_GAME 路径） */
    private final boolean inGame;

    /** 中文显示名 */
    private final String displayName;

    GameState (String layoutDirName, boolean needConfig, boolean inGame, String displayName)
    {
        this.layoutDirName = layoutDirName;
        this.needConfig = needConfig;
        this.inGame = inGame;
        this.displayName = displayName;
    }

    /**
     * 获取布局目录名，null 表示该状态无需页面布局
     */
    public String getLayoutDirName ()
    {
        return layoutDirName;
    }

    /**
     * 是否需要页面配置文件
     */
    public boolean isNeedConfig ()
    {
        return needConfig;
    }

    /**
     * 是否处于游戏内
     */
    public boolean isInGame ()
    {
        return inGame;
    }

    /**
     * 获取中文显示名
     */
    public String getDisplayName ()
    {
        return displayName;
    }
}
