package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 退出客户端命令
 * @date 2017/12/7 下午3:21
 */
@Slf4j
@Cmd(name = "quit", sort = 6, summary = "Exit the client command",
        eg = {
                "quit"
        })
public class QuitCommand extends AbstractCommand {
    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public void excute(Instrumentation inst) {
        ChannelFuture future = ctxT.writeAndFlush("Bye!\r\n");
        future.addListener(ChannelFutureListener.CLOSE);

    }
}
