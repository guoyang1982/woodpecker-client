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
}
