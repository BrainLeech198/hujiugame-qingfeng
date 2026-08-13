package com.hujiugame.qingfeng.lwjgl3.imp;

import com.badlogic.gdx.files.FileHandle;
import com.hujiugame.qingfeng.util.interact.interfaces.ExplorerOpener;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class DesktopExplorerOpener implements ExplorerOpener
{
    @Override
    public void open (FileHandle path)
    {
        if (path == null)
        {
            LogUtils.error(DesktopExplorerOpener.class, "open Parameter is empty (path): null");
            return;
        }

        String absolutePath = path.file().getAbsolutePath();
        File file = new File(absolutePath);

        LogUtils.debug(DesktopExplorerOpener.class, "open Request path (path): " + absolutePath);
        LogUtils.debug(DesktopExplorerOpener.class, "open Is directory (isDirectory): " + path.isDirectory());

        String os = System.getProperty("os.name").toLowerCase();

        // macOS：AWT（java.awt.Desktop）要求在主线程调用，与 LWJGL3 抢占主线程（-XstartOnFirstThread）
        // 会死锁卡死游戏（Finder 关闭回焦点时 AWT 与 GLFW 互相等待）。macOS 一律走 CLI open，不初始化 AWT。
        if (!os.contains("mac") && Desktop.isDesktopSupported())
        {
            Desktop desktop = Desktop.getDesktop();
            try
            {
                if (file.isDirectory())
                {
                    desktop.open(file);
                    LogUtils.debug(DesktopExplorerOpener.class, "open Desktop.open success (dir): " + absolutePath);
                    return;
                }
                else
                {
                    // 文件则打开其父目录
                    File parent = file.getParentFile();
                    if (parent != null)
                    {
                        desktop.open(parent);
                        LogUtils.debug(DesktopExplorerOpener.class, "open Desktop.open success (parent): " + parent.getAbsolutePath());
                        return;
                    }
                }
            }
            catch (Exception e)
            {
                LogUtils.debug(DesktopExplorerOpener.class, "open Desktop API failed, fallback to CLI: " + e.getMessage());
            }
        }

        // 回退 / macOS：使用平台命令在后台线程启动，不阻塞 GL 线程
        launchCli(file, os);
    }

    /**
     * 使用平台命令在系统资源管理器中显示文件/文件夹。
     * 进程在后台守护线程启动并消费输出，即使命令挂起也不会卡死游戏主线程。
     *
     * @param file 要显示的文件或文件夹
     * @param os   小写系统名（win/mac/linux）
     */
    private void launchCli (File file, String os)
    {
        try
        {
            ProcessBuilder pb;
            if (os.contains("win"))
            {
                if (file.isDirectory())
                {
                    pb = new ProcessBuilder("explorer", file.getAbsolutePath());
                }
                else
                {
                    pb = new ProcessBuilder("explorer", "/select,", file.getAbsolutePath());
                }
            }
            else if (os.contains("mac"))
            {
                if (file.isDirectory())
                {
                    pb = new ProcessBuilder("open", file.getAbsolutePath());
                }
                else
                {
                    pb = new ProcessBuilder("open", "-R", file.getAbsolutePath());
                }
            }
            else // Linux and others
            {
                if (file.isDirectory())
                {
                    pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                }
                else
                {
                    File parent = file.getParentFile();
                    if (parent != null)
                    {
                        pb = new ProcessBuilder("xdg-open", parent.getAbsolutePath());
                    }
                    else
                    {
                        LogUtils.error(DesktopExplorerOpener.class, "launchCli Cannot get parent directory (parent): null");
                        return;
                    }
                }
            }

            // 合并 stderr 到 stdout，并丢弃输出，防止缓冲区满导致进程挂起
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            // 后台线程消费输出流并等待退出，防止缓冲区死锁，且不阻塞 GL 线程
            Thread drainThread = new Thread(() ->
            {
                try (java.io.InputStream is = process.getInputStream())
                {
                    byte[] buffer = new byte[4096];
                    while (is.read(buffer) != -1) { /* discard */ }
                }
                catch (IOException e)
                {
                    LogUtils.debug(DesktopExplorerOpener.class, "launchCli 读取输出中断: " + e.getMessage());
                }
                finally
                {
                    try
                    {
                        LogUtils.debug(DesktopExplorerOpener.class, "launchCli Process completed (exitCode): " + process.waitFor());
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "explorer-output-drain");
            drainThread.setDaemon(true);
            drainThread.start();

            LogUtils.debug(DesktopExplorerOpener.class, "launchCli Process launched (cmd): " + pb.command());
        }
        catch (Exception e)
        {
            LogUtils.error(DesktopExplorerOpener.class, "launchCli Execution exception", e);
        }
    }
}
