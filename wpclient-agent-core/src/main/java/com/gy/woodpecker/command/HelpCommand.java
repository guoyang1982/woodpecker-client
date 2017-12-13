package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
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
@Cmd(name = "help", sort = 7, summary = "Help command",
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

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(),
                new TTable.ColumnDefine(),
                new TTable.ColumnDefine()
        });

        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                if (!cmd.isHacking()) {
                    StringBuffer str = new StringBuffer();
                    for(String s:cmd.eg()){
                        str.append(s).append("\r\n");
                    }
                    tTable.addRow(cmd.name(), cmd.summary(),str.toString());
                }
            }
        }
        ctxT.writeAndFlush(tTable.rendering());
    }
}
