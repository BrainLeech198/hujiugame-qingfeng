package com.hujiugame.qingfeng.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.hujiugame.qingfeng.data.JsonEntity;
import com.hujiugame.qingfeng.type.Name;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.game.GameInfoManager;
import com.hujiugame.qingfeng.type.key.common.FileHandleKey;
import com.hujiugame.qingfeng.type.key.config.GameInfoKey;
import com.hujiugame.qingfeng.type.key.config.LanguageKey;
import com.hujiugame.qingfeng.util.system.FileUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LanguageManager
{
    /**
     * 文本解析的数据源类型
     */
    public enum Field
    {
        LANGUAGE("language"),
        GAME("game");

        private final String value;

        Field (String value)
        {
            this.value = value;
        }

        public String getValue ()
        {
            return value;
        }

        public static Field fromValue (String value)
        {
            for (Field field : values())
            {
                if (field.value.equals(value))
                {
                    return field;
                }
            }
            return null;
        }
    }

    // json文件
    private JsonEntity json = new JsonEntity();

    // 使用的语言
    private String name = null;

    // 语言路径类型
    private String kind = null;

    // 语言路径句柄
    private FileHandle pathHandle = null;

    // 语言适配版本
    private String version;

    // 状态码
    private long stateCode = 0;

    // 可用语言块列表
    private Set<String> availableBlocks = Collections.emptySet();

    // 文本解析：游戏信息管理器（可选，游戏内使用）
    private GameInfoManager gameInfoManager = null;
    private long gameInfoStateCode = 0;

    // 文本解析：变量标记配置
    private static final String START_KEY = "{";
    private static final String END_KEY = "}";
    private static final char FIELD_SEPARATOR = '$';
    private static final char BLOCK_SEPARATOR = '#';

    // 解析结果 - 使用 LRU 缓存
    private static final int MAX_BLOCK_COUNT = 3;
    // accessOrder = true 表示按访问顺序排序，最近访问的在尾部
    private final Map<String, Map<String, String>> blockMap = new LinkedHashMap<String, Map<String, String>>(16, 0.75f, true)
    {
        protected boolean removeEldestEntry (Map.Entry<String, Map<String, String>> eldest)
        {
            return size() > MAX_BLOCK_COUNT;
        }
    };

    /**
     * 更新状态码，标记语言管理器状态发生变化
     */
    private void update ()
    {
        stateCode++;
    }

    /**
     * 解析语言路径：从语言集配置中查找指定语言，若不存在则自动修复为默认语言
     *
     * @param pathName           语言路径名称
     * @param directoryPathHandle  语言路径句柄
     * @param isLauncherLanguage 是否为启动器语言（启动器语言不存在时会自动修复）
     * @param userConfigManager  用户配置管理器，用于修复用户配置
     * @return 解析成功返回 true，失败返回 false
     */
    private boolean parseLanguagePath (String pathName, FileHandle directoryPathHandle, boolean isLauncherLanguage, UserConfigManager userConfigManager)
    {
        try
        {
            // 读取语言集json
            FileHandle dictionaryJsonPathHandle = directoryPathHandle.child(FileName.LANGUAGE_DICTIONARY_CONFIG);
            JsonEntity dictionaryJson = new JsonEntity();

            // 检查文件存在
            if (FileUtils.isFileExist(dictionaryJsonPathHandle))
            {
                dictionaryJson = new JsonEntity(dictionaryJsonPathHandle);
            }

            // 文件不存在
            if (dictionaryJson.isEmpty())
            {
                // 复制默认配置到外部
                FileHandle internalDictionaryJsonPathHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_LANGUAGE, FileName.LANGUAGE_DICTIONARY_CONFIG));
                FileUtils.copyFile(internalDictionaryJsonPathHandle, dictionaryJsonPathHandle);

                // 读取json
                dictionaryJson = new JsonEntity(dictionaryJsonPathHandle);
            }

            // 解析语言集配置
            boolean isExist = false;
            if (dictionaryJson.containsKey(pathName))
            {
                // 解析语言配置
                JsonEntity languageJson = dictionaryJson.getJsonEntityByKey(pathName);

                // 配置合法
                if (!languageJson.isEmpty())
                {
                    // 存在对应键
                    if (languageJson.containsKey(LanguageKey.Config.NAME))
                    {
                        // 解析名称和类型
                        name = languageJson.getString(LanguageKey.Config.NAME);
                        kind = languageJson.getString(LanguageKey.Config.KIND);
                        if (FileHandleKey.INTERNAL.equals(kind))
                        {
                            pathHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_LANGUAGE, pathName));
                        }
                        else
                        {
                            pathHandle = directoryPathHandle.child(pathName);
                        }

                        LogUtils.debug(LanguageManager.class, "parsePath 用户使用的语言 (name): " + name);
                        LogUtils.debug(LanguageManager.class, "parsePath 用户使用的语言 (kind): " + kind);
                        LogUtils.debug(LanguageManager.class, "parsePath 用户使用的语言 (path): " + pathHandle);

                        // 判断文件夹是否存在
                        if (FileUtils.isDirectoryExist(pathHandle))
                        {
                            isExist = true;
                        }
                    }
                }
            }

            // 不存在语言
            if (!isExist)
            {
                if (!isLauncherLanguage)
                {
                    FileHandle maybeLanguagePathHandle = directoryPathHandle.child(pathName);
                    LogUtils.error(LanguageManager.class, "parsePath 找不到语言 (path): " + maybeLanguagePathHandle);
                    return false;
                }

                // 不存在对应语言索引 默认使用默认语言
                name = Name.DEFAULT_LANGUAGE_NAME;
                kind = FileHandleKey.INTERNAL;
                pathHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_LANGUAGE, FileName.DEFAULT_LANGUAGE_PATH));

                // 修复用户配置
                userConfigManager.setLanguage(FileName.DEFAULT_LANGUAGE_PATH);
                userConfigManager.save(userConfigManager.getPathHandle());
                LogUtils.debug(LanguageManager.class, "parsePath 修复用户配置 (language): " + name);

                // 融合内部语言集配置：外部语言集作为 mainJson（主体保留），内部语言集作为 mergeJson（补入缺失的官方语言条目）
                FileHandle internalDictionaryJsonPathHandle = Gdx.files.internal(FileUtils.pathJoin(PathName.ASSET_S_LANGUAGE, FileName.LANGUAGE_DICTIONARY_CONFIG));
                JsonEntity internalDictionaryJson = new JsonEntity(internalDictionaryJsonPathHandle);
                if (!internalDictionaryJson.isEmpty())
                {
                    dictionaryJson = dictionaryJson.combined(internalDictionaryJson);
                    FileUtils.createStringFile(dictionaryJson.getJsonString(), dictionaryJsonPathHandle, false);
                    LogUtils.debug(LanguageManager.class, "parsePath 融合内部语言集配置成功 (json): " + dictionaryJson);
                }

                LogUtils.debug(LanguageManager.class, "parsePath 添加默认语言 (name): " + name);
                LogUtils.debug(LanguageManager.class, "parsePath 添加默认语言 (kind): " + kind);
                LogUtils.debug(LanguageManager.class, "parsePath 添加默认语言 (path): " + pathHandle);
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "parseLanguagePath", e);
            return false;
        }
    }

    /**
     * 解析语言JSON配置文件
     *
     * @param pathHandle 语言路径文件句柄
     * @return 解析成功返回 true，失败返回 false
     */
    private boolean parseJson (FileHandle pathHandle)
    {
        try
        {
            // 读取语言配置
            FileHandle languageJsonPathHandle = pathHandle.child(FileName.LANGUAGE_S_CONFIG);
            json = new JsonEntity(languageJsonPathHandle);

            // 语言配置不存在
            if (json.isEmpty())
            {
                LogUtils.error(LanguageManager.class, "parseJson 语言配置不存在 (file): " + languageJsonPathHandle);
                return false;
            }

            LogUtils.debug(LanguageManager.class, "parseJson 读取语言配置 (path): " + languageJsonPathHandle);

            // 解析可用语言块列表
            if (json.containsKey(LanguageKey.BLOCKS))
            {
                availableBlocks = new HashSet<>(json.getStringList(LanguageKey.BLOCKS));
                LogUtils.debug(LanguageManager.class, "parseJson 读取可用语言块 (blocks): " + availableBlocks);
            }
            else
            {
                availableBlocks = Collections.emptySet();
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "parseJson", e);
            return false;
        }
    }

    /**
     * 从JSON中加载语言版本号
     *
     * @param json 语言JSON实体
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadVersionFromJson (JsonEntity json)
    {
        try
        {
            // 字体读取
            if (json.containsKey(LanguageKey.VERSION))
            {
                version = json.getString(LanguageKey.VERSION);
                LogUtils.debug(LanguageManager.class, "loadVersionFromJson 读取语言版本 (version): " + version);
            }
            else
            {
                LogUtils.error(LanguageManager.class, "loadVersionFromJson 读取语言版本 失败");
                return false;
            }
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "loadVersionFromJson", e);
            return false;
        }
    }

    /**
     * 将嵌套的JSON实体扁平化为键值对映射（递归处理子对象）
     *
     * @param json   待扁平化的JSON实体
     * @param prefix 当前递归层级的前缀字符串
     * @param map    扁平化结果存储的目标映射
     * @return 扁平化成功返回 true，失败返回 false
     */
    private boolean flattenMap (JsonEntity json, String prefix, Map<String, String> map)
    {
        try
        {
            for (String key : json.keySet())
            {
                // 判断是否到底
                if (json.getObject(key) instanceof String)
                {
                    map.put(prefix + key, json.getString(key));
                }
                else
                {
                    // 递归
                    if (!flattenMap(json.getJsonEntityByKey(key), prefix + key + ".", map))
                    {
                        return false;
                    }
                }
            }
            LogUtils.debug(LanguageManager.class, "flattenMap 读取语言 (map): " + map);
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "flattenMap", e);
            return false;
        }
    }

    /**
     * 初始化语言管理器：解析语言路径、加载配置和版本
     *
     * @param pathName           语言路径名称
     * @param directoryPathHandle  语言路径文件句柄
     * @param isLauncherLanguage 是否为启动器语言
     * @param userConfigManager  用户配置管理器
     * @return 初始化成功返回 true，失败返回 false
     */
    public boolean init (String pathName, FileHandle directoryPathHandle, boolean isLauncherLanguage, UserConfigManager userConfigManager)
    {
        try
        {
            // 解析语言路径
            if (!parseLanguagePath(pathName, directoryPathHandle, isLauncherLanguage, userConfigManager))
            {
                LogUtils.error(LanguageManager.class, "init 解析使用的语言失败 (name): " + pathName);
                return false;
            }

            // 解析json
            if (!parseJson(pathHandle))
            {
                LogUtils.error(LanguageManager.class, "init 解析语言配置失败 (name): " + pathName);
                return false;
            }

            // 获取版本
            if (!loadVersionFromJson(json))
            {
                LogUtils.error(LanguageManager.class, "init 读取语言版本失败 (json): " + json);
                return false;
            }

            // 增加状态码
            update();

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "init", e);
            return false;
        }
    }

    /**
     * 重新加载语言：调用 init 重新初始化
     *
     * @param name                语言路径名称
     * @param directoryPathHandle 语言路径文件句柄
     * @param isLauncherLanguage  是否为启动器语言
     * @param userConfigManager   用户配置管理器
     * @return 重载成功返回 true，失败返回 false
     */
    public boolean reload (String name, FileHandle directoryPathHandle, boolean isLauncherLanguage, UserConfigManager userConfigManager)
    {
        try
        {
            // 切换语言前清空旧语言块缓存：块名不随语言变，命中缓存会返回旧语言内容
            blockMap.clear();
            return init(name, directoryPathHandle, isLauncherLanguage, userConfigManager);
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "reload", e);
            return false;
        }
    }

    /**
     * 获取语言显示名称
     *
     * @return 语言显示名称
     */
    public String getName ()
    {
        return name;
    }

    /**
     * 获取语言种类
     *
     * @return 语言种类
     */
    public String getKind  ()
    {
        return kind;
    }

    /**
     * 获取语言路径
     *
     * @return 语言路径
     */
    public FileHandle getPathHandle ()
    {
        return pathHandle;
    }

    /**
     * 获取可用语言块列表
     *
     * @return 可用语言块名称集合，未配置时返回空集合
     */
    public Set<String> getAvailableBlocks ()
    {
        return availableBlocks;
    }

    /**
     * 加载指定语言块：读取并扁平化语言文件，加入LRU缓存
     *
     * @param block 语言块名称
     * @return 加载成功返回 true，失败返回 false
     */
    private boolean loadBlock (String block)
    {
        try
        {
            // 校验：如果声明了可用块列表，检查请求的块是否在清单中
            if (!availableBlocks.isEmpty() && !availableBlocks.contains(block))
            {
                // 自动检测：文件实际存在则加入清单
                // 先尝试直接用 block 原名查找，找不到则补 .json
                FileHandle probePath = pathHandle.child(block);
                if (!FileUtils.isFileExist(probePath))
                {
                    probePath = pathHandle.child(block + ".json");
                }
                if (FileUtils.isFileExist(probePath))
                {
                    availableBlocks.add(block);
                    LogUtils.debug(LanguageManager.class, "loadBlock 自动发现并加入新语言块 (block): " + block);
                }
                else
                {
                    LogUtils.error(LanguageManager.class, "loadBlock 未知的语言块 (block): " + block + " (available): " + availableBlocks);
                    return false;
                }
            }

            // 如果已经存在，直接返回（LRU 会自动更新访问顺序）
            if (blockMap.containsKey(block))
            {
                return true;
            }

            // 读取并扁平化语言图：先尝试 block 原路径，找不到则补 .json
            FileHandle mapPathHandle = pathHandle.child(block);
            if (!FileUtils.isFileExist(mapPathHandle))
            {
                mapPathHandle = pathHandle.child(block + ".json");
            }
            JsonEntity mapJson = new JsonEntity(mapPathHandle);
            Map<String, String> newFlattenMap = new HashMap<>();
            if (!flattenMap(mapJson, "", newFlattenMap))
            {
                LogUtils.error(LanguageManager.class, "loadBlock 读取新块语言图失败 (block): " + block);
                return false;
            }

            // 放入缓存，LinkedHashMap 会自动处理超出容量的淘汰
            blockMap.put(block, newFlattenMap);

            // 增加状态码
            update();
            LogUtils.debug(LanguageManager.class, "loadBlock 加载新块语言图成功 (block): " + block);
            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "loadBlock", e);
            return false;
        }
    }

    /**
     * 获取指定语言块中键对应的文本值，块未加载时自动加载
     *
     * @param block  语言块名称
     * @param textKey 文本键
     * @return 对应的文本值，若不存在则回退到键本身
     */
    public String getText (String block, String textKey)
    {
        try
        {
            if (!blockMap.containsKey(block))
            {
                if (!loadBlock(block))
                {
                    LogUtils.error(LanguageManager.class, "getText 重载 读取块失败 (block): " + block);
                    return textKey;  // 回退到键本身
                }
                else
                {
                    LogUtils.debug(LanguageManager.class, "getText 重载 读取块成功 (block): " + block);
                }
            }
            // 注意：get 操作会触发 LinkedHashMap 的访问顺序更新
            return blockMap.get(block).getOrDefault(textKey, textKey);
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "getText", e);
            return textKey;
        }
    }

    /**
     * 获取当前状态码，同时轮询游戏信息管理器的状态变化
     *
     * @return 当前状态码
     */
    public long getStateCode ()
    {
        // 轮询游戏信息
        if (gameInfoManager != null && gameInfoManager.getStateCode() != gameInfoStateCode)
        {
            gameInfoStateCode = gameInfoManager.getStateCode();
            update();
        }

        return stateCode;
    }

    // ===================================================================================================================
    // 文本解析（从 TextManager 合并）

    /**
     * 设置游戏信息管理器（可选，游戏内使用时注入）
     *
     * @param gameInfoManager 游戏信息管理器实例
     */
    public void setGameInfoManager (GameInfoManager gameInfoManager)
    {
        this.gameInfoManager = gameInfoManager;
        update();
    }

    /**
     * 解析语言文本：从语言管理器中获取指定块和键对应的文本
     */
    private String parseLanguageText (String block, String key)
    {
        try
        {
            return getText(block, key);
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "parseLanguageText", e);
            return "Parse language key error (block): " + block + " (key): " + key;
        }
    }

    /**
     * 解析游戏信息文本：从游戏信息管理器中获取指定键对应的信息
     */
    private String parseGameInfoText (String key)
    {
        try
        {
            if (gameInfoManager != null)
            {
                Object info = gameInfoManager.getInfo(key);
                return info == null ? "null" : info.toString();
            }
            else
            {
                return "GameInfoMap is null";
            }
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "parseGameInfoText", e);
            return " Parse gameInfo key error (key): " + key;
        }
    }

    /**
     * 解析花括号内的变量文本：按域分隔符和块分隔符拆解并获取实际值
     *
     * @param braceText 花括号内的变量文本，格式为 "域$块#键" 或 "域$键"
     * @return 解析后的实际文本，解析失败返回原文本
     */
    private String parseBraceText (String braceText)
    {
        try
        {
            // 分割域
            String[] splitField = braceText.split(Pattern.quote(String.valueOf(FIELD_SEPARATOR)));

            // 域和主键
            String field;
            String mainKey;
            if (splitField.length == 2)
            {
                field = splitField[0];
                mainKey = splitField[1];
            }
            else
            {
                LogUtils.error(LanguageManager.class, "parseBraceText 出现错误，不正确的分隔符数量 (braceText): " + braceText);
                return braceText;
            }

            // 分割块
            String[] splitBlock = mainKey.split(Pattern.quote(String.valueOf(BLOCK_SEPARATOR)));

            // 块和键
            String block = null;
            String key;
            if (splitBlock.length == 1)
            {
                key = splitBlock[0];
            }
            else if (splitBlock.length == 2)
            {
                block = splitBlock[0];
                key = splitBlock[1];
            }
            else
            {
                LogUtils.error(LanguageManager.class, "parseBraceText 错误，不正确的分隔符数量 (braceText): " + braceText);
                return braceText;
            }

            // 获取实际值
            Field f = Field.fromValue(field);
            if (f == null)
            {
                return "Field is not exist (field): " + field;
            }

            switch (f)
            {
                case LANGUAGE:
                    return parseLanguageText(block, key);

                case GAME:
                    return parseGameInfoText(key);

                default:
                    return "Field is not exist (field): " + field;
            }
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "parseBraceText", e);
            return braceText;
        }
    }

    /**
     * 解析文本中的变量标记（如 {language$block#key}），替换为实际内容
     * <p>
     * 性能：每次调用做正则匹配 + 可能的语言块加载，不应在每帧热路径中调用；
     * TextObject 内部通过状态码轮询实现懒解析，避免重复调用。
     *
     * @param text 包含变量标记的原始文本
     * @return 解析后的文本，所有变量标记已被替换为实际内容
     */
    public String resolveText (String text)
    {
        try
        {
            String quotedStart = Pattern.quote(START_KEY);
            String quotedEnd = Pattern.quote(END_KEY);

            String regex = quotedStart + "(.*?)" + quotedEnd + "|(?:(?!" + quotedStart + "|" + quotedEnd + ").)+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);

            List<String> textList = new ArrayList<>();
            List<Boolean> isBraceList = new ArrayList<>();

            while (matcher.find())
            {
                if (matcher.group(1) != null)
                {
                    textList.add(matcher.group(1));
                    isBraceList.add(true);
                }
                else
                {
                    textList.add(matcher.group());
                    isBraceList.add(false);
                }
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < textList.size(); i++)
            {
                if (isBraceList.get(i))
                {
                    result.append(parseBraceText(textList.get(i)));
                }
                else
                {
                    result.append(textList.get(i));
                }
            }

            return result.toString();
        }
        catch (Exception e)
        {
            LogUtils.error(LanguageManager.class, "resolveText", e);
            return text;
        }
    }

    // ===================================================================================================================
    // 上载到 GameInfoManager

    /**
     * 将语言信息上载到运行时信息管理器
     *
     * @param gameInfoManager 运行时信息管理器
     */
    public void uploadTo (GameInfoManager gameInfoManager)
    {
        gameInfoManager.putInfo(GameInfoKey.User.LANGUAGE_NAME, name);
    }
}
