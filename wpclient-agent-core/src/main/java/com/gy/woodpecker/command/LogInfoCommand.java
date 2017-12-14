package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;
import java.util.Enumeration;
import java.util.List;

/**
 * @author guoyang
 * @Description: 获取应用配置日志的类，为设置日志级别服务
 * @date 2017/12/7 下午1:48
 */
@Slf4j
@Cmd(name = "loginfo", sort = 2, summary = "Get the class of the application configuration log to set up the log level service",
        eg = {
                "loginfo com.gy",
                "loginfo"
        })
public class LogInfoCommand extends AbstractCommand{
    @IndexArg(index = 0, name = "class-pattern",isRequired = false,summary = "类路径名")
    private String classPattern;

    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public void excute(Instrumentation inst) {

        String logerT = ConfigPropertyUtile.getProperties().getProperty("agent.log.name");
        StringBuffer strLog = new StringBuffer();

        if(logerT.equals("log4j")){
            getAppLog4jInfo(classPattern, strLog,inst);
        }else if(logerT.equals("logback")){
            getAppLogbackInfo(classPattern, strLog,inst);
        }

        ctxT.writeAndFlush(strLog.toString());
    }


    private static boolean getAppLog4jInfo(String filterLog, StringBuffer strLog,Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        for(Class clazz:classes){

            if(clazz.getName().equals("org.apache.log4j.LogManager")){
                try {
                    //获取的都是业务应用的加载类 因为classloader隔离，不能够直接获取对象进行转换 java.lang.ClassCastException
                    ClassLoader appClassLoader = clazz.getClassLoader();
                    final Class<?> classOfLogger = appClassLoader.loadClass("org.apache.log4j.Logger");

                    final Object objectOfCurrentLoggers  = clazz.getMethod("getCurrentLoggers").invoke(null);
                    Enumeration enumeration = (Enumeration)objectOfCurrentLoggers;
                    while (enumeration.hasMoreElements()){
                        Object loggerObj = enumeration.nextElement();
                        String logName = (String)classOfLogger.getMethod("getName").invoke(loggerObj);
                        if(StringUtils.isNotBlank(filterLog) ){
                            if(logName.startsWith(filterLog)){
                                strLog.append(logName).append("\n");
                            }
                        }else{
                            strLog.append(logName).append("\n");
                        }
                    }

                } catch (Exception e) {
                    log.info(e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    private static boolean getAppLogbackInfo(String filterLog, StringBuffer strLog,Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        for(Class clazz:classes){
            if(clazz.getName().equals("org.slf4j.LoggerFactory")){

                try {
                    //获取的都是业务应用的加载类 因为classloader隔离，不能够直接获取对象进行转换 java.lang.ClassCastException: ch.qos.logback.classic.Logger cannot be cast to ch.qos.logback.classic.Logger
                    ClassLoader appClassLoader = clazz.getClassLoader();
                    log.info(appClassLoader.toString());
                    final Object objectOfILoggerFactory = clazz.getMethod("getILoggerFactory").invoke(null);
                    final Class<?> classOfLoggerContext = appClassLoader.loadClass("ch.qos.logback.classic.LoggerContext");
                    final Class<?> classOfLogger = appClassLoader.loadClass("ch.qos.logback.classic.Logger");
                    final Object objectOfLogs = classOfLoggerContext.getMethod("getLoggerList").invoke(objectOfILoggerFactory);
                    List lists = (List)objectOfLogs;
                    for(int i=0;i<lists.size();i++){
                        Object objectOfLog = lists.get(i);
                        String logName = (String)classOfLogger.getMethod("getName").invoke(objectOfLog);
                        if(StringUtils.isNotBlank(filterLog) ){
                            if(logName.startsWith(filterLog)){
                                strLog.append(logName).append("\n");
                            }
                        }else{
                            strLog.append(logName).append("\n");
                        }
                    }
                }catch (Exception e){
                    log.info(e.getMessage());
                }

                return true;
            }
        }
        return false;
    }

}
