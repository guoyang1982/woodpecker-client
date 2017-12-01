package com.gy.woodpecker.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.alibaba.fastjson.JSONObject;
import com.gy.woodpecker.message.MessageBean;
import com.gy.woodpecker.redis.RedisClient;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import com.gy.woodpecker.tools.IPUtile;
import com.gy.woodpecker.transformer.WoodpeckTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by guoyang on 17/10/27.
 */
@Slf4j
public class LoggerFacility {
    private static volatile LoggerFacility loggerFacility;

    private static Instrumentation inst;
    protected RedisClient redisClient;

    private volatile String  appName;

    private volatile String healthCheck = "true";

    public volatile boolean telHealthCheck = true;

    private int healthCheckDelay = 10000;

    public static volatile boolean f = true;

    private static int corePoolSize = 4;
    private static int maximumPoolSize = 4;
    private static int keepAliveTime = 10;
    private static int queueCount = 5000;

    //private static ArrayBlockingQueue arrayBlockingQueue = new ArrayBlockingQueue<Runnable>(queueCount);
    private static ThreadPoolExecutor executorPools =
            new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(queueCount),
            new MessageRejectedExecutionHandler());

    public static String threadPoolsMonitor(){
        int activeCount = executorPools.getActiveCount();
        long completedTaskCount = executorPools.getCompletedTaskCount();
        int corePoolSize = executorPools.getCorePoolSize();
        int maximumPoolSize = executorPools.getMaximumPoolSize();
        long taskCount = executorPools.getTaskCount();
        int poolSize = executorPools.getPoolSize();
        int queueSize = executorPools.getQueue().size();
        StringBuffer str = new StringBuffer();
        str.append("the activeCount=").append(activeCount).append("\r\n").append("the completedTaskCount=")
                .append(completedTaskCount).append("\r\n").append("the corePoolSize=").append(corePoolSize)
                .append("\r\n").append("the maximumPoolSize=").append(maximumPoolSize).append("\r\n").append("the taskCount=")
                .append(taskCount).append("\r\n").append("the poolSize=").append(poolSize).append("\r\n").append("the queueSize=")
                .append(queueSize);
        return str.toString();
    }


    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    private LoggerFacility(){
        try{
            initConfig();

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void initConfig() {
        this.appName = ConfigPropertyUtile.getProperties().getProperty("application.name");
        String healthCheckT = ConfigPropertyUtile.getProperties().getProperty("redis.health.Check");
        if(null != healthCheckT && !healthCheckT.equals("")){
            this.healthCheck = healthCheckT;
        }
        String delay = ConfigPropertyUtile.getProperties().getProperty("redis.health.check.delay");
        if(null != delay && !delay.equals("")){
            this.healthCheckDelay = Integer.parseInt(delay);
        }
        String corePoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.corePoolSize");
        if(null != corePoolSizeT && !corePoolSizeT.equals("")){
            this.corePoolSize = Integer.parseInt(corePoolSizeT);
        }
        String maximumPoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.maximumPoolSize");
        if(null != maximumPoolSizeT && !maximumPoolSizeT.equals("")){
            this.maximumPoolSize = Integer.parseInt(maximumPoolSizeT);
        }
        String keepAliveTimeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.keepAliveTime");
        if(null != keepAliveTimeT && !keepAliveTimeT.equals("")){
            this.keepAliveTime = Integer.parseInt(keepAliveTimeT);
        }
        String queueCountT = ConfigPropertyUtile.getProperties().getProperty("log.thread.queue.count");
        if(null != queueCountT && !queueCountT.equals("")){
            this.queueCount = Integer.parseInt(queueCountT);
        }

        //初始化 redis
        redisClient = new RedisClient();
        redisClient.init();
        //启动健康检查
        healthCheck();
        //处理要插桩的类
        inst.addTransformer(new WoodpeckTransformer());
    }

    /**
     *
     * @return
     */
    public static LoggerFacility getInstall(Instrumentation instT){
        if(null == loggerFacility){
            synchronized (LoggerFacility.class){
                if(null == loggerFacility){
                    if(null != instT){

                        inst = instT;
                    }
                    loggerFacility = new LoggerFacility();
                }
            }
        }

        return loggerFacility;
    }

    /**
     * 发送消息
     * @param msg
     */
    public void sendToRedis(final String msg) {
        log.info("发送异常日志消息!"+msg);

        if (!f) {
            log.info("redis集群不健康, 不处理操作!");
            return;
        }
        if(null == appName || appName.equals("")){
            log.info("应用名为空, 不处理操作!");
            return;
        }
        executorPools.execute(new Runnable() {
            public void run() {
                try {
                    MessageBean messageBean = new MessageBean();
                    messageBean.setAppName(appName);
                    messageBean.setIp(IPUtile.getIntranetIP());
                    messageBean.setMsg(msg);
                    messageBean.setCreateTime(timeForNow());

                    redisClient.rightPush(appName, JSONObject.toJSONString(messageBean));
                } catch (Exception e) {
                    log.info("发送异常日志消息失败!"+e.getMessage());
                }

            }
        });
    }
    private String timeForNow(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return format.format(now);
    }

    /**
     * 打印拒绝任务时 线程池的详细信息
     */
    private static class MessageRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 监控
            if (log.isInfoEnabled()) {
                log.info("LoggerFacility rejectedExecution, ThreadPoolExecutor:{}", executor.toString());
           }
        }
    }

    public static String getLogInfor(String filterLog){
        String logerT = ConfigPropertyUtile.getProperties().getProperty("agent.log.name");
        StringBuffer strLog = new StringBuffer();

        if(logerT.equals("log4j")){
            if (getAppLog4jInfo(filterLog, strLog)) return strLog.toString();
        }else if(logerT.equals("logback")){
            if (getAppLogbackInfo(filterLog, strLog)) return strLog.toString();
        }

        return strLog.toString();
    }

    private static boolean getAppLogbackInfo(String filterLog, StringBuffer strLog) {
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
                                    strLog.append(logName).append("\r\n");
                                }
                            }else{
                                strLog.append(logName).append("\r\n");
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

    private static boolean getAppLog4jInfo(String filterLog, StringBuffer strLog) {
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
                                strLog.append(logName).append("\r\n");
                            }
                        }else{
                            strLog.append(logName).append("\r\n");
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

    public static void setLogLevel(String className,String level){
        String logerT = ConfigPropertyUtile.getProperties().getProperty("agent.log.name");
        if(logerT.equals("log4j")){
            setAppLog4jLevel(className, level);
        }else if(logerT.equals("logback")) {
            setAppLogbackLevel(className, level);
        }
    }

    private static void setAppLogbackLevel(String className, String level) {
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

    private static void setAppLog4jLevel(String className, String level) {
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


    public void healthCheck(){
        if(healthCheck.equals("false")){
            return;
        }
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long delay = healthCheckDelay;
        long initDelay = 0;
        executor.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        if(!telHealthCheck){
                            return;
                        }
                        log.info("执行redis健康检查!");
                        try{
                            if(null != redisClient){
                                redisClient.set(appName+"-ping","1",1);
                            }
                            f = true;
                        }catch (Exception e){
                            log.info("redis健康检查异常,{}",e);
                            f = false;
                        }
                    }
                },
                initDelay,
                delay,
                TimeUnit.MILLISECONDS);
    }
}
