package com.gy.woodpecker.netty;

import com.gy.woodpecker.command.ResetCommand;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.session.SessionManager;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import com.gy.woodpecker.tools.GaStringUtils;
import com.gy.woodpecker.tools.IPUtile;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/12 下午4:24
 */
@Slf4j
public class NettyConnetManageHandler extends ChannelDuplexHandler {
    private static final byte[] EOT = "".getBytes();

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
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = address.getAddress().getHostAddress();
        String whiteIPs = ConfigPropertyUtile.getProperties().getProperty("netty.whiteIPs");
        if(StringUtils.isNotBlank(whiteIPs)){
            boolean f = false;
            //ip黑白名单验证
            String ips[] = whiteIPs.split(",");
            for (String wip:ips){
                if(IPUtile.isInRange(ip,wip)){
                    f = true;
                    break;
                }
            }
            if(!f){
                log.error("不在白名单内不允许建链!");
                ctx.close();
                return;
            }
        }

        //创建会话
        SessionManager.newSession(ctx);
        //ctx.write("欢迎来到啄木鸟控制端!\n");
        ctx.write(GaStringUtils.getLogo() + "\n");
        ctx.flush();

        ctx.write("请输入控制命令.\n\0");
        ctx.flush();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端关闭,准备清理增强的代码!");
        ResetCommand resetCommand = new ResetCommand();
        resetCommand.setCtxT(ctx);
        resetCommand.setSessionId(SessionManager.getSessionId(ctx));
        resetCommand.excute(ContextConfig.inst);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent evnet = (IdleStateEvent) evt;
            //客户端没有命令输入 ping下看是否关闭了
            if (evnet.state().equals(IdleState.READER_IDLE)) {
                ctx.channel().writeAndFlush("\1").addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (f.isSuccess()) {
                            log.info("ping success!");
                            return;
                        } else {
                            int sessionId = SessionManager.getSessionId(ctx);
                            log.info("客户端关闭,准备清理增强的代码!");
                            ResetCommand resetCommand = new ResetCommand();
                            resetCommand.setCtxT(ctx);
                            resetCommand.setSessionId(SessionManager.getSessionId(ctx));
                            resetCommand.excute(ContextConfig.inst);
                            log.info("idle sessionId=" + sessionId + ",close netty!");
                            ctx.close();
                        }
                    }
                });
            }
        }
        ctx.fireUserEventTriggered(evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //发生异常
        int sessionId = SessionManager.getSessionId(ctx);
        log.info("连接异常!" + sessionId);
    }

}
