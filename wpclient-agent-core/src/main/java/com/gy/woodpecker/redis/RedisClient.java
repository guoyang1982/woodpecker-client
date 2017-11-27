package com.gy.woodpecker.redis;

import com.gy.woodpecker.log.Logstatic;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by guoyang on 17/10/27.
 */
@Slf4j
public class RedisClient {
    JedisCluster jedisCluster = null;
    //连接超时
    int conTimeoutR = 1000;
    //读超时
    int soTimeoutR = 1000;
    //最大重试次数
    int maxAttemptsR = 1;

    public void init(){
        log.info("init redis!!!!!!");
        String hosts = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.host");
        String password = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.password");
        String conTimeout = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.conTimeout");
        if(null != conTimeout && !conTimeout.equals("")){
            conTimeoutR = Integer.parseInt(conTimeout);
        }
        String soTimeout = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.soTimeout");
        if(null != soTimeout && !soTimeout.equals("") ){
            soTimeoutR = Integer.parseInt(soTimeout);
        }
        String maxAttempts = ConfigPropertyUtile.getProperties().getProperty("redis.cluster.maxAttempts");
        if(null != maxAttempts && !maxAttempts.equals("") ){
            maxAttemptsR = Integer.parseInt(maxAttempts);
        }
        String[] serverArray = hosts.split(",");
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();

        for (String ipPort : serverArray) {
            String[] ipPortPair = ipPort.split(":");
            nodes.add(new HostAndPort(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim())));
        }

        //注意：这里超时时间不要太短，他会有超时重试机制。而且其他像httpclient、dubbo等RPC框架也要注意这点
        jedisCluster = new JedisCluster
                (nodes, conTimeoutR, soTimeoutR, maxAttemptsR,
                        password, new GenericObjectPoolConfig());

    }

    public void set(String key,String value,int time){
        log.info("the key="+key+",the value="+value+":"+ com.gy.woodpecker.log.Logstatic.test);
        log.error("the key="+key+",the value="+value);

        jedisCluster.set(key,value);
        jedisCluster.expire(key,time);
    }

    public void rightPush(String key,String value){Logstatic.test = "1111111111gggggg";
        log.info("the key="+key+",the value="+value);
        jedisCluster.rpush(key,value);
    }


    public static void main(String args[]){
        new RedisClient().init();
    }
}
