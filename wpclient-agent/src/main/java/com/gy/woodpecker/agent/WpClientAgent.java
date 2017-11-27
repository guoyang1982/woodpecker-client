package com.gy.woodpecker.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.util.jar.JarFile;

/**
 * Created by guoyang on 17/10/25.
 */
public class WpClientAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        main(agentArgs, inst);

    }

    public static void agentmain(String args, Instrumentation inst) {main(args, inst);
    }

    private static void main(String agentArgs, Instrumentation inst) {

        String path = WpClientAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            inst.appendToBootstrapClassLoaderSearch(
                    new JarFile(path)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(null == agentArgs || agentArgs.equals("")){
            System.out.println("传入的配置文件为空!");
            return;
        }
        final ClassLoader classLoader;

        try {
            //环境隔离 wpclient的classloader和要切入的项目进行隔离，这样所依赖的jar文件类不会互相污染
            classLoader = new AgentClassLoader(path.replace("wpclient-agent.jar","wpclient-agent-core.jar"));

            //初始化配置文件
            final Class<?> classOfConfigure = classLoader.loadClass("com.gy.woodpecker.tools.ConfigPropertyUtile");
            classOfConfigure.getMethod("initProperties",String.class).invoke(null,agentArgs);

            final Class<?> classOfLog = classLoader.loadClass("com.gy.woodpecker.log.LoggerFacility");
            final Object objectOfLog = classOfLog.getMethod("getInstall",Instrumentation.class).invoke(null,inst);
            //启动日志收集
            LoggerFactoryProx.init(classOfLog,objectOfLog);
            //初始化 监控端口
            final Class<?> classOfNetty = classLoader.loadClass("com.gy.woodpecker.netty.NettyFactory");
            classOfNetty.getMethod("init").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
