package com.gy.woodpecker.netty;

import com.gy.woodpecker.handler.CommandHandler;
import com.gy.woodpecker.handler.DefaultCommandHandler;
import com.gy.woodpecker.log.LoggerFacility;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/11/16 上午10:54
 */
public class NettyTelnetHandler extends SimpleChannelInboundHandler<String> {
    private final CommandHandler commandHandler = new DefaultCommandHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection.
        ctx.write("欢迎来到啄木鸟控制端!\r\n");
        ctx.write("请输入控制命令.\r\n");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {

        String response;
        boolean close = false;
        if (request.isEmpty()) {
            response = "请输入命令.\r\n";
        }else {
            commandHandler.executeCommand(request,ctx,LoggerFacility.inst);
        }
    }
}
