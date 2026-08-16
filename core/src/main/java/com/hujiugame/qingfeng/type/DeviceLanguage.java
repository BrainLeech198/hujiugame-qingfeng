package com.hujiugame.qingfeng.type;

import com.hujiugame.qingfeng.type.file.FileName;

import java.util.Locale;

/**
 * 设备语言枚举：将系统语言映射到项目语言目录名（语言_地区，大写）
 * <p>
 * 用于首次运行时根据设备语言决定默认语言配置（写 user_config 的 language 字段），
 * 四平台（Windows/Linux/Mac/Android）均基于 JVM 的 {@link Locale#getDefault()} 通用。
 * <p>
 * 性能：仅在启动首次运行阶段调用一次，非热路径。
 */
public enum DeviceLanguage
{
    /** 简体中文·中国大陆 */
    ZH_CN(FileName.LANGUAGE_ZH_CN_PATH),

    /** 繁體中文·台灣 */
    ZH_TW(FileName.LANGUAGE_ZH_TW_PATH),

    /** 繁體中文·香港 */
    ZH_HK(FileName.LANGUAGE_ZH_HK_PATH),

    /** English·United States */
    EN_US(FileName.LANGUAGE_EN_US_PATH),

    /** English·United Kingdom */
    EN_GB(FileName.LANGUAGE_EN_GB_PATH),

    /** Español·España */
    ES_ES(FileName.LANGUAGE_ES_ES_PATH),

    /** Español·México */
    ES_MX(FileName.LANGUAGE_ES_MX_PATH),

    /** Français·France */
    FR_FR(FileName.LANGUAGE_FR_FR_PATH),

    /** Français·Canada */
    FR_CA(FileName.LANGUAGE_FR_CA_PATH),

    /** Português·Brasil */
    PT_BR(FileName.LANGUAGE_PT_BR_PATH),

    /** Português·Portugal */
    PT_PT(FileName.LANGUAGE_PT_PT_PATH),

    /** Русский·Россия */
    RU_RU(FileName.LANGUAGE_RU_RU_PATH),

    /** 日本語·日本 */
    JA_JP(FileName.LANGUAGE_JA_JP_PATH),

    /** 한국어·대한민국 */
    KO_KR(FileName.LANGUAGE_KO_KR_PATH),

    /** Deutsch·Deutschland */
    DE_DE(FileName.LANGUAGE_DE_DE_PATH);

    // 语言目录名（user_config 的 language 字段值）
    private final String pathName;

    DeviceLanguage (String pathName)
    {
        this.pathName = pathName;
    }

    /**
     * 根据设备语言选择对应的语言枚举
     * <p>
     * 按「语言 + 地区」精确匹配地区变体，未命中时回退该语言的默认地区：
     * zh 的 TW→台湾繁体、HK/MO→香港繁体、其余→简体大陆；en 的 GB→英式、其余→美式；
     * es 的 MX→墨西哥、其余→西班牙；fr 的 CA→加拿大、其余→法国；pt 的 PT→葡萄牙、其余→巴西；
     * ru/ja/ko/de 仅单地区。不支持的语言回退英文。
     * <p>
     * 性能：仅启动首次运行阶段调用一次，非热路径。
     *
     * @param locale 设备语言环境
     * @return 匹配的语言枚举，未命中回退 {@link #EN_US}
     */
    public static DeviceLanguage fromLocale (Locale locale)
    {
        String language = locale.getLanguage();
        String country = locale.getCountry();

        if ("zh".equals(language))
        {
            if ("TW".equals(country))
            {
                return ZH_TW;
            }
            if ("HK".equals(country) || "MO".equals(country))
            {
                return ZH_HK;
            }
            return ZH_CN;
        }
        if ("en".equals(language))
        {
            if ("GB".equals(country))
            {
                return EN_GB;
            }
            return EN_US;
        }
        if ("es".equals(language))
        {
            if ("MX".equals(country))
            {
                return ES_MX;
            }
            return ES_ES;
        }
        if ("fr".equals(language))
        {
            if ("CA".equals(country))
            {
                return FR_CA;
            }
            return FR_FR;
        }
        if ("pt".equals(language))
        {
            if ("PT".equals(country))
            {
                return PT_PT;
            }
            return PT_BR;
        }
        if ("ru".equals(language))
        {
            return RU_RU;
        }
        if ("ja".equals(language))
        {
            return JA_JP;
        }
        if ("ko".equals(language))
        {
            return KO_KR;
        }
        if ("de".equals(language))
        {
            return DE_DE;
        }

        // 不支持的语言回退英文
        return EN_US;
    }

    /**
     * 检测系统默认语言对应的语言枚举
     *
     * @return 系统默认语言对应的语言枚举
     */
    public static DeviceLanguage detectDefault ()
    {
        return fromLocale(Locale.getDefault());
    }

    /**
     * 获取语言目录名
     *
     * @return 语言目录名，如 zh_CN、en_US、es_ES
     */
    public String getPathName ()
    {
        return pathName;
    }
}
