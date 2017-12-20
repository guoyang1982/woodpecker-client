package com.gy.woodpecker.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.util.jar.JarFile;

/**
 * Created by guoyang on 17/10/25.
 */
public class WpClientAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        main(agentArgs, inst, true);

    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst, false);
    }

    private static void main(String agentArgs, Instrumentation inst, boolean premain) {

        String path = WpClientAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            inst.appendToBootstrapClassLoaderSearch(
                    new JarFile(path)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == agentArgs || agentArgs.equals("")) {
            System.out.println("传入的配置文件为空!");
            return;
        }
        final ClassLoader classLoader;

        try {
            //环境隔离 wpclient的classloader和要切入的项目进行隔离，这样所依赖的jar文件类不会互相污染
            classLoader = new AgentClassLoader(path.replace("wpclient-agent.jar", "wpclient-agent-core.jar"));

            //初始化配置文件
            final Class<?> classOfConfigure = classLoader.loadClass("com.gy.woodpecker.tools.ConfigPropertyUtile");
            classOfConfigure.getMethod("initProperties", String.class).invoke(null, agentArgs);

            //保存inst
            final Class<?> classOfContextConfig = classLoader.loadClass("com.gy.woodpecker.config.ContextConfig");
            classOfContextConfig.getMethod("setInst", Instrumentation.class).invoke(null, inst);

            if (premain) {
                //启动日志收集
                final Class<?> classOfLog = classLoader.loadClass("com.gy.woodpecker.log.LoggerFacility");
                final Object objectOfLog = classOfLog.getMethod("getInstall").invoke(null);
                //代理日志收集类
                LoggerFactoryProx.init(classOfLog, objectOfLog);
            }

            //初始化 监控端口
            final Class<?> classOfNetty = classLoader.loadClass("com.gy.woodpecker.netty.NettyFactory");
            classOfNetty.getMethod("init").invoke(null);
            // 获取各种Hook
            final Class<?> adviceWeaverClass = classLoader.loadClass("com.gy.woodpecker.weaver.AdviceWeaver");
            // 初始化间谍
            Spy.initForAgentLauncher(
                    classLoader,
                    adviceWeaverClass.getMethod("methodOnBegin",
                            int.class,
                            ClassLoader.class,
                            String.class,
                            String.class,
                            String.class,
                            Object.class,
                            Object[].class),
                    adviceWeaverClass.getMethod("methodOnEnd",
                            int.class,
                            ClassLoader.class,
                            String.class,
                            String.class,
                            String.class,
                            Object.class,
                            Object[].class,
                            Object.class),
                    null,
                    adviceWeaverClass.getMethod("methodOnInvokeBeforeTracing",
                            int.class,
                            int.class,
                            String.class,
                            String.class,
                            String.class),
                    adviceWeaverClass.getMethod("methodOnInvokeAfterTracing",
                            int.class,
                            int.class,
                            String.class,
                            String.class,
                            String.class),
                    adviceWeaverClass.getMethod("methodOnInvokeThrowTracing",
                            int.class,
                            int.class,
                            String.class,
                            String.class,
                            String.class,
                            Object.class),
                    null,
                    adviceWeaverClass.getMethod("printMethod",
                            int.class,
                            ClassLoader.class,
                            String.class,
                            String.class,
                            Object.class
                    )
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
