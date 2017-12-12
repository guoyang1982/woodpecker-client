package com.gy.woodpecker.netty;

import com.gy.woodpecker.handler.CommandHandler;
import com.gy.woodpecker.handler.DefaultCommandHandler;
import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.session.SessionManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/11/16 上午10:54
 */
@Slf4j
public class NettyTelnetHandler extends SimpleChannelInboundHandler<String> {
    private final CommandHandler commandHandler = new DefaultCommandHandler();
    // 会话ID序列生成器
    //private static final AtomicInteger sessionIndexSequence = new AtomicInteger(0);
    //private AttributeKey<Integer> attributeKey = AttributeKey.valueOf("sessionId");
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        // Send greeting for a new connection.
//
//        Attribute<Integer> attribute = ctx.attr(attributeKey);
//
//        if(attribute.get() == null) {
//            attribute.set(sessionIndexSequence.getAndIncrement());
//        }
//
//        ctx.write("欢迎来到啄木鸟控制端!\r\n");
//        ctx.write("请输入控制命令.\r\n");
//        ctx.flush();
//    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        Attribute<Integer> attribute = ctx.attr(attributeKey);
//        int sessionId = attribute.get();
//        log.info("连接关闭啦啦啦啦啦啦啦啦啦啦啦啦!"+sessionId);
//        cause.printStackTrace();
//        ctx.close();
//    }

    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {

        String response;
        boolean close = false;
        if (request.isEmpty()) {
            response = "请输入命令.\r\n";
            ctx.writeAndFlush(response);
        }else {
            int sessionId = SessionManager.getSessionId(ctx);
            commandHandler.executeCommand(request,ctx,LoggerFacility.inst,sessionId);
        }
    }
//
//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (evt instanceof IdleStateEvent) {
//            IdleStateEvent evnet = (IdleStateEvent) evt;
//            if (evnet.state().equals(IdleState.ALL_IDLE)) {
//                Attribute<Integer> attribute = ctx.attr(attributeKey);
//                int sessionId = attribute.get();
//                log.info("idle sessionId="+sessionId);
//            }
//        }
//        ctx.fireUserEventTriggered(evt);
//    }
}
