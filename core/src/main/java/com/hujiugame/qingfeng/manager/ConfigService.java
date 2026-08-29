package com.hujiugame.qingfeng.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.hujiugame.qingfeng.data.play.PlayLocalData;
import com.hujiugame.qingfeng.game.GameUserConfigManager;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.util.system.CrashUtils;
import com.hujiugame.qingfeng.util.system.FileUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;

/**
 * 统一配置服务：管理启动器与游戏的 UserConfig → Theme → Language 初始化链路
 */
public final class ConfigService
{
    private UserConfigManager userConfigManager;
    private ThemeManager themeManager;
    private LanguageManager languageManager;

    private GameUserConfigManager gameUserConfigManager;

    private static final int USER_CONFIG_RETRY_COUNT = 5;
    private static final int USER_CONFIG_REPAIR_COUNT = 5;

    /**
     * 绑定启动器配置管理器引用
     *
     * @param userConfigManager 用户配置管理器
     * @param themeManager      主题管理器
     * @param languageManager   语言管理器
     * @return 是否绑定成功
     */
    public boolean init (UserConfigManager userConfigManager,
                         ThemeManager themeManager,
                         LanguageManager languageManager)
    {
        try
        {
            this.userConfigManager = userConfigManager;
            this.themeManager = themeManager;
            this.languageManager = languageManager;

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(ConfigService.class, "init", e);
            return false;
        }
    }

    // ===================================================================================================================
    // 启动器配置
    // ===================================================================================================================

    /**
     * 加载启动器配置：读取 user_config.json（含重试修复），然后初始化语言和主题
     *
     * @return 是否加载成功
     */
    public boolean loadLauncherConfig ()
    {
        try
        {
            // 用户设置读取
            FileHandle userConfigPathHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET, FileName.USER_CONFIG));
            int configInitialRetryCount = 0;
            while (!userConfigManager.init(userConfigPathHandle))
            {
                // 记录重试次数
                configInitialRetryCount++;
                if (configInitialRetryCount >= USER_CONFIG_RETRY_COUNT)
                {
                    LogUtils.error(ConfigService.class, "loadLauncherConfig 读取用户设置重试次数已达上限，准备崩溃");
                    CrashUtils.crash(new RuntimeException("loadLauncherConfig 用户配置初始化失败，已重试" + configInitialRetryCount + "次"));
                    return false;
                }
                LogUtils.error(ConfigService.class, "loadLauncherConfig 读取用户设置失败 第" + configInitialRetryCount + "次重试");

                // 尝试修复
                int configRepairCount = 0;
                while (!userConfigManager.repair())
                {
                    // 记录重试次数
                    configRepairCount++;
                    if (configRepairCount >= USER_CONFIG_REPAIR_COUNT)
                    {
                        LogUtils.error(ConfigService.class, "loadLauncherConfig 修复用户设置重试次数已达上限，准备崩溃");
                        CrashUtils.crash(new RuntimeException("loadLauncherConfig 用户配置修复失败，已重试" + configRepairCount + "次"));
                        return false;
                    }
                    LogUtils.error(ConfigService.class, "loadLauncherConfig 尝试修复用户设置失败 第" + configRepairCount + "次重试");
                }
                LogUtils.debug(ConfigService.class, "loadLauncherConfig 尝试修复用户设置成功");
            }
            LogUtils.debug(ConfigService.class, "loadLauncherConfig 读取用户设置成功");

            // 根据用户设置 读取语言设置
            String languagePathName = userConfigManager.getLanguage();
            FileHandle languageConfigPathHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET_S_LANGUAGE));
            if (!languageManager.init(languagePathName, languageConfigPathHandle, true, userConfigManager))
            {
                LogUtils.error(ConfigService.class, "loadLauncherConfig 读取语言文件失败");
                return false;
            }
            else
            {
                LogUtils.debug(ConfigService.class, "loadLauncherConfig 读取语言文件成功");
            }

            // 根据用户数据 读取主题设置
            String themePathName = userConfigManager.getTheme();
            FileHandle themeConfigPathHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET_S_THEME));
            if (!themeManager.init(themePathName, themeConfigPathHandle, true, userConfigManager))
            {
                LogUtils.error(ConfigService.class, "loadLauncherConfig 读取主题信息失败");
                return false;
            }
            else
            {
                LogUtils.debug(ConfigService.class, "loadLauncherConfig 读取主题信息成功");
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(ConfigService.class, "loadLauncherConfig", e);
            return false;
        }
    }

    // ===================================================================================================================
    // 游戏配置
    // ===================================================================================================================

    /**
     * 加载游戏配置：创建 GameUserConfigManager，初始化语言和主题管理器，并存入 PlayLocalData
     *
     * @param gamePathHandle 游戏路径句柄
     * @param gameId         游戏ID
     * @param playLocalData  游戏数据内容（接收创建的管理器）
     * @return 是否加载成功
     */
    public boolean loadGameConfig (FileHandle gamePathHandle, String gameId, PlayLocalData playLocalData)
    {
        try
        {
            // 加载用户游戏偏好设置
            GameUserConfigManager gameUserConfigManager = new GameUserConfigManager();
            if (!gameUserConfigManager.init(gamePathHandle, gameId))
            {
                LogUtils.error(ConfigService.class, "loadGameConfig 加载用户游戏偏好json失败 (path): " + gamePathHandle);
                return false;
            }
            else
            {
                this.gameUserConfigManager = gameUserConfigManager;
                playLocalData.setGameUserConfigManager(gameUserConfigManager);
                LogUtils.debug(ConfigService.class, "loadGameConfig 加载用户游戏偏好json成功 (path): " + gamePathHandle);
            }

            // 加载使用的语言
            String languagePathName = gameUserConfigManager.getLanguage();
            FileHandle languageConfigPathHandle = gamePathHandle.child(PathName.IN_GAME_ASSET_S_LANGUAGE);
            LanguageManager gameLanguageManager = new LanguageManager();
            if (!gameLanguageManager.init(languagePathName, languageConfigPathHandle, false, null))
            {
                LogUtils.error(ConfigService.class, "loadGameConfig 加载语言失败 (language): " + languagePathName);
                return false;
            }
            else
            {
                playLocalData.setLanguageManager(gameLanguageManager);
                LogUtils.debug(ConfigService.class, "loadGameConfig 加载语言成功 (language): " + languagePathName);
            }

            // 加载使用的主题
            String themePathName = gameUserConfigManager.getTheme();
            FileHandle themeConfigPathHandle = gamePathHandle.child(PathName.IN_GAME_ASSET_S_THEME);
            ThemeManager gameThemeManager = new ThemeManager();
            if (!gameThemeManager.init(themePathName, themeConfigPathHandle, false, null))
            {
                LogUtils.error(ConfigService.class, "loadGameConfig 加载主题失败 (theme): " + themePathName);
                return false;
            }
            else
            {
                playLocalData.setThemeManager(gameThemeManager);
                LogUtils.debug(ConfigService.class, "loadGameConfig 加载主题成功 (theme): " + themePathName);
            }

            LogUtils.debug(ConfigService.class, "loadGameConfig 加载用户游戏偏好设置成功");
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(ConfigService.class, "loadGameConfig", e);
            return false;
        }
    }

    /**
     * 获取游戏用户配置管理器（loadGameConfig 成功后可用）
     *
     * @return 游戏用户配置管理器
     */
    @javax.annotation.Nullable
    public GameUserConfigManager getGameUserConfigManager ()
    {
        return gameUserConfigManager;
    }

    /**
     * 销毁游戏配置，重置游戏用户配置管理器及 PlayLocalData 中的配置管理器
     *
     * @param playLocalData 游戏数据内容
     * @return 销毁是否成功
     */
    public boolean disposeGameConfig (PlayLocalData playLocalData)
    {
        try
        {
            gameUserConfigManager = null;
            playLocalData.setThemeManager(null);
            playLocalData.setLanguageManager(null);
            playLocalData.setGameUserConfigManager(null);

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(ConfigService.class, "disposeGameConfig", e);
            return false;
        }
    }

    // ===================================================================================================================
    // 销毁
    // ===================================================================================================================

    /**
     * 销毁配置服务，清空所有引用
     *
     * @return 是否销毁成功
     */
    public boolean dispose ()
    {
        try
        {
            userConfigManager = null;
            themeManager = null;
            languageManager = null;
            gameUserConfigManager = null;

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(ConfigService.class, "dispose", e);
            return false;
        }
    }
}
