package com.gy.woodpecker.redis;

import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.log.Logstatic;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by guoyang on 17/10/27.
 */
@Slf4j
public class RedisClient {
    public static RedisClient RedisClientInstance = new RedisClient();
    JedisCluster jedisCluster = null;
    //连接超时
    int conTimeoutR = 1000;
    //读超时
    int soTimeoutR = 1000;
    //最大重试次数
    int maxAttemptsR = 1;

    public volatile boolean telHealthCheck = true;

    private int healthCheckDelay = 10000;

    private RedisClient() {
    }

    public void init() {
        log.info("init redis!!!!!!");
        String delay = ConfigPropertyUtile.getProperties().getProperty("redis.health.check.delay");
        if(null != delay && !delay.equals("")){
            this.healthCheckDelay = Integer.parseInt(delay);
        }
        String hosts = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.host");
        String password = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.password");
        String conTimeout = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.conTimeout");
        if (null != conTimeout && !conTimeout.equals("")) {
            conTimeoutR = Integer.parseInt(conTimeout);
        }
        String soTimeout = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.soTimeout");
        if (null != soTimeout && !soTimeout.equals("")) {
            soTimeoutR = Integer.parseInt(soTimeout);
        }
        String maxAttempts = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.maxAttempts");
        if (null != maxAttempts && !maxAttempts.equals("")) {
            maxAttemptsR = Integer.parseInt(maxAttempts);
        }
        String[] serverArray = hosts.split(",");
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();

        for (String ipPort : serverArray) {
            String[] ipPortPair = ipPort.split(":");
            nodes.add(new HostAndPort(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim())));
        }

        //注意：这里超时时间不要太短，他会有超时重试机制。而且其他像httpclient、dubbo等RPC框架也要注意这点
        if(StringUtils.isBlank(password)){
            jedisCluster = new JedisCluster
                    (nodes, conTimeoutR, soTimeoutR, maxAttemptsR, new GenericObjectPoolConfig());
        }else{
            jedisCluster = new JedisCluster
                    (nodes, conTimeoutR, soTimeoutR, maxAttemptsR,
                            password, new GenericObjectPoolConfig());
        }
    }

    public void set(String key, String value, int time) {
//        log.info("the key=" + key + ",the value=" + value + ":" + com.gy.woodpecker.log.Logstatic.test);
//        log.error("the key=" + key + ",the value=" + value);

        jedisCluster.set(key, value);
        jedisCluster.expire(key, time);
    }

    public void rightPushBytes(String key, byte[] value) {
        log.info("the key123=" + key);
        jedisCluster.rpush(key.getBytes(), value);
    }


    public void rightPush(String key, String value) {
//        Logstatic.test = "1111111111gggggg";
//        log.info("the key=" + key + ",the value=" + value);
        jedisCluster.rpush(key, value);
    }

    public void healthCheck(final String appName) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long delay = healthCheckDelay;
        long initDelay = 0;
        executor.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        if (!telHealthCheck) {
                            return;
                        }
                        log.info("执行redis健康检查!");
                        try {
                            //if(null != redisClient){
                            set(appName + "-ping", "1", 1);
                            // }
                            LoggerFacility.f = true;
                        } catch (Exception e) {
                            log.info("redis健康检查异常,{}", e);
                            LoggerFacility.f = false;
                        }
                    }
                },
                initDelay,
                delay,
                TimeUnit.MILLISECONDS);
    }

    public static void main(String args[]) {
        new RedisClient().init();
    }
}
