package com.gy.woodpecker.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.alibaba.fastjson.JSONObject;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.message.MessageBean;
import com.gy.woodpecker.redis.RedisClient;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import com.gy.woodpecker.tools.IPUtile;
import com.gy.woodpecker.tools.MsgPackUtil;
import com.gy.woodpecker.transformer.SpyTransformer;
import com.gy.woodpecker.transformer.WoodpeckTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by guoyang on 17/10/27.
 */
@Slf4j
public class LoggerFacility {
    private static volatile LoggerFacility loggerFacility;

    //public static Instrumentation inst;
    protected RedisClient redisClient;

    private volatile String appName;

    private volatile String healthCheck = "true";

    public static volatile boolean f = true;
    //客户端关闭发送日志命令
    public static volatile boolean slog = true;

    private static int corePoolSize = 4;
    private static int maximumPoolSize = 4;
    private static int keepAliveTime = 10;
    private static int queueCount = 5000;

    //private static ArrayBlockingQueue arrayBlockingQueue = new ArrayBlockingQueue<Runnable>(queueCount);
    private static ThreadPoolExecutor executorPools =
            new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(queueCount),
                    new MessageRejectedExecutionHandler());

    public static String threadPoolsMonitor() {
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

    private LoggerFacility() {
        try {
            initConfig();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initConfig() {
        this.appName = ConfigPropertyUtile.getProperties().getProperty("application.name");
        String healthCheckT = ConfigPropertyUtile.getProperties().getProperty("redis.health.Check");
        if (null != healthCheckT && !healthCheckT.equals("")) {
            this.healthCheck = healthCheckT;
        }

        String corePoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.corePoolSize");
        if (null != corePoolSizeT && !corePoolSizeT.equals("")) {
            this.corePoolSize = Integer.parseInt(corePoolSizeT);
        }
        String maximumPoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.maximumPoolSize");
        if (null != maximumPoolSizeT && !maximumPoolSizeT.equals("")) {
            this.maximumPoolSize = Integer.parseInt(maximumPoolSizeT);
        }
        String keepAliveTimeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.keepAliveTime");
        if (null != keepAliveTimeT && !keepAliveTimeT.equals("")) {
            this.keepAliveTime = Integer.parseInt(keepAliveTimeT);
        }
        String queueCountT = ConfigPropertyUtile.getProperties().getProperty("log.thread.queue.count");
        if (null != queueCountT && !queueCountT.equals("")) {
            this.queueCount = Integer.parseInt(queueCountT);
        }

        //启动redis
        RedisClient.RedisClientInstance.init();
        //启动健康检查
        if (!healthCheck.equals("false")) {
            RedisClient.RedisClientInstance.healthCheck(appName);
        }


        //处理要插桩的类
        System.out.println("?????????处理要插桩的类");
        ContextConfig.inst.addTransformer(new WoodpeckTransformer());
        //inst.addTransformer(new WoodpeckTransformer());
    }

    /**
     * @return
     */
    public static LoggerFacility getInstall() {
        if (null == loggerFacility) {
            synchronized (LoggerFacility.class) {
                if (null == loggerFacility) {
//                    if(null != instT){
//
//                        inst = instT;
//                    }
                    loggerFacility = new LoggerFacility();
                }
            }
        }

        return loggerFacility;
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendToRedis(final String msg) {
        log.info("发送异常日志消息!" + msg);

        if (!slog) {
            log.info("客户端关闭了发送, 不处理操作!");
            return;
        }
        if (!f) {
            log.info("redis集群不健康, 不处理操作!");
            return;
        }
        if (null == appName || appName.equals("")) {
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

                    //RedisClient.RedisClientInstance.rightPush(appName, JSONObject.toJSONString(messageBean));
                    byte[] msg = MsgPackUtil.toBytes(messageBean);
                    RedisClient.RedisClientInstance.rightPushBytes(appName,msg);
                } catch (Exception e) {
                    log.info("发送异常日志消息失败!" + e.getMessage());
                }

            }
        });
    }

    public static void main(String args[]) throws Exception{
        // Create serialize objects.
//        List<String> src = new ArrayList<String>();
//        src.add("msgpack");
//        src.add("kumofs");
//        src.add("viver");

// Serialize
//        byte[] raw = msgpack.write(src);

// Deserialize directly using a template
//        List<String> dst1 = msgpack.read(raw, Templates.tList(Templates.TString));
//        System.out.println(dst1.get(0));
//        System.out.println(dst1.get(1));
//        System.out.println(dst1.get(2));

// Or, Deserialze to Value then convert type.
//        Value dynamic = msgpack.read(raw);
//        List<String> dst2 = new Converter(dynamic)
//                .read(Templates.tList(Templates.TString));
//        System.out.println(dst2.get(0));
//        System.out.println(dst2.get(1));
//        System.out.println(dst2.get(2));

        MessageBean messageBean = new MessageBean();
        messageBean.setAppName("lms");
        messageBean.setIp(IPUtile.getIntranetIP());
        messageBean.setMsg("ssssssssdffg");
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        //messageBean.setCreateTime(format.format(now));
        byte[] raw1 = MsgPackUtil.toBytes(messageBean);
        System.out.println(new String(raw1));
        MessageBean obj = MsgPackUtil.toObject(raw1,MessageBean.class);
        System.out.println(obj.getAppName());
    }

    private String timeForNow() {
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
}
