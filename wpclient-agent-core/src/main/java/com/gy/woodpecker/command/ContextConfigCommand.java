package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.config.ContextConfig;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/14 下午7:05
 */
@Slf4j
@Cmd(name = "open", sort = 13, summary = "Setting variable switch",
        eg = {
                "open dumpclazz true",
                "open dumpclazz false"
        })
public class ContextConfigCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "target", summary = "the target")
    private String target;

    @IndexArg(index = 1, name = "value", summary = "the value,ture or false")
    private String value;


    @Override
    public void excute(Instrumentation inst) {
        if (target.equals("dumpclazz")) {
            if (value.equals("true")) {
                ContextConfig.isdumpClass = true;
                ctxT.writeAndFlush("成功打开dump增强的类!\n");
                return;
            }
            if (value.equals("false")) {
                ContextConfig.isdumpClass = false;
                ctxT.writeAndFlush("成功关闭dump增强的类!\n");
                return;
            }
        }
    }
}
