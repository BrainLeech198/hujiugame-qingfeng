package com.hujiugame.qingfeng;

import com.badlogic.gdx.*;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.hujiugame.qingfeng.di.InstanceContent;
import com.hujiugame.qingfeng.core.GameHost;
import com.hujiugame.qingfeng.core.UpdateChecker;
import com.hujiugame.qingfeng.type.Numeric;
import com.hujiugame.qingfeng.type.file.FileName;
import com.hujiugame.qingfeng.type.file.PathName;
import com.hujiugame.qingfeng.type.ui.UseViewport;
import com.hujiugame.qingfeng.input.ControllerInputHandler;
import com.hujiugame.qingfeng.input.KeyboardInputHandler;
import com.hujiugame.qingfeng.input.VirtualInputHandler;
import com.hujiugame.qingfeng.manager.UserConfigManager;
import com.hujiugame.qingfeng.util.json.parser.JsonTextParser;
import com.hujiugame.qingfeng.util.system.CrashUtils;
import com.hujiugame.qingfeng.util.system.FileUtils;
import com.hujiugame.qingfeng.util.system.LogUtils;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * {@link ApplicationListener} 全平台一套代码
 */
public class Main extends ApplicationAdapter
{
    // 静态参数
    // ===================================================================================================================
    // 命令行参数
    private List<String> argsList;

    // libGDX类初始化
    // ===================================================================================================================
    // 使用的视口
    private UseViewport useViewport = UseViewport.STRETCH;

    // 舞台
    private Stage stage;

    // 画笔
    private SpriteBatch spriteBatch;

    // 输入管理类
    private InputMultiplexer inputMultiplexer;

    // 实例管理类
    // ===================================================================================================================
    private InstanceContent instanceContent;

    // 逻辑初始化
    // ===================================================================================================================
    // 控制类
    private UpdateChecker updateChecker;
    private GameHost gameHost;

    // 懒初始化
    private volatile boolean slashed = false;
    private volatile boolean lazyInitialized = false;

    // 子线程引用
    private Thread inputAdapterThread;
    private Thread updateVersionThread;

    // 构造函数
    // =====================================================================================================================

    /**
     * 无参构造函数
     */
    public Main ()
    {
        argsList = null;
        System.out.println("Application start with no args");
    }

    /**
     * 带参数构造函数
     *
     * @param args 命令行参数
     */
    public Main (String[] args)
    {
        argsList = Arrays.asList(args);
        if (!argsList.isEmpty())
            System.out.println("Application start with args : " + argsList);
        else
            System.out.println("Application start with 0 args");
    }

    /**
     * 设置命令行参数（仅在首次设置有效）
     *
     * @param args 命令行参数
     */
    public void setArgsList (String[] args)
    {
        if (argsList != null) return;
        argsList = Arrays.asList(args);
        System.out.println("Application set args : " + argsList);
    }

    // =====================================================================================================================

    /**
     * 初始化应用，包括日志系统、libGDX 基础组件，延迟其余初始化到首次渲染循环后
     */
    @Override
    public void create ()
    {
        // 全局替换 FileHandle 为 QfFileHandle，使 toString() 输出 "type:path"
        // 注意: QfFiles 包装引入路径翻倍问题，暂时禁用
        // Gdx.files = new QfFiles(Gdx.files);

        // 启动日志
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String logDateTime = dateTime.format(formatter);
        String gdxString = logDateTime + "] [" + "INFO " + "] [" + "Main";
        Gdx.app.log(gdxString, "Current file.encoding: " + Charset.defaultCharset().displayName());
        Gdx.app.log(gdxString, "Application start");

        // TODO:读取上次是否成功启动


        // 初始化
        try
        {
            // logger日志初始化
            if (!LogUtils.init(FileUtils.pathJoin(PathName.BASE, PathName.LOG, FileName.LOG_CONFIG)))
            {
                LogUtils.error(Main.class, "create LogUtils初始化失败");
                throw new Exception("LogUtils初始化失败");
            }
            else
            {
                LogUtils.debug(Main.class, "create LogUtils初始化成功");
            }

            // libGDX类初始化
            if (!initLibGDX())
            {
                LogUtils.error(Main.class, "create initLibGDX初始化失败");
                throw new Exception("libGDX类初始化失败");
            }
            else
            {
                LogUtils.debug(Main.class, "create initLibGDX初始化成功");
            }
        }
        catch (Exception e)
        {
            CrashUtils.safeCrash(e);
            return;
        }

        LogUtils.debug(Main.class, "create 快速启动完成，剩余初始化延迟到首次render循环后");
    }

    /**
     * 初始化 libGDX 基础组件（画笔、视窗、舞台），分辨率/视窗由 UserConfigManager.initDisplayConfig 确定
     *
     * @return 是否初始化成功
     */
    private boolean initLibGDX ()
    {
        try
        {
            try
            {
                // 基础组件:画笔
                spriteBatch = new SpriteBatch();
            }
            catch (IllegalArgumentException e)
            {
                // 错误信息
                StringBuilder errorMessage = new StringBuilder();

                // 捕获并打印错误，获取更详细的着色器编译信息
                LogUtils.error(Main.class, "initLibGDX SpriteBatch创建失败", e);

                // 这里可能还包含一个来自 ShaderProgram 内部的错误日志，需要一起打印
                Throwable cause = e.getCause();
                while (cause != null)
                {
                    LogUtils.error(Main.class, "initLibGDX SpriteBatch创建失败 Caused by: " + cause.getMessage());
                    errorMessage.append(" Caused by: ").append(cause.getMessage());
                    cause = cause.getCause();
                }

                // 尝试获取着色器编译日志
                try
                {
                    // 反射或直接尝试编译默认着色器
                    ShaderProgram.pedantic = false;
                    ShaderProgram defaultShader = SpriteBatch.createDefaultShader();
                    if (!defaultShader.isCompiled())
                    {
                        LogUtils.error(Main.class, "默认着色器编译日志: " + defaultShader.getLog());
                        errorMessage.append(" 着色器编译日志: ").append(defaultShader.getLog());
                    }
                }
                catch (Exception ex)
                {
                    LogUtils.error(Main.class, "无法获取着色器编译日志", ex);
                    errorMessage.append(" 无法获取着色器编译日志");
                }

                // 处理错误，例如抛出一个运行时异常以便开发者知晓
                throw new RuntimeException("无法初始化SpriteBatch，请检查OpenGL环境。" + errorMessage, e);
            }

            // 初始化显示配置（特例函数）：读取user_config、应用分辨率、确定视窗
            useViewport = UserConfigManager.initDisplayConfig();

            // 给舞台stage配置使用的视窗
            stage = new Stage(useViewport.getViewport(), spriteBatch);

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(Main.class, "initLibGDX", e);
            throw e;
        }
    }

    // ===================================================================================================================

    /**
     * 每帧渲染循环，处理清屏、懒初始化、主绘制和顶层绘制
     */
    @Override
    public void render ()
    {
        try
        {
            // 获取帧时间增量
            float deltaTime = Gdx.graphics.getDeltaTime();

            // 清屏 + 适配视口（每帧起点，固定流程）
            ScreenUtils.clear(0, 0, 0, 1f); // 黑色背景，可配置为主题色
            stage.getViewport().apply(); // 适配窗口大小变化
            spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined); // 绑定相机矩阵

            // 懒初始化
            if (!isLazyInitialized())
            {
                if (!isSlashed()) slash();
                else lazyInit();
                return;
            }

            // 主绘制
            mainRender(deltaTime);

            // 顶层绘制
            topRender(deltaTime);
        }
        catch (Throwable e)
        {
            CrashUtils.safeCrash(e);
        }
    }

    /**
     * 主渲染逻辑，包含精灵批次、游戏逻辑、舞台动画的更新和绘制
     *
     * @param deltaTime 帧时间增量
     */
    private void mainRender (float deltaTime)
    {
        // 开启渲染 Batch
        spriteBatch.begin();

        // 调用渲染逻辑
        gameHost.run(deltaTime);

        // 处理 Stage 输入和动画
        stage.act(deltaTime);

        // 结束 Batch 渲染
        spriteBatch.end();

        // 绘制 Stage
        stage.draw();
    }

    /**
     * 顶层绘制逻辑，绘制覆盖层内容（如虚拟输入提示）
     */
    private void topRender (float deltaTime)
    {
        stage.getViewport().apply(); // 适配窗口大小变化
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined); // 绑定相机矩阵

        // 开启渲染 Batch
        spriteBatch.begin();

        // 调用渲染逻辑
        instanceContent.topRender();

        // 结束 Batch 渲染
        spriteBatch.end();
    }

    /**
     * 应用暂停回调
     */
    @Override
    public void pause ()
    {
        LogUtils.info(Main.class, "pause 应用暂停");
    }

    /**
     * 应用恢复回调
     */
    @Override
    public void resume ()
    {
        LogUtils.info(Main.class, "resume 应用恢复");
    }

    /**
     * 窗口大小变化回调，更新舞台视口
     *
     * @param width  新宽度
     * @param height 新高度
     */
    @Override
    public void resize (int width, int height)
    {
        stage.getViewport().update(width, height, true);
    }

    /**
     * 释放所有资源，关闭子线程、释放画笔和舞台，并退出应用
     */
    @Override
    public void dispose ()
    {
        // 关闭子线程
        interruptAndJoinThread(inputAdapterThread);
        interruptAndJoinThread(updateVersionThread);

        if (spriteBatch != null) spriteBatch.dispose();
        if (stage != null) stage.dispose();

        InstanceContent instance = InstanceContent.getInstance();
        if (instance != null) instance.dispose();

        LogUtils.info(Main.class, "dispose() 释放完成");
        Gdx.app.exit();
    }

    // ===================================================================================================================

    /**
     * 是否已闪屏
     *
     * @return 是否已闪屏
     */
    private boolean isSlashed ()
    {
        return slashed;
    }

    /**
     * 设置闪屏完成状态
     *
     * @param slashed 是否已闪屏
     */
    private void setSlashed (boolean slashed)
    {
        this.slashed = slashed;
    }

    /**
     * 是否已完成懒初始化
     *
     * @return 是否已完成懒初始化
     */
    private boolean isLazyInitialized ()
    {
        return lazyInitialized;
    }

    /**
     * 设置懒初始化完成状态
     *
     * @param lazyInitialized 是否已完成懒初始化
     */
    private void setLazyInitialized (boolean lazyInitialized)
    {
        this.lazyInitialized = lazyInitialized;
    }

    /**
     * 闪屏处理，标记闪屏阶段完成
     */
    private void slash ()
    {
        setSlashed(true);
    }

    /**
     * 懒初始化（在首次 render 循环后执行，保证窗口管理器已识别渲染窗口），
     * 包括实例注入、输入适配、解析器初始化、差异更新线程和游戏初始化
     */
    private void lazyInit ()
    {
        LogUtils.debug(Main.class, "lazyInit 懒加载初始化");

        stage.getViewport().apply(); // 适配窗口大小变化
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);

        // 懒初始化
        try
        {
            // 注入实现类（原create中迁移至此）
            if (!initInstance())
            {
                LogUtils.error(Main.class, "lazyInit initInstance注入实现类失败");
                throw new Exception("lazyInit initInstance注入实现类失败");
            }
            else
            {
                LogUtils.debug(Main.class, "lazyInit initInstance注入实现类成功");
            }

            // 输入初始化
            if (!initInputAdapter())
            {
                LogUtils.error(Main.class, "lazyInit initInputAdapter初始化输入管理类失败");
                throw new Exception("lazyInit initInputAdapter初始化输入管理类失败");
            }
            else
            {
                LogUtils.debug(Main.class, "lazyInit initInputAdapter初始化输入管理类成功");
            }

            // 设置解析器
            if (!initParser())
            {
                LogUtils.error(Main.class, "lazyInit initParser初始化解析器失败");
                throw new Exception("lazyInit initParser初始化解析器失败");
            }
            else
            {
                LogUtils.debug(Main.class, "lazyInit initParser初始化解析器成功");
            }

            // 线程化差异更新
            if (!threadUpdateVersion())
            {
                LogUtils.error(Main.class, "lazyInit threadUpdateVersion线程化差异更新失败");
                throw new Exception("lazyInit threadUpdateVersion线程化差异更新失败");
            }
            else
            {
                LogUtils.debug(Main.class, "lazyInit threadUpdateVersion线程化差异更新成功");
            }

            // 初始化游戏（这里可能会调用 Init 状态机，耗时最长）
            if (!gameHost.init())
            {
                LogUtils.error(Main.class, "lazyInit gameHost.init()游戏创建失败");
                throw new Exception("lazyInit gameHost.init()游戏创建失败");
            }
            else
            {
                LogUtils.debug(Main.class, "lazyInit gameHost.init()游戏创建成功");
            }
        }
        catch (Exception e)
        {
            CrashUtils.safeCrash(e);
        }

        setLazyInitialized(true);
        LogUtils.debug(Main.class, "lazyInit 懒加载初始化完成");
    }

    /**
     * 初始化 InstanceContent 实例管理器并获取控制器引用
     *
     * @return 是否初始化成功
     */
    private boolean initInstance ()
    {
        try
        {
            // 获取用户主目录作为根目录（直接使用系统属性，避免 QfFiles 包装引入路径异常）
            String rootPath = System.getProperty("user.home");
            LogUtils.info(Main.class, "getRootAbsolutePath 获取根目录 (rootPath): " + rootPath);
            LogUtils.info(Main.class, "getRootAbsolutePath 资源总目录 (gamePath): " + FileUtils.pathJoin(rootPath, PathName.BASE).replace("/", "\\"));

            // 初始化实例管理器
            if (!InstanceContent.init(rootPath, useViewport, spriteBatch, stage))
            {
                LogUtils.error(Main.class, "initInstance instanceContent单例管理器初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(Main.class, "initInstance instanceContent单例管理器初始化成功");
            }

            instanceContent = InstanceContent.getInstance();

            updateChecker = instanceContent.getUpdateChecker();
            gameHost = instanceContent.getGameHost();

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(Main.class, "initInstance", e);
            throw e;
        }
    }

    /**
     * 初始化输入适配器，创建并注册键盘、手柄和虚拟输入处理器
     *
     * @return 是否初始化成功
     */
    private boolean initInputAdapter ()
    {
        try
        {
            // 初始化输入适配器
            inputMultiplexer = new InputMultiplexer();

            // 舞台输入
            inputMultiplexer.addProcessor(stage);

            // 设置输入器
            Gdx.input.setInputProcessor(inputMultiplexer);

            inputAdapterThread = new Thread(() ->
            {
                // 必须等待主线程的 Controllers 初始化完成（通常在 create 之后）
                Gdx.app.postRunnable(() ->
                {
                    // 虚拟化输入模拟器
                    VirtualInputHandler virtualInputHandler = new VirtualInputHandler(instanceContent);
                    instanceContent.setVirtualInputHandler(virtualInputHandler);

                    // 键盘输入
                    KeyboardInputHandler keyboardInputHandler = new KeyboardInputHandler(instanceContent, virtualInputHandler);
                    instanceContent.setKeyboardInputHandler(keyboardInputHandler);
                    inputMultiplexer.addProcessor(keyboardInputHandler);

                    // 手柄输入
                    ControllerInputHandler controllerHandler = new ControllerInputHandler(instanceContent, virtualInputHandler);
                    instanceContent.setControllerInputHandler(controllerHandler);
                    Controllers.addListener(controllerHandler);

                    // 设置输入器
                    Gdx.input.setInputProcessor(inputMultiplexer);
                });
            });
            inputAdapterThread.setDaemon(true);
            inputAdapterThread.setUncaughtExceptionHandler((t, e) ->
            {
                LogUtils.error(Main.class, "inputAdapterThread异常", (Exception) e);
                CrashUtils.safeCrash(new RuntimeException("输入线程崩溃: " + e.getMessage(), e));
            });
            inputAdapterThread.start();

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(Main.class, "initInputAdapter", e);
            throw e;
        }
    }

    /**
     * 初始化文字解析器
     *
     * @return 是否初始化成功
     */
    private boolean initParser ()
    {
        try
        {
            // 文字解析器
            if (!JsonTextParser.init(instanceContent.getLanguageManager()))
            {
                LogUtils.error(Main.class, "initParser 文字解析器初始化失败");
                return false;
            }
            else
            {
                LogUtils.debug(Main.class, "initParser 文字解析器初始化成功");
            }

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(Main.class, "initParser", e);
            throw e;
        }
    }

    /**
     * 在新线程中执行游戏版本差异更新检测
     *
     * @return 是否成功启动更新线程
     */
    public boolean threadUpdateVersion ()
    {
        try
        {
            updateVersionThread = new Thread(() ->
            {
                if (!updateChecker.init())
                {
                    Gdx.app.postRunnable(() ->
                    {
                        LogUtils.error(Main.class, "threadUpdateVersion updateChecker.init() 游戏更新检测失败");
                        CrashUtils.safeCrash(new RuntimeException("threadUpdateVersion updateChecker.init() 游戏更新检测失败"));
                    });
                }
                else
                {
                    Gdx.app.postRunnable(() ->
                        LogUtils.debug(Main.class, "threadUpdateVersion updateChecker.init() 游戏更新检测成功"));
                }
            });
            updateVersionThread.setDaemon(true);
            updateVersionThread.setUncaughtExceptionHandler((t, e) ->
            {
                LogUtils.error(Main.class, "updateVersionThread异常", (Exception) e);
            });
            updateVersionThread.start();

            return true;
        }
        catch (Exception e)
        {
            LogUtils.error(Main.class, "threadUpdateVersion", e);
            throw e;
        }
    }

    // ===================================================================================================================

    /**
     * 中断并等待线程结束
     *
     * @param thread 要中断的线程
     */
    private void interruptAndJoinThread (Thread thread)
    {
        if (thread != null && thread.isAlive())
        {
            thread.interrupt();
            try
            {
                thread.join(Numeric.Time.THREAD_JOIN_MS);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

}
