package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.log.LoggerFacility;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 设置应用是否发送异常消息
 * @date 2017/12/7 下午1:51
 */
@Slf4j
@Cmd(name = "slog", sort = 4, summary = "设置应用是否发送异常消息",
        eg = {
                "slog true"
        })
public class LogSendMgeCommand implements Command {
    @IndexArg(index = 0, name = "isSend", isRequired = true, summary = "是否发送异常日志消息")
    private String isSend;

    @Override
    public void doAction(ChannelHandlerContext ctx, Instrumentation inst) {

        if (isSend.equals("true")) {
            LoggerFacility.f = true;
            ctx.writeAndFlush("成功打开日志监控!\r\n");
            return;
        }
        if (isSend.equals("false")) {
            LoggerFacility.getInstall(null).f = false;
            ctx.writeAndFlush("成功关闭日志监控!\r\n");
            return;
        }
        ctx.writeAndFlush("参数错误!\r\n");
    }
}
