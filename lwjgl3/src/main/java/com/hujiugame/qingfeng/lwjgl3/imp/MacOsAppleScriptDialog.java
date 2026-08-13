package com.hujiugame.qingfeng.lwjgl3.imp;

import com.badlogic.gdx.Gdx;
import com.hujiugame.qingfeng.util.interact.interfaces.NativeDialog;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.io.InputStream;

/**
 * macOS 原生对话框实现（AppleScript {@code osascript display dialog}，非 AWT）。
 * <p>
 * macOS 上 AWT/Swing 与 LWJGL3 抢占主线程（{@code -XstartOnFirstThread}）会死锁卡死游戏，
 * 因此 macOS 原生弹窗改用独立进程 {@code osascript} 实现，与游戏主线程完全解耦。
 */
public final class MacOsAppleScriptDialog implements NativeDialog
{
    /** 确定按钮文案 */
    private static final String OK_BUTTON = "确定";
    /** 确认对话框按钮顺序（取消在前，避免误触确定） */
    private static final String[] CONFIRM_BUTTONS = {"取消", "确定"};
    /** 默认按钮（确定）被按下时 osascript 返回的退出码 */
    private static final int EXIT_OK = 0;

    @Override
    public void showInfo (String title, String message, Runnable onClose)
    {
        // 信息提示：无论以何种方式关闭都执行 onClose
        runDialog(buildDialogScript(title, message, new String[]{OK_BUTTON}, OK_BUTTON),
            exitCode ->
            {
                if (onClose != null)
                {
                    onClose.run();
                }
            });
    }

    @Override
    public void showConfirm (String title, String message, Runnable onConfirm, Runnable onCancel)
    {
        runDialog(buildDialogScript(title, message, CONFIRM_BUTTONS, OK_BUTTON),
            exitCode ->
            {
                if (exitCode == EXIT_OK)
                {
                    if (onConfirm != null)
                    {
                        onConfirm.run();
                    }
                }
                else if (onCancel != null)
                {
                    onCancel.run();
                }
            });
    }

    @Override
    public void showError (String title, String message, Runnable onClose)
    {
        runDialog(buildDialogScript(title, message, new String[]{OK_BUTTON}, OK_BUTTON, "stop"),
            exitCode ->
            {
                if (onClose != null)
                {
                    onClose.run();
                }
            });
    }

    // ===================================================================================================

    /**
     * 构建 {@code display dialog} 脚本
     *
     * @param title         标题
     * @param message       内容
     * @param buttons       按钮数组
     * @param defaultButton 默认按钮文案
     * @return AppleScript 脚本字符串
     */
    private String buildDialogScript (String title, String message, String[] buttons, String defaultButton)
    {
        return buildDialogScript(title, message, buttons, defaultButton, null);
    }

    /**
     * 构建 {@code display dialog} 脚本（可指定图标）
     *
     * @param title         标题
     * @param message       内容
     * @param buttons       按钮数组
     * @param defaultButton 默认按钮文案
     * @param icon          图标名（"stop"/"caution"/"note"），null 表示不指定
     * @return AppleScript 脚本字符串
     */
    private String buildDialogScript (String title, String message, String[] buttons, String defaultButton, String icon)
    {
        StringBuilder sb = new StringBuilder("display dialog \"");
        sb.append(escapeAppleScript(message)).append("\" with title \"");
        sb.append(escapeAppleScript(title)).append("\" buttons {");
        for (int i = 0; i < buttons.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append('"').append(escapeAppleScript(buttons[i])).append('"');
        }
        sb.append("} default button \"").append(escapeAppleScript(defaultButton)).append('"');
        if (icon != null)
        {
            sb.append(" with icon ").append(icon);
        }
        return sb.toString();
    }

    /**
     * 转义 AppleScript 字符串中的反斜杠与双引号
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeAppleScript (String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 在后台线程执行 osascript，退出码回调回 GL 线程
     *
     * @param script  AppleScript 脚本
     * @param handler 退出码处理器（GL 线程执行）
     */
    private void runDialog (String script, ExitCodeHandler handler)
    {
        Thread dialogThread = new Thread(() ->
        {
            int exitCode = -1;
            try
            {
                ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // 消费输出，防止缓冲区死锁
                try (InputStream is = process.getInputStream())
                {
                    byte[] buffer = new byte[4096];
                    while (is.read(buffer) != -1) { /* discard */ }
                }
                exitCode = process.waitFor();
            }
            catch (Exception e)
            {
                LogUtils.error(MacOsAppleScriptDialog.class, "runDialog 执行失败", e);
            }
            finally
            {
                final int code = exitCode;
                Gdx.app.postRunnable(() ->
                {
                    try
                    {
                        handler.handle(code);
                    }
                    catch (Exception e)
                    {
                        LogUtils.error(MacOsAppleScriptDialog.class, "postRunnable 回调异常", e);
                    }
                });
            }
        }, "apple-script-dialog");
        dialogThread.setDaemon(true);
        dialogThread.start();
    }

    /**
     * 对话框退出码处理器
     */
    @FunctionalInterface
    private interface ExitCodeHandler
    {
        /**
         * 处理对话框退出码
         *
         * @param exitCode osascript 进程退出码
         */
        void handle (int exitCode);
    }
}
