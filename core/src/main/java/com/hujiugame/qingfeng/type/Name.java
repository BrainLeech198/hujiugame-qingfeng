package com.hujiugame.qingfeng.type;

public final class Name

{
    private Name()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // 主题显示名（与 theme_config.json 的 name 字段同源，主题自身写法）
    public static final String DEFAULT_THEME_NAME = "默认主题";

    // 语言显示名（与 language_config.json 的 name 字段同源，语言自身写法）
    public static final String LANGUAGE_ZH_CN_NAME = "简体中文·中国大陆";
    public static final String LANGUAGE_ZH_TW_NAME = "繁體中文·台灣";
    public static final String LANGUAGE_ZH_HK_NAME = "繁體中文·香港";
    public static final String LANGUAGE_EN_US_NAME = "English·United States";
    public static final String LANGUAGE_EN_GB_NAME = "English·United Kingdom";
    public static final String LANGUAGE_ES_ES_NAME = "Español·España";
    public static final String LANGUAGE_ES_MX_NAME = "Español·México";
    public static final String LANGUAGE_FR_FR_NAME = "Français·France";
    public static final String LANGUAGE_FR_CA_NAME = "Français·Canada";
    public static final String LANGUAGE_PT_BR_NAME = "Português·Brasil";
    public static final String LANGUAGE_PT_PT_NAME = "Português·Portugal";
    public static final String LANGUAGE_RU_RU_NAME = "Русский·Россия";
    public static final String LANGUAGE_JA_JP_NAME = "日本語·日本";
    public static final String LANGUAGE_KO_KR_NAME = "한국어·대한민국";
    public static final String LANGUAGE_DE_DE_NAME = "Deutsch·Deutschland";
    public static final String DEFAULT_LANGUAGE_NAME = LANGUAGE_EN_US_NAME;// 默认语言显示名（回退兜底用，当前默认英文）

    // 游戏默认主题显示名（与 theme_config.json 的 name 字段同源，主题自身写法）
    public static final String GAME_DEFAULT_THEME_NAME = "theme";

    // Script 任务名称
    public static final String GAME_START_TASK_NAME = "start_task";
    public static final String GAME_LOOP_TASK_NAME = "loop_task";
    public static final String GAME_TRIGGER_TASK_NAME = "trigger_task";
}
