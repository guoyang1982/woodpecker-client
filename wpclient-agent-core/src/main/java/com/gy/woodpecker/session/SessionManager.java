package com.gy.woodpecker.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/12 下午4:37
 */
public class SessionManager {
    private static AttributeKey<Integer> attributeKey = AttributeKey.valueOf("sessionId");
    // 会话ID序列生成器
    private static final AtomicInteger sessionIndexSequence = new AtomicInteger(0);

    /**
     * 创建一个会话
     * @param ctx
     */
    public static void newSession(ChannelHandlerContext ctx){
        //Attribute<Integer> attribute = ctx.attr(attributeKey);
        //不能用ChannelHandlerContext的Attribute，多个handler不能共享，所以用channel的，这样都能共享
        Attribute<Integer> attribute = ctx.channel().attr(attributeKey);

        if(attribute.get() == null) {
            attribute.set(sessionIndexSequence.getAndIncrement());
        }
    }

    /**
     * 获取一个会话id
     * @param ctx
     * @return
     */
    public static int getSessionId(ChannelHandlerContext ctx){
        Attribute<Integer> attribute = ctx.channel().attr(attributeKey);
        return attribute.get();
    }

}
