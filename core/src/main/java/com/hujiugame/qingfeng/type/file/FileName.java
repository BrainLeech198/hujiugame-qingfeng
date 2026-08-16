package com.hujiugame.qingfeng.type.file;

public final class FileName
{
    private FileName()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // 启动器
    public static final String INTERNAL_DIRECTORY_STRUCTURE_CONFIG = "directory_structure.json";

    public static final String IMAGE_ERROR = "error.png";
    public static final String DEFAULT_SPLASH = "app_init.png";
    public static final String DEFAULT_REPAIR = "app_repair.png";

    public static final String KEYBOARD_BUTTON_ENTER = "keyboard_button_enter.png";
    public static final String KEYBOARD_BUTTON_ESCAPE = "keyboard_button_escape.png";
    public static final String KEYBOARD_BUTTON_ENTER_OR_ESCAPE = "keyboard_button_enter_or_escape.png";

    public static final String CONTROLLER_CURSOR = "controller_cursor.png";
    public static final String CONTROLLER_BUTTON_A = "controller_button_a.png";
    public static final String CONTROLLER_BUTTON_B = "controller_button_b.png";
    public static final String CONTROLLER_BUTTON_A_OR_B = "controller_button_a_or_b.png";

    public static final String VIRTUAL_CONFIRM_RECT = "virtual_confirm_rect.png";
    public static final String VIRTUAL_CANCEL_RECT = "virtual_cancel_rect.png";

    public static final String LOG_CONFIG = "log_config.json";
    public static final String UPDATE_CONFIG = "update_config.json";
    public static final String CRASH_LOG = "crash-";       // 崩溃日志前缀（crash-<时间戳>）
    public static final String APP_VERSION = "app_version.json";
    public static final String APP_CONFIG = "app_config.json";
    public static final String USER_CONFIG = "user_config.json";

    public static final String LANGUAGE_ZH_CN_PATH = "zh_CN";
    public static final String LANGUAGE_ZH_TW_PATH = "zh_TW";
    public static final String LANGUAGE_ZH_HK_PATH = "zh_HK";
    public static final String LANGUAGE_EN_US_PATH = "en_US";
    public static final String LANGUAGE_EN_GB_PATH = "en_GB";
    public static final String LANGUAGE_ES_ES_PATH = "es_ES";
    public static final String LANGUAGE_ES_MX_PATH = "es_MX";
    public static final String LANGUAGE_FR_FR_PATH = "fr_FR";
    public static final String LANGUAGE_FR_CA_PATH = "fr_CA";
    public static final String LANGUAGE_PT_BR_PATH = "pt_BR";
    public static final String LANGUAGE_PT_PT_PATH = "pt_PT";
    public static final String LANGUAGE_RU_RU_PATH = "ru_RU";
    public static final String LANGUAGE_JA_JP_PATH = "ja_JP";
    public static final String LANGUAGE_KO_KR_PATH = "ko_KR";
    public static final String LANGUAGE_DE_DE_PATH = "de_DE";
    public static final String DEFAULT_LANGUAGE_PATH = LANGUAGE_EN_US_PATH;
    public static final String LANGUAGE_DICTIONARY_CONFIG = "language_config.json";
    public static final String LANGUAGE_S_CONFIG = "language.json";

    public static final String DEFAULT_THEME = "default_theme";
    public static final String THEME_DICTIONARY_CONFIG = "theme_config.json";
    public static final String THEME_S_CONFIG = "theme.json";
    public static final String THEME_S_ICON = "icon.png";

    public static final String THEME_S_UI_FONT_S_CONFIG = "font.json";
    public static final String THEME_S_UI_MESSAGE_BOX_S_CONFIG = "message_box.json";
    public static final String THEME_S_UI_CONFIG = "ui_config.json";

    public static final String PAGE_LAYOUT = "layout.json";
    public static final String PAGE_CONFIG = "config.json";

    public static final String GAME_DICTIONARY_CONFIG = "game_config.json";
    public static final String SAVE_DICTIONARY_CONFIG = "save_config.json";
    public static final String IMPORT_DICTIONARY_CONFIG = "import_config.json";
    public static final String EXPORT_DICTIONARY_CONFIG = "export_config.json";

    // 游戏内
    public static final String IN_GAME_CONFIG = "game.json";
    public static final String IN_GAME_ICON = "icon.png";
    public static final String IN_GAME_USER_CONFIG = "user_config.json";

    public static final String IN_GAME_SCRIPT_DICTIONARY_CONFIG = "script_config.json";

    public static final String IN_GAME_PAGE_LAYOUT = "layout.json";
    public static final String IN_GAME_PAGE_CONFIG = "config.json";

    public static final String IN_GAME_STORY_TEMPLATE_DICTIONARY_CONFIG = "template_config.json";
    public static final String IN_GAME_STORY_S_ROLE_DICTIONARY_CONFIG = "role_config.json";
    public static final String IN_GAME_STORY_S_ROLE_CONFIG = "role.json";
    public static final String IN_GAME_STORY_S_ROLE_SHOW_LAYOUT = "show.json";

    public static final String IN_GAME_STORY_S_ROLE_PAGE_S_LAYOUT = "layout.json";
    public static final String IN_GAME_STORY_S_ROLE_PAGE_S_BEHAVIOR = "behavior.json";
}
