package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.log.LoggerFacility;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 设置健康检查开关
 * @date 2017/12/7 下午3:05
 */
@Slf4j
@Cmd(name = "hc", sort = 1, summary = "Setting up the redis health check",
        eg = {
                "hc true",
                "hc false"
        })
public class HealthCheckCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "isHC", isRequired = true, summary = "设置健康检查开关")
    private String isHC;

    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public void excute(Instrumentation inst) {
        if (isHC.equals("true")) {
            LoggerFacility.getInstall(null).telHealthCheck = true;
            ctxT.writeAndFlush("成功打开健康检查!\n");
            return;
        }
        if (isHC.equals("false")) {
            LoggerFacility.getInstall(null).telHealthCheck = false;
            ctxT.writeAndFlush("成功关闭健康检查!\n");
            return;
        }
    }
}
