package com.hujiugame.qingfeng.game;

import com.badlogic.gdx.files.FileHandle;
import com.hujiugame.qingfeng.core.SceneStack;
import com.hujiugame.qingfeng.data.story.tree.BranchStructure;
import com.hujiugame.qingfeng.data.story.tree.TreeStructure;
import com.hujiugame.qingfeng.data.story.tree.TreeStructureInfo;
import com.hujiugame.qingfeng.game.loader.GamePlayDataLoader;
import com.hujiugame.qingfeng.game.loader.GameResourceLoader;
import com.hujiugame.qingfeng.manager.ConfigService;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.data.play.Player;
import com.hujiugame.qingfeng.data.story.Role;
import com.hujiugame.qingfeng.type.game.GameState;
import com.hujiugame.qingfeng.type.play.Hoster;
import com.hujiugame.qingfeng.event.imp.game.EnterGame;
import com.hujiugame.qingfeng.event.imp.game.PlayGame;
import com.hujiugame.qingfeng.event.imp.state.PushGameState;
import com.hujiugame.qingfeng.event.imp.game.QuitGame;
import com.hujiugame.qingfeng.event.EventQueue;
import com.hujiugame.qingfeng.manager.LanguageManager;
import com.hujiugame.qingfeng.util.json.parser.JsonTextParser;
import com.hujiugame.qingfeng.util.system.LogUtils;

public final class GameSessionManager
{
    private final ConfigService configService;
    private final GameResourceLoader resourceLoader;
    private final GamePlayDataLoader dataLoader;
    private final EventQueue eventQueue;
    private final GameLogicService gameLogicService;
    private final GameInfoManager gameInfoManager;
    private final PlayLocalData playLocalData;
    private final SceneStack sceneStack;

    /** 进入游戏会话前保存的启动器 LanguageManager，退出时恢复 */
    private LanguageManager launcherLanguageManager;

    /**
     * 构造游戏会话管理器
     *
     * @param configService    配置服务
     * @param resourceLoader   资源加载器
     * @param dataLoader       数据加载器
     * @param eventQueue     事件队列
     * @param gameLogicService        游戏逻辑
     * @param gameInfoManager  游戏信息管理器
     * @param playLocalData  游戏数据内容
     * @param sceneStack 场景栈
     */
    public GameSessionManager (ConfigService configService,
                               GameResourceLoader resourceLoader,
                               GamePlayDataLoader dataLoader,
                               EventQueue eventQueue,
                               GameLogicService gameLogicService,
                               GameInfoManager gameInfoManager,
                               PlayLocalData playLocalData,
                               SceneStack sceneStack)
    {
        this.configService = configService;
        this.resourceLoader = resourceLoader;
        this.dataLoader = dataLoader;
        this.eventQueue = eventQueue;
        this.gameLogicService = gameLogicService;
        this.gameInfoManager = gameInfoManager;
        this.playLocalData = playLocalData;
        this.sceneStack = sceneStack;
    }

    /**
     * 加载游戏，检查目录结构并切换到加载界面
     *
     * @param gamePathDirectory 游戏目录文件句柄
     * @param nowGameState 当前游戏状态
     * @return 加载是否成功
     */
    public boolean loadGame (FileHandle gamePathDirectory, GameState nowGameState)
    {
        try
        {
            LogUtils.debug(GameSessionManager.class, "loadGame 尝试加载游戏 (path): " + gamePathDirectory.path());

            // 校验游戏逻辑结构
            if (!gameLogicService.checkGameDirectory(gamePathDirectory))
            {
                LogUtils.error(GameSessionManager.class, "loadGame 游戏目录结构错误 (path): " + gamePathDirectory.path());
                return false;
            }
            else
            {
                LogUtils.debug(GameSessionManager.class, "loadGame 游戏目录结构正确 (path): " + gamePathDirectory.path());
            }

            // 存储游戏基础信息
            playLocalData.setGamePathHandle(gamePathDirectory);

            // 进入加载页面
            eventQueue.addEvent(new PushGameState(nowGameState, GameState.MENU_LOAD));

            LogUtils.debug(GameSessionManager.class, "loadGame 成功进入加载界面 (path): " + gamePathDirectory.path());
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "loadGame", e);
            return false;
        }
    }

    /**
     * 进入游戏，加载用户配置、资源和数据，并切换到游戏菜单状态
     */
    public void enterGame ()
    {
        try
        {
            // 创建玩家数据
            Player player = new Player(gameInfoManager);
            playLocalData.setPlayer(player);

            // 提取并解析游戏信息
            FileHandle gamePathHandle = playLocalData.getGamePathHandle();
            JsonEntity gameConfigJson = gameLogicService.parseGameConfig(gamePathHandle);

            String gameId = gameLogicService.parseGameId(gameConfigJson);
            String gameName = gameLogicService.parseGameName(gameConfigJson);
            String gameVersion = gameLogicService.parseGameVersion(gameConfigJson);
            int gameLauncherVersion = gameLogicService.parseGameLauncherVersion(gameConfigJson);

            // 存储游戏信息
            player.setGameId(gameId);
            player.setGameName(gameName);
            player.setGameVersion(gameVersion);
            player.setGameLauncherVersion(gameLauncherVersion);

            LogUtils.info(GameSessionManager.class, "enterGame 存储游戏信息 (path): " + gamePathHandle);

            // 加载用户游戏偏好设置
            if (!configService.loadGameConfig(gamePathHandle, gameId, playLocalData))
            {
                LogUtils.error(GameSessionManager.class, "enterGame 加载用户游戏偏好设置失败");
                return;
            }

            // 切换 JsonTextParser 到游戏的语言管理器，并注入 GameInfoManager
            launcherLanguageManager = JsonTextParser.getLanguageManager();
            LanguageManager gameLanguageManager = playLocalData.getLanguageManager();
            gameLanguageManager.setGameInfoManager(gameInfoManager);
            JsonTextParser.setLanguageManager(gameLanguageManager);

            // 加载游戏资源
            if (!resourceLoader.loadResource(playLocalData.getThemeManager()))
            {
                LogUtils.error(GameSessionManager.class, "enterGame 加载游戏资源失败");
                // loadResource 失败时回滚语言管理器切换和已切换的用户配置（语言、主题）
                rollbackLanguageManager();
                configService.disposeGameConfig(playLocalData);
                return;
            }

            // 读取游戏数据
            if (!dataLoader.loadData(gamePathHandle))
            {
                LogUtils.error(GameSessionManager.class, "enterGame 读取游戏数据失败");
                // loadData 失败时按 LIFO 回滚已加载的资源和用户配置
                dataLoader.disposeData();
                resourceLoader.disposeResource();
                rollbackLanguageManager();
                configService.disposeGameConfig(playLocalData);
                return;
            }

            // 进入游戏
            eventQueue.addEvent(new EnterGame(gamePathHandle, gameId, gameName, gameVersion, gameLauncherVersion));

            LogUtils.info(GameSessionManager.class, "enterGame 进入游戏 (name): " + gameName + " (id): " + gameId + " (path): " + gamePathHandle);
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "enterGame", e);
            // enterGame 异常时按 LIFO 回滚所有已加载阶段（dispose 方法幂等，安全重复调用）
            dataLoader.disposeData();
            resourceLoader.disposeResource();
            rollbackLanguageManager();
            configService.disposeGameConfig(playLocalData);
        }
    }

    /**
     * 退出游戏，依次释放数据、资源和用户配置，并重置游戏状态
     */
    public void quitGame ()
    {
        try
        {
            // 恢复启动器语言管理器
            if (launcherLanguageManager != null)
            {
                JsonTextParser.setLanguageManager(launcherLanguageManager);
                launcherLanguageManager = null;
            }

            // 重置游戏状态
            eventQueue.addEvent(new QuitGame());

            // 释放游戏数据
            if (!dataLoader.disposeData())
            {
                LogUtils.error(GameSessionManager.class, "quitGame 释放游戏数据失败");
                return;
            }

            // 释放游戏资源
            if (!resourceLoader.disposeResource())
            {
                LogUtils.error(GameSessionManager.class, "quitGame 释放游戏资源失败");
                return;
            }

            // 释放用户游戏偏好设置
            if (!configService.disposeGameConfig(playLocalData))
            {
                LogUtils.error(GameSessionManager.class, "quitGame 释放用户游戏偏好设置失败");
                return;
            }

            // 退出游戏
            LogUtils.info(GameSessionManager.class, "quitGame 退出游戏" + " (path): " + playLocalData.getGamePathHandle());
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "quitGame", e);
        }
    }

    /**
     * 检查当前是否处于游戏中状态
     * @return 游戏中则返回true，否则返回false
     */
    public boolean isInGame ()
    {
        return sceneStack.getGameState().isInGame();
    }

    /**
     * 开始新游戏剧情，设置主持人、角色、起始树根和起始页面
     * @param hoster 主持人对象
     * @param role 角色对象
     * @return 开始剧情是否成功
     */
    public boolean playNewStory (Hoster hoster, Role role)
    {
        try
        {
            // 设置游戏主持人和游玩的角色
            playLocalData.getPlayer().setHoster(hoster);
            playLocalData.getPlayer().setRole(role);

            // 读取角色剧情树
            if (playLocalData.getGameStoryManager().loadRoleStory(role))
            {
                LogUtils.debug(GameSessionManager.class, "playNewStory 读取剧情树成功 (roleId): " + role.getId());
            }
            else
            {
                LogUtils.error(GameSessionManager.class, "playNewStory 读取剧情树失败 (roleId): " + role.getId());
                return false;
            }

            // 获取角色剧情起始树根
            if (playLocalData.getPlayer().setTreeStructure(
                playLocalData.getGameStoryManager().getTreeStructure(role.getRoot())))
            {
                LogUtils.debug(GameSessionManager.class, "playNewStory 获取角色起始树根成功 (roleId): " + role.getId());
            }
            else
            {
                LogUtils.error(GameSessionManager.class, "playNewStory 获取角色起始树根失败 (roleId): " + role.getId());
                return false;
            }

            // 获取角色剧情树根的起始页面
            if (playLocalData.getPlayer().setNextPage(
                playLocalData.getGameStoryManager().getPage(
                    playLocalData.getPlayer().getTreeStructure().getNowPageId())))
            {
                LogUtils.debug(GameSessionManager.class, "playNewStory 获取起始页面成功 (roleId): " + role.getId());
            }
            else
            {
                LogUtils.error(GameSessionManager.class, "playNewStory 获取起始页面失败 (roleId): " + role.getId());
                return false;
            }

            // 创建游戏状态并切换到游戏播放状态
            eventQueue.addEvent(new PlayGame(hoster, role));

            LogUtils.info(GameSessionManager.class, "playNewStory 创建角色新游戏 (hoster): " + hoster + " (roleId): " + role.getId());
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "playNewStory", e);
            return false;
        }
    }

    /**
     * 剧情前进一页
     * @return 前进一页是否成功
     */
    public boolean storyForwardPage ()
    {
        try
        {
            TreeStructure treeStructure = playLocalData.getPlayer().getTreeStructure();

            // 剧情树类型
            if (treeStructure instanceof BranchStructure)
            {
                // 尝试获取下一页
                if (treeStructure.forwardPage())
                {
                    if (!playLocalData.getPlayer().setNextPage(playLocalData.getGameStoryManager().getPage(treeStructure.getNowPageId())))
                    {
                        LogUtils.error(GameSessionManager.class, "storyForwardPage setNextPage 失败"
                            + " (nowPageId): " + treeStructure.getNowPageId());
                        return false;
                    }
                    LogUtils.debug(GameSessionManager.class, "storyForwardPage 获取下一页成功"
                        + " (treeStructureInfo): " + treeStructure.getTreeStructureInfo() + " (nowPageId): " + treeStructure.getNowPageId());
                    return true;
                }
                else
                {
                    LogUtils.debug(GameSessionManager.class, "storyForwardPage 获取下一页失败"
                        + " (treeStructureInfo): " + treeStructure.getTreeStructureInfo() + " (nowPageId): " + treeStructure.getNowPageId());
                    return false;
                }
            }
            else
            {
                LogUtils.error(GameSessionManager.class, "storyForwardPage 获取下一页失败 不允许的树形 (treeStructureInfo): " + treeStructure.getTreeStructureInfo());
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "storyForwardPage", e);
            return false;
        }
    }

    /**
     * 跳转到指定剧情树和页面
     * @param treeStructureInfo 树结构信息
     * @param pageId 页面ID
     * @return 跳转是否成功
     */
    public boolean storyGotoPage (TreeStructureInfo treeStructureInfo, String pageId)
    {
        try
        {
            // 查找相关剧情树和页面
            TreeStructure treeStructure = playLocalData.getGameStoryManager().getTreeStructure(treeStructureInfo);
            if (treeStructure != null)
            {
                // 存在相关页面
                if (treeStructure.getPageIdList().contains(pageId))
                {
                    // 跳转
                    if (treeStructure.setNowPageId(pageId))
                    {
                        playLocalData.getPlayer().setTreeStructure(treeStructure);
                        if (!playLocalData.getPlayer().setNextPage(playLocalData.getGameStoryManager().getPage(pageId)))
                        {
                            LogUtils.error(GameSessionManager.class, "storyGotoPage setNextPage 失败 (pageId): " + pageId);
                            return false;
                        }
                        LogUtils.debug(GameSessionManager.class, "storyGotoPage 跳转成功 (treeStructureInfo): " + treeStructureInfo + " (pageId): " + pageId);
                        return true;
                    }
                    else
                    {
                        LogUtils.error(GameSessionManager.class, "storyGotoPage 跳转失败 (treeStructureInfo): " + treeStructureInfo + " (pageId): " + pageId);
                        return false;
                    }
                }
                else
                {
                    LogUtils.error(GameSessionManager.class, "storyGotoPage 该树形中不存在该页面"
                        + " (treeStructureInfo): " + treeStructureInfo + " (pageId): " + pageId);
                    return false;
                }
            }
            else
            {
                LogUtils.error(GameSessionManager.class, "storyGotoPage 获取剧情树失败 (treeStructureInfo): " + treeStructureInfo);
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(GameSessionManager.class, "storyGotoPage", e);
            return false;
        }
    }

    /**
     * 回滚语言管理器切换：恢复启动器的 LanguageManager 到 JsonTextParser
     */
    private void rollbackLanguageManager ()
    {
        if (launcherLanguageManager != null)
        {
            JsonTextParser.setLanguageManager(launcherLanguageManager);
            launcherLanguageManager = null;
        }
    }
}
