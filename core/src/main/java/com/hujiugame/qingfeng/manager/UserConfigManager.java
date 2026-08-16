package com.hujiugame.qingfeng.manager;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.DeviceLanguage;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.game.GameInfoManager;
import com.hujiugame.qingfeng.type.key.ConfigKey;
import com.hujiugame.qingfeng.type.key.GameInfoKey;
import com.hujiugame.qingfeng.type.ui.UseViewport;
import com.hujiugame.qingfeng.util.system.FileUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;
import com.hujiugame.qingfeng.util.system.PlatformUtils;

public final class UserConfigManager
{
    // json文件
    private JsonEntity json;

    // 存储路径句柄
    private FileHandle pathHandle;

    // 使用的语言(文件名字)
    private String language;

    // 使用的主题(文件夹名字)
    private String theme;

    // 使用的视窗
    private UseViewport useViewport;

    // 是否全屏
    private boolean fullscreen;

    // 窗口化尺寸记忆（进入全屏前保存，退出全屏时恢复）
    private int windowedWidth = 1280;
    private int windowedHeight = 720;

    // 窗口分辨率
    private int resolutionWidth;
    private int resolutionHeight;

    // 声音音量
    private float soundVolumeTotal;
    private float soundVolumeMusic;
    private float soundVolumeSound;

    /**
     * 初始化用户配置
     *
     * @param userConfigJson 默认用户配置的JSON实体
     */
    private void initUserConfig (JsonEntity userConfigJson)
    {
        // 复制默认配置到外部
        String internalPath = FileUtils.pathJoin(PathName.ASSET, FileName.USER_CONFIG);
        FileHandle internalPathHandle = Gdx.files.internal(internalPath);
        FileUtils.copyFile(internalPathHandle, pathHandle);
        LogUtils.info(UserConfigManager.class, "parseJson 用户配置文件不存在, 已复制默认配置 (path): " + pathHandle);

        // 读取用户设置json
        userConfigJson = new JsonEntity(pathHandle);
        LogUtils.info(UserConfigManager.class, "parseJson 正在进行个性化服务配置...");

        // ================ 以下是个性化服务配置 ================

        // 首次运行：按设备语言改写默认语言，落盘后用户可手动改
        // 与默认语言相同时不写盘，避免无谓文件写入
        DeviceLanguage deviceLanguage = DeviceLanguage.detectDefault();
        String defaultLanguage = userConfigJson.getString(ConfigKey.User.LANGUAGE);
        if (!deviceLanguage.getPathName().equals(defaultLanguage))
        {
            userConfigJson.put(ConfigKey.User.LANGUAGE, deviceLanguage.getPathName());
            FileUtils.createStringFile(userConfigJson.toString(), pathHandle, false);
        }
        LogUtils.info(UserConfigManager.class, "parseJson 首次运行自动识别设备语言 (language): " + deviceLanguage.getPathName());
    }

    /**
     * 解析用户配置JSON文件，若文件不存在则从内部默认配置复制，并按设备语言改写默认语言
     *
     * @param pathHandle     配置文件的路径句柄
     * @return 解析成功返回 true，失败返回 false
     */
    private boolean parseJson (FileHandle pathHandle)
    {
        try
        {
            JsonEntity userConfigJson = new JsonEntity();

            // 检查文件存在
            if (FileUtils.isFileExist(pathHandle))
            {
                userConfigJson = new JsonEntity(pathHandle);
            }

            // 如果文件读取失败 即本次是首次运行
            if (userConfigJson.isEmpty())
            {
                // 初始化用户配置
                initUserConfig(userConfigJson);
            }

            // 存储json
            this.json = userConfigJson;
            LogUtils.debug(UserConfigManager.class, "parseJson 用户配置文件 (json): " + userConfigJson);
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "parseJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载语言配置
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadLanguageFromJson (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.LANGUAGE))
            {
                this.language = userConfigJson.getString(ConfigKey.User.LANGUAGE);
                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置缺少language字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadLanguageFromJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载主题配置
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadThemeFromJson (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.THEME))
            {
                this.theme = userConfigJson.getString(ConfigKey.User.THEME);
                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置缺少theme字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadThemeFromJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载视窗模式配置
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadUseViewportFromJson (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.USE_VIEWPORT))
            {
                this.useViewport = UseViewport.valueOf(userConfigJson.getString(ConfigKey.User.USE_VIEWPORT).toUpperCase());
                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置缺少useViewport字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadUseViewportFromJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载全屏模式配置
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadFullscreenFromJson (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.FULLSCREEN))
            {
                this.fullscreen = userConfigJson.getBoolean(ConfigKey.User.FULLSCREEN);
                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置缺少fullscreen字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadFullscreenFromJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载窗口分辨率配置（宽和高）
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadResolutionFromJson (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.Resolution.KEY))
            {
                JsonEntity resolutionJson = userConfigJson.getJsonEntityByKey(ConfigKey.User.Resolution.KEY);

                // width
                if (resolutionJson.containsKey(ConfigKey.User.Resolution.WIDTH))
                {
                    this.resolutionWidth = resolutionJson.getInt(ConfigKey.User.Resolution.WIDTH);
                }
                else
                {
                    LogUtils.error(UserConfigManager.class, "loadResolutionFromJson 用户配置缺少resolution.width字段");
                    return false;
                }

                // height
                if (resolutionJson.containsKey(ConfigKey.User.Resolution.HEIGHT))
                {
                    this.resolutionHeight = resolutionJson.getInt(ConfigKey.User.Resolution.HEIGHT);
                }
                else
                {
                    LogUtils.error(UserConfigManager.class, "loadResolutionFromJson 用户配置缺少resolution.height字段");
                    return false;
                }

                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置缺少resolution字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadResolutionFromJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载声音音量配置（总音量、音乐音量、音效音量）
     *
     * @param userConfigJson 用户配置的JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadSoundVolume (JsonEntity userConfigJson)
    {
        try
        {
            if (userConfigJson.containsKey(ConfigKey.User.Volume.KEY))
            {
                JsonEntity soundVolumeJson = userConfigJson.getJsonEntityByKey(ConfigKey.User.Volume.KEY);

                // total
                if (soundVolumeJson.containsKey(ConfigKey.User.Volume.TOTAL))
                {
                    this.soundVolumeTotal = soundVolumeJson.getFloat(ConfigKey.User.Volume.TOTAL);
                }
                else
                {
                    LogUtils.error(UserConfigManager.class, "loadSoundVolume 用户配置缺少soundVolume.total字段");
                    return false;
                }

                // music
                if (soundVolumeJson.containsKey(ConfigKey.User.Volume.MUSIC))
                {
                    this.soundVolumeMusic = soundVolumeJson.getFloat(ConfigKey.User.Volume.MUSIC);
                }
                else
                {
                    LogUtils.error(UserConfigManager.class, "loadSoundVolume 用户配置缺少soundVolume.music字段");
                    return false;
                }

                // sound
                if (soundVolumeJson.containsKey(ConfigKey.User.Volume.SOUND))
                {
                    this.soundVolumeSound = soundVolumeJson.getFloat(ConfigKey.User.Volume.SOUND);
                }
                else
                {
                    LogUtils.error(UserConfigManager.class, "loadSoundVolume 用户配置缺少soundVolume.sound字段");
                    return false;
                }

                return true;
            }
            else
            {
                LogUtils.error(UserConfigManager.class, "loadSoundVolume 用户配置缺少soundVolume字段");
                return false;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "loadSoundVolume", e);
            return false;
        }
    }

    /**
     * 初始化用户配置管理器，依次解析并加载所有配置项
     *
     * @param pathHandle     配置文件路径句柄
     * @return 初始化成功返回 true，失败返回 false
     */
    public boolean init (FileHandle pathHandle)
    {
        try
        {
            // 存储路径
            this.pathHandle = pathHandle;

            // 读取json
            if (!parseJson(pathHandle))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置json读取失败");
                return false;
            }

            // language
            if (!loadLanguageFromJson(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置language读取失败");
                return false;
            }

            // theme
            if (!loadThemeFromJson(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置theme读取失败");
                return false;
            }

            // useViewport
            if (!loadUseViewportFromJson(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置useViewport读取失败");
                return false;
            }

            // fullscreen
            if (!loadFullscreenFromJson(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置fullscreen读取失败");
                return false;
            }

            // resolution
            if (!loadResolutionFromJson(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置resolution读取失败");
                return false;
            }

            // soundVolume
            if (!loadSoundVolume(json))
            {
                LogUtils.error(UserConfigManager.class, "init 用户配置soundVolume读取失败");
                return false;
            }

            // debug
            LogUtils.info(UserConfigManager.class, "init 初始化配置成功 (json): " + json);
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "init", e);
            return false;
        }
    }

    /**
     * 初始化显示配置（特例函数）：读取 user_config 并应用分辨率、确定视窗模式
     * <p>
     * 仅启动阶段（initLibGDX 建 Stage 前）调用一次，与 {@link #init(FileHandle)} 分工：
     * 本函数只负责「分辨率 + 视窗」两块，供 Stage 选择视口；桌面首次启动（无 user_config）时
     * 按屏幕 80%/16:9 检测分辨率，写入仅含 resolution 的小配置，后续由 UpdateChecker
     * protect 机制与内部默认完整配置合并；完整 user_config 解析仍走 init()。
     * <p>
     * 性能：启动时调用一次（含一次外部文件读取 + 可能的屏幕检测），非热路径。
     *
     * @return 确定使用的视窗模式
     */
    public static UseViewport initDisplayConfig ()
    {
        // 读取user_config
        FileHandle userConfigHandle = Gdx.files.external(FileUtils.pathJoin(PathName.BASE, PathName.ASSET, FileName.USER_CONFIG));
        JsonEntity userConfigJson = new JsonEntity();
        if (FileUtils.isFileExist(userConfigHandle))
        {
            userConfigJson = new JsonEntity(userConfigHandle);
        }

        // 使用的视窗
        UseViewport useViewport;

        // 存在配置
        if (!userConfigJson.isEmpty())
        {
            LogUtils.debug(UserConfigManager.class, "initDisplayConfig 读取user_config成功");

            // 配置分辨率
            if (PlatformUtils.isNotDesktop())
            {
                LogUtils.debug(UserConfigManager.class, "initDisplayConfig 运行平台不支持调整分辨率 (platform): " + PlatformUtils.getPlatformType());
            }
            else if (userConfigJson.containsKey(ConfigKey.User.Resolution.KEY))
            {
                // 分辨率
                JsonEntity resolutionJson = userConfigJson.getJsonEntityByKey(ConfigKey.User.Resolution.KEY);

                if (resolutionJson.containsKey(ConfigKey.User.Resolution.WIDTH) && resolutionJson.containsKey(ConfigKey.User.Resolution.HEIGHT))
                {
                    int screenWidth = resolutionJson.getInt(ConfigKey.User.Resolution.WIDTH);
                    int screenHeight = resolutionJson.getInt(ConfigKey.User.Resolution.HEIGHT);
                    Gdx.graphics.setWindowedMode(screenWidth, screenHeight);
                }
            }

            // 配置视窗: stretch, fit, fill
            if (userConfigJson.containsKey(ConfigKey.User.USE_VIEWPORT))
            {
                String useViewportName = userConfigJson.getString(ConfigKey.User.USE_VIEWPORT).toUpperCase();
                useViewport = UseViewport.valueOf(useViewportName);
            }
            else
            {
                useViewport = UseViewport.STRETCH;
            }
        }
        // 不存在配置
        else
        {
            LogUtils.debug(UserConfigManager.class, "initDisplayConfig 读取user_config失败");

            // 配置分辨率: 屏幕80%的16:9
            if (PlatformUtils.isNotDesktop())
            {
                LogUtils.debug(UserConfigManager.class, "initDisplayConfig 运行平台不支持调整分辨率 (platform): " + PlatformUtils.getPlatformType());
            }
            else
            {
                // 窗口占屏幕比例: 80%，即窗口尺寸约为屏幕可用区域的 80%
                // 按 16:9 等比缩放，保证窗口比例合理且不超出屏幕
                final float WINDOW_SCREEN_RATIO = 0.8f;

                // 检测失败/异常的 兜底分辨率
                int detectWidth = 1024;
                int detectHeight = 576;

                try
                {
                    Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
                    if (displayMode != null && displayMode.width > 0 && displayMode.height > 0)
                    {
                        int screenWidth = displayMode.width;
                        int screenHeight = displayMode.height;

                        // 1. 以屏幕宽度为基准: 取 WINDOW_SCREEN_RATIO 作为窗口宽度
                        int targetWidth = (int)(screenWidth * WINDOW_SCREEN_RATIO);
                        // 2. 按 16:9 算出对应高度
                        int targetHeight = targetWidth * 9 / 16;

                        // 3. 检查高度是否超过屏幕高度的 WINDOW_SCREEN_RATIO
                        //    若超出则改以高度为基准反算宽度，避免窗口超出屏幕垂直范围
                        int maxHeight = (int)(screenHeight * WINDOW_SCREEN_RATIO);
                        if (targetHeight > maxHeight)
                        {
                            targetHeight = maxHeight;
                            targetWidth = targetHeight * 16 / 9;
                        }

                        detectWidth = targetWidth;
                        detectHeight = targetHeight;
                    }
                }
                catch (Exception e)
                {
                    LogUtils.error(UserConfigManager.class, "initDisplayConfig 无法获取屏幕尺寸，使用兜底 1024x576", e);
                }

                // 应用窗口尺寸
                Gdx.graphics.setWindowedMode(detectWidth, detectHeight);

                // 写入仅含 resolution 的配置文件到外部路径
                // 该文件后续会被 UpdateChecker.init() 的 protect 机制发现:
                //   - moveProtectExternalFile 将其备份到 temp/
                //   - 与 internal 默认完整配置 combined 合并
                //   - copyInternalFile 覆盖外部后, restoreProtectExternalFile 还原合并后的完整配置
                //   最终外部 user_config.json = 完整配置 + resolution 为本次检测值
                Map<String, Object> resolutionMap = new HashMap<>();
                resolutionMap.put(ConfigKey.User.Resolution.WIDTH, detectWidth);
                resolutionMap.put(ConfigKey.User.Resolution.HEIGHT, detectHeight);
                JsonEntity configJson = new JsonEntity();
                configJson.put(ConfigKey.User.Resolution.KEY, resolutionMap);
                FileUtils.createStringFile(configJson.toString(), userConfigHandle, false);

                LogUtils.info(UserConfigManager.class, "initDisplayConfig 初次启动自适应分辨率: " + detectWidth + "x" + detectHeight);
            }

            // 配置视窗: Desktop默认使用stretch Android默认使用fit
            if (PlatformUtils.isDesktop())
            {
                useViewport = UseViewport.STRETCH;
                LogUtils.info(UserConfigManager.class, "initDisplayConfig 使用平台 (platform): " + PlatformUtils.getPlatformType() + " 最佳视窗 (viewport): " + useViewport);
            }
            else if (PlatformUtils.isAndroid())
            {
                useViewport = UseViewport.FIT;
                LogUtils.info(UserConfigManager.class, "initDisplayConfig 使用平台 (platform): " + PlatformUtils.getPlatformType() + " 最佳视窗 (viewport): " + useViewport);
            }
            else
            {
                useViewport = UseViewport.STRETCH;
                LogUtils.info(UserConfigManager.class, "initDisplayConfig 未知平台 (platform): " + PlatformUtils.getPlatformType() + " 使用默认视窗 (viewport): " + useViewport);
            }
        }
        LogUtils.info(UserConfigManager.class, "initDisplayConfig 分辨率 (resolution): " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        LogUtils.info(UserConfigManager.class, "initDisplayConfig 视窗 (viewport): " + useViewport);

        return useViewport;
    }

    /**
     * 切换全屏/窗口模式，保存或恢复窗口尺寸
     * <p>
     * 全屏状态以 Gdx.graphics 实际窗口状态为准（不依赖 fullscreen 配置字段，避免与实际窗口漂移）；
     * 进入全屏前记忆窗口化尺寸，退出时恢复。fullscreen 配置字段仍由 isFullscreen/setFullscreen 单独维护。
     * <p>
     * 性能：用户触发时调用一次（含全屏模式切换），非热路径。
     */
    public void toggleFullscreen ()
    {
        Graphics graphics = Gdx.graphics;

        if (graphics.isFullscreen())
        {
            // 退出全屏：恢复保存的窗口状态
            graphics.setWindowedMode(windowedWidth, windowedHeight);
            LogUtils.info(UserConfigManager.class, "toggleFullscreen 退出全屏");
        }
        else
        {
            // 进入全屏：保存当前窗口状态
            windowedWidth = graphics.getWidth();
            windowedHeight = graphics.getHeight();

            // 切换到全屏模式
            Graphics.DisplayMode displayMode = graphics.getDisplayMode();
            graphics.setFullscreenMode(displayMode);
            LogUtils.info(UserConfigManager.class, "toggleFullscreen 切换到全屏模式");
        }
    }

    /**
     * 修复用户配置文件：从内部默认配置复制覆盖外部配置并重新初始化
     *
     * @return 修复成功返回 true，失败返回 false
     */
    public boolean repair ()
    {
        try
        {
            // 内部路径
            String internalPath = FileUtils.pathJoin(PathName.ASSET, FileName.USER_CONFIG);
            FileHandle internalPathHandle = Gdx.files.internal(internalPath);

            // 维修
            FileUtils.copyFile(internalPathHandle, pathHandle);
            LogUtils.debug(UserConfigManager.class, "repair 修复用户配置 (path): " + pathHandle);

            // 重新读取
            return init(pathHandle);
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "repair", e);
            return false;
        }
    }

    /**
     * 保存当前用户配置到指定路径
     *
     * @param pathHandle  保存路径句柄
     * @return 保存成功返回 true，失败返回 false
     */
    public boolean save (FileHandle pathHandle)
    {
        try
        {
            return FileUtils.createStringFile(json.toString(), pathHandle, false);
        }
        catch (Exception e)
        {
            LogUtils.error(UserConfigManager.class, "save", e);
            return false;
        }
    }

    // ===================================================================================================================

    /**
     * 获取用户配置的JSON实体
     *
     * @return 用户配置的JSON实体
     */
    public JsonEntity getJson ()
    {
        return json;
    }

    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径字符串
     */
    public FileHandle getPathHandle ()
    {
        return pathHandle;
    }

    /**
     * 获取当前使用的语言
     *
     * @return 语言名称
     */
    public String getLanguage ()
    {
        return language;
    }

    /**
     * 设置当前使用的语言并更新JSON配置
     *
     * @param language 语言名称
     */
    public void setLanguage (String language)
    {
        this.language = language;
        json.put(ConfigKey.User.LANGUAGE, language);
    }

    /**
     * 获取当前使用的主题
     *
     * @return 主题名称
     */
    public String getTheme ()
    {
        return theme;
    }

    /**
     * 设置当前使用的主题并更新JSON配置
     *
     * @param theme 主题名称
     */
    public void setTheme (String theme)
    {
        this.theme = theme;
        json.put(ConfigKey.User.THEME, theme);
    }

    /**
     * 获取当前使用的视窗模式
     *
     * @return 视窗模式枚举
     */
    public UseViewport getUseViewport ()
    {
        return useViewport;
    }

    /**
     * 设置当前使用的视窗模式
     *
     * @param useViewport 视窗模式枚举
     */
    public void setUseViewport (UseViewport useViewport)
    {
        this.useViewport = useViewport;
    }

    /**
     * 判断当前是否为全屏模式
     *
     * @return 全屏返回 true，窗口化返回 false
     */
    public boolean isFullscreen ()
    {
        return fullscreen;
    }

    /**
     * 设置是否全屏
     *
     * @param fullscreen 全屏标志
     */
    public void setFullscreen (boolean fullscreen)
    {
        this.fullscreen = fullscreen;
    }

    /**
     * 获取窗口分辨率宽度
     *
     * @return 分辨率宽度（像素）
     */
    public int getResolutionWidth ()
    {
        return resolutionWidth;
    }

    /**
     * 设置窗口分辨率宽度
     *
     * @param resolutionWidth 分辨率宽度（像素）
     */
    public void setResolutionWidth (int resolutionWidth)
    {
        this.resolutionWidth = resolutionWidth;
    }

    /**
     * 获取窗口分辨率高度
     *
     * @return 分辨率高度（像素）
     */
    public int getResolutionHeight ()
    {
        return resolutionHeight;
    }

    /**
     * 设置窗口分辨率高度
     *
     * @param resolutionHeight 分辨率高度（像素）
     */
    public void setResolutionHeight (int resolutionHeight)
    {
        this.resolutionHeight = resolutionHeight;
    }

    /**
     * 获取总音量
     *
     * @return 总音量值（0.0 ~ 1.0）
     */
    public float getSoundVolumeTotal ()
    {
        return soundVolumeTotal;
    }

    /**
     * 设置总音量并更新JSON配置
     *
     * @param soundVolumeTotal 总音量值（0.0 ~ 1.0）
     */
    public void setSoundVolumeTotal (float soundVolumeTotal)
    {
        this.soundVolumeTotal = soundVolumeTotal;
        json.getJsonEntityByKey(ConfigKey.User.Volume.KEY).put(ConfigKey.User.Volume.TOTAL, soundVolumeTotal);
    }

    /**
     * 获取音乐音量
     *
     * @return 音乐音量值（0.0 ~ 1.0）
     */
    public float getSoundVolumeMusic ()
    {
        return soundVolumeMusic;
    }

    /**
     * 设置音乐音量并更新JSON配置
     *
     * @param soundVolumeMusic 音乐音量值（0.0 ~ 1.0）
     */
    public void setSoundVolumeMusic (float soundVolumeMusic)
    {
        this.soundVolumeMusic = soundVolumeMusic;
        json.getJsonEntityByKey(ConfigKey.User.Volume.KEY).put(ConfigKey.User.Volume.MUSIC, soundVolumeMusic);
    }

    /**
     * 获取音效音量
     *
     * @return 音效音量值（0.0 ~ 1.0）
     */
    public float getSoundVolumeSound ()
    {
        return soundVolumeSound;
    }

    /**
     * 设置音效音量并更新JSON配置
     *
     * @param soundVolumeSound 音效音量值（0.0 ~ 1.0）
     */
    public void setSoundVolumeSound (float soundVolumeSound)
    {
        this.soundVolumeSound = soundVolumeSound;
        json.getJsonEntityByKey(ConfigKey.User.Volume.KEY).put(ConfigKey.User.Volume.SOUND, soundVolumeSound);
    }

    // ===================================================================================================================
    // 上载到 GameInfoManager

    /**
     * 将所有用户配置上载到运行时信息管理器
     *
     * @param gameInfoManager 运行时信息管理器
     */
    public void uploadTo (GameInfoManager gameInfoManager)
    {
        gameInfoManager.putInfo(GameInfoKey.User.USE_VIEWPORT, useViewport.name().toLowerCase());
        gameInfoManager.putInfo(GameInfoKey.User.FULLSCREEN, fullscreen);
        gameInfoManager.putInfo(GameInfoKey.User.LANGUAGE, language);
        gameInfoManager.putInfo(GameInfoKey.User.THEME, theme);
        gameInfoManager.putInfo(GameInfoKey.User.Resolution.WIDTH, resolutionWidth);
        gameInfoManager.putInfo(GameInfoKey.User.Resolution.HEIGHT, resolutionHeight);
        gameInfoManager.putInfo(GameInfoKey.User.SoundVolume.TOTAL, soundVolumeTotal);
        gameInfoManager.putInfo(GameInfoKey.User.SoundVolume.MUSIC, soundVolumeMusic);
        gameInfoManager.putInfo(GameInfoKey.User.SoundVolume.SOUND, soundVolumeSound);
    }
}
