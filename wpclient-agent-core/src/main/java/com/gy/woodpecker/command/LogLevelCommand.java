package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 设置应用类的日志级别
 * @date 2017/12/7 下午1:56
 */
@Slf4j
@Cmd(name = "loglevel", sort = 3, summary = "设置应用类的日志级别",
        eg = {
                "loglevel com.gy.woo.classname debug",
                "loglevel root debug",
                "loglevel com.gy.woo.classname info",
                "loglevel com.gy.woo.classname warn",
                "loglevel com.gy.woo.classname error"
        })
public class LogLevelCommand implements Command{
    @IndexArg(index = 0, name = "class-pattern",isRequired = false,summary = "类路径名")
    private String classPattern;
    @IndexArg(index = 1, name = "level",isRequired = false,summary = "日志级别")
    private String level;
    @Override
    public void doAction(ChannelHandlerContext ctx, Instrumentation inst) {
        if(StringUtils.isBlank(classPattern) || StringUtils.isBlank(level)){
            ctx.writeAndFlush("参数错误!\r\n");
            return;
        }
        String logerT = ConfigPropertyUtile.getProperties().getProperty("agent.log.name");
        if(logerT.equals("log4j")){
            setAppLog4jLevel(classPattern, level,inst);
        }else if(logerT.equals("logback")) {
            setAppLogbackLevel(classPattern, level,inst);
        }
        ctx.writeAndFlush("设置日志成功!\r\n");
    }

    private static void setAppLogbackLevel(String className, String level,Instrumentation inst) {
        //获取业务系统所有已经加载的类
        Class[] classes = inst.getAllLoadedClasses();
        for (Class clazz : classes) {
            if (clazz.getName().equals("org.slf4j.LoggerFactory")) {
                try {
                    //获取的都是业务应用的加载类 因为classloader隔离，不能够直接获取对象进行转换 java.lang.ClassCastException: ch.qos.logback.classic.Logger cannot be cast to ch.qos.logback.classic.Logger
                    ClassLoader appClassLoader = clazz.getClassLoader();
                    log.info(appClassLoader.toString());
                    final Class<?> classOfLoggerContext = appClassLoader.loadClass("ch.qos.logback.classic.LoggerContext");
                    final Class<?> classOfLogger = appClassLoader.loadClass("ch.qos.logback.classic.Logger");
                    final Class<?> classOfLevel = appClassLoader.loadClass("ch.qos.logback.classic.Level");

                    final Object objectOfILoggerFactory = clazz.getMethod("getILoggerFactory").invoke(null);
                    //获取logger 对像
                    final Object objectOfLogger = classOfLoggerContext.getMethod("getLogger",String.class).invoke(objectOfILoggerFactory,className);
                    if(null != objectOfLogger){
                        //获取level对象
                        final Object objectOfLevel = classOfLevel.getMethod("toLevel",String.class).invoke(null,level);
                        classOfLogger.getMethod("setLevel",classOfLevel).invoke(objectOfLogger,objectOfLevel);
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    private static void setAppLog4jLevel(String className, String level,Instrumentation inst) {
        //获取业务系统所有已经加载的类
        Class[] classes = inst.getAllLoadedClasses();
        for (Class clazz : classes) {
            if (clazz.getName().equals("org.apache.log4j.LogManager")) {
                try {
                    //获取的都是业务应用的加载类 因为classloader隔离，不能够直接获取对象进行转换
                    ClassLoader appClassLoader = clazz.getClassLoader();
                    log.info(appClassLoader.toString());
                    final Class<?> classOfLogger = appClassLoader.loadClass("org.apache.log4j.Logger");
                    final Class<?> classOfLevel = appClassLoader.loadClass("org.apache.log4j.Level");
                    //获取logger 对像
                    final Object objectOfLogger = clazz.getMethod("exists",String.class).invoke(null,className);
                    if(null != objectOfLogger){
                        //获取level对象
                        final Object objectOfLevel = classOfLevel.getMethod("toLevel",String.class).invoke(null,level);
                        classOfLogger.getMethod("setLevel",classOfLevel).invoke(objectOfLogger,objectOfLevel);
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

}
