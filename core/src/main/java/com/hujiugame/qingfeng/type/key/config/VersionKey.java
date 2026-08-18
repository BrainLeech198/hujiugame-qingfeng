package com.hujiugame.qingfeng.type.key.config;

/**
 * 版本相关 JSON 字段名常量
 */
public final class VersionKey
{
    private VersionKey()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ====================================================================================================
    // app_version.json 字段

    /** 内部版本号 */
    public static final String APP_VERSION = "app_version";
    /** 内部版本类型 */
    public static final String APP_VERSION_TYPE = "app_version_type";
    /** 内部版本字符串 */
    public static final String APP_VERSION_STRING = "app_version_string";
    /** beta 细分快照码（Minecraft 法则 YYwWWa），仅 beta 填充，release 为空 */
    public static final String APP_VERSION_SNAPSHOT = "app_version_snapshot";

    // ====================================================================================================
    // 远程 versions.json 字段

    /** 最新版本号 */
    public static final String NEWEST_VERSION = "newest_version";
    /** 最新版本类型 */
    public static final String NEWEST_VERSION_TYPE = "newest_version_type";
    /** 最新版本字符串 */
    public static final String NEWEST_VERSION_STRING = "newest_version_string";
    /** 最新版本 beta 细分快照码（Minecraft 法则 YYwWWa），release 为空 */
    public static final String NEWEST_VERSION_SNAPSHOT = "newest_version_snapshot";

    // ====================================================================================================
    // update_config.json 字段

    public static final class Update
    {
        private Update()
        {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        /** 受保护文件列表 */
        public static final String PROTECT = "protect";
        /** 禁止文件列表 */
        public static final String PROHIBIT = "prohibit";
    }
}
