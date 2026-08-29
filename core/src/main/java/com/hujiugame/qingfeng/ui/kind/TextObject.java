package com.hujiugame.qingfeng.ui.kind;

import com.hujiugame.qingfeng.manager.LanguageManager;

public class TextObject
{
    private LanguageManager languageManager;
    private long languageManagerStateCode = -1;

    private String rawText = null;
    private String baseText = null;
    private String displayText = null;
    private String prefix =  null;
    private String suffix =  null;

    /**
     * 创建文本对象
     *
     * @param languageManager 语言管理器（可为 null，表示纯文本）
     * @param rawText         原始文本内容
     */
    public TextObject (LanguageManager languageManager, String rawText)
    {
        this.languageManager = languageManager;
        this.rawText = rawText;
    }

    /**
     * 更换语言管理器并重置状态码
     */
    private void changeLanguageManager (LanguageManager languageManager)
    {
        this.languageManager = languageManager;
        this.languageManagerStateCode = -1;
    }

    // ===================================================================================================================

    /**
     * 解析原始文本，将文本键解析为基础文本
     */
    private void parseRawText ()
    {
        this.baseText = languageManager.resolveText(getRawText());
        this.languageManagerStateCode = languageManager.getStateCode();
    }

    /**
     * 组合前缀、基础文本和后缀，生成显示文本
     */
    private void combineText ()
    {
        this.displayText = getPrefix() + getBaseText() + getSuffix();
    }

    // ===================================================================================================================

    /**
     * 获取原始文本
     */
    public String getRawText ()
    {
        if (rawText != null)
        {
            return rawText;
        }
        else
        {
            return "";
        }
    }

    /**
     * 设置原始文本，并清空缓存的基础文本
     */
    public void setRawText (String rawText)
    {
        this.rawText = rawText;
        baseText = null;
    }

    /**
     * 获取前缀
     */
    private String getPrefix ()
    {
        if (prefix != null)
        {
            return prefix;
        }
        else
        {
            return "";
        }
    }

    /**
     * 设置前缀，并清空缓存的显示文本
     */
    public void setPrefix (String prefix)
    {
        this.prefix = prefix;
        displayText = null;
    }

    /**
     * 获取后缀
     */
    private String getSuffix ()
    {
        if (suffix != null)
        {
            return suffix;
        }
        else
        {
            return "";
        }
    }

    /**
     * 设置后缀，并清空缓存的显示文本
     */
    public void setSuffix (String suffix)
    {
        this.suffix = suffix;
        displayText = null;
    }

    /**
     * 同时设置前缀和后缀
     */
    public void setPrefixSuffix (String prefix, String suffix)
    {
        setPrefix(prefix);
        setSuffix(suffix);
    }

    /**
     * 获取解析后的基础文本（懒加载）
     */
    public String getBaseText ()
    {
        // 懒解析
        if (baseText == null)
        {
            parseRawText();
        }
        return baseText;
    }

    // ===================================================================================================================

    /**
     * 获取完整的显示文本（前缀 + 基础文本 + 后缀，懒加载）
     */
    public String getDisplayText ()
    {

        // 纯文本
        if (languageManager == null)
        {
            return getRawText();
        }

        // 懒解析
        if (languageManagerStateCode != languageManager.getStateCode())
        {
            parseRawText();
            combineText();
        }
        else
        {
            // 懒加载
            if (displayText == null)
            {
                combineText();
            }
        }

        return displayText;
    }

    /**
     * 返回文本对象的字符串表示
     */
    @Override
    public String toString ()
    {
        return "TextObject{" +
                "languageManager=" + languageManager +
                ", languageManagerStateCode=" + languageManagerStateCode +
                ", rawText='" + rawText + '\'' +
                ", baseText='" + baseText + '\'' +
                ", displayText='" + displayText + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}
