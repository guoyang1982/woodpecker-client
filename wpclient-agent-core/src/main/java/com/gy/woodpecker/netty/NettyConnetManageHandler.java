package com.gy.woodpecker.netty;

import com.gy.woodpecker.command.ResetCommand;
import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.session.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/12 下午4:24
 */
@Slf4j
public class NettyConnetManageHandler  extends ChannelDuplexHandler {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.info("NETTY SERVER PIPELINE: channelRegistered");
        super.channelRegistered(ctx);
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.info("NETTY SERVER PIPELINE: channelUnregistered");
        super.channelUnregistered(ctx);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //创建会话
        SessionManager.newSession(ctx);
        ctx.write("欢迎来到啄木鸟控制端!\r\n");
        ctx.write("请输入控制命令.\r\n");
        ctx.flush();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端关闭,准备清理增强的代码!");
        ResetCommand resetCommand = new ResetCommand();
        resetCommand.excute(LoggerFacility.inst);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent evnet = (IdleStateEvent) evt;
            if (evnet.state().equals(IdleState.ALL_IDLE)) {
                int sessionId = SessionManager.getSessionId(ctx);
                log.info("idle sessionId="+sessionId);
            }
        }
        ctx.fireUserEventTriggered(evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //发生异常
        int sessionId = SessionManager.getSessionId(ctx);
        log.info("连接异常!"+sessionId);
    }

}
