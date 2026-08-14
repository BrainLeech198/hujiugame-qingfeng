package com.hujiugame.qingfeng.type;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动器版本码 → 版本字符串 对照表
 *
 * 启动器版本码（app_version）为单调递增整型，无法从中还原 major.minor.patch 结构，
 * 通过本表可将版本码映射回版本字符串，供 minor/patch/major 兼容判断使用。
 * 每次发布新版本须在此追加一条映射。
 */
public final class AppVersionTable
{
    private AppVersionTable()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final Map<Integer, String> VERSION_CODE_TO_STRING = new HashMap<>();

    static
    {
        VERSION_CODE_TO_STRING.put(1, "1.0.0");
        VERSION_CODE_TO_STRING.put(2, "1.0.0");
    }

    /**
     * 根据版本码获取对应版本字符串
     *
     * @param versionCode 启动器版本码（app_version）
     * @return 版本字符串，若该版本码未收录则返回 null
     */
    @javax.annotation.Nullable
    public static String getVersionString (int versionCode)
    {
        return VERSION_CODE_TO_STRING.get(versionCode);
    }
}
