package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 * @author guoyang
 * @Description: 帮助命令
 * @date 2017/12/7 下午3:29
 */
@Slf4j
@Cmd(name = "help", sort = 7, summary = "帮助命令",
        eg = {
                "help"
        })
public class HelpCommand extends AbstractCommand{
    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public void excute(Instrumentation inst) {
        StringBuffer str = new StringBuffer();
        Map<String, Class<?>> commandMap = Commands.getInstance().listCommands();
        final List<Class<?>> classes = new ArrayList<Class<?>>(commandMap.values());
        Collections.sort(classes, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final Integer o1s = o1.getAnnotation(Cmd.class).sort();
                final Integer o2s = o2.getAnnotation(Cmd.class).sort();
                return o1s.compareTo(o2s);
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                if (!cmd.isHacking()) {
                    str.append(cmd.name()).append(":").append(cmd.summary()).append("\r\n");
                    for(String s:cmd.eg()){
                        str.append("  ").append(s).append("\r\n");
                    }
                }
            }
        }
        ctxT.writeAndFlush("==========================================\r\n");
        ctxT.writeAndFlush(str.toString());
        ctxT.writeAndFlush("==========================================");
    }
}
