package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.log.LoggerFacility;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 发送消息线程信息
 * @date 2017/12/7 下午3:17
 */
@Slf4j
@Cmd(name = "thread", sort = 5, summary = "发送消息线程信息",
        eg = {
                "thread"
        })
public class LogThreadInfoCommand implements Command {
    @Override
    public void doAction(ChannelHandlerContext ctx, Instrumentation inst) {
       String threadInfo = LoggerFacility.threadPoolsMonitor()+"\r\n";
        ctx.writeAndFlush(threadInfo);
    }
}
