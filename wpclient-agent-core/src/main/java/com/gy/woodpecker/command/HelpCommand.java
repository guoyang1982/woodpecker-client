package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author guoyang
 * @Description: 帮助命令
 * @date 2017/12/7 下午3:29
 */
@Slf4j
@Cmd(name = "help", sort = 7, summary = "Help command",
        eg = {
                "help",
                "help print"
        })
public class HelpCommand extends AbstractCommand{
    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @IndexArg(index = 0, isRequired = false, name = "command-name", summary = "Command name")
    private String cmd;

    @Override
    public boolean excute(Instrumentation inst) {

        if (isBlank(cmd)
                || !Commands.getInstance().listCommands().containsKey(cmd)) {
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
        } else {
            final Class<?> clazz = Commands.getInstance().listCommands().get(cmd);
            ctxT.writeAndFlush(commandHelp(clazz));
        }


        return true;
    }

    private String commandHelp(Class<?> clazz) {

        final Cmd cmd = clazz.getAnnotation(Cmd.class);
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        })
                .addRow("USAGE", drawUsage(clazz, cmd));

        boolean hasOptions = false;
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)
                    || f.isAnnotationPresent(NamedArg.class)) {
                hasOptions = true;
                break;
            }
        }

        if (hasOptions) {
            tTable.addRow("OPTIONS", drawOptions(clazz));
        }

        if (null != cmd.eg()) {
            tTable.addRow("EXAMPLE", drawEg(cmd));
        }

        return tTable.padding(1).rendering();
    }

    private String drawUsage(final Class<?> clazz, final Cmd cmd) {

        final StringBuilder usageSB = new StringBuilder();
        final StringBuilder sbOp = new StringBuilder();
        final StringBuilder sbLongOp = new StringBuilder();
        for (Field f : clazz.getDeclaredFields()) {

            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                if (namedArg.name().length() == 1) {
                    sbOp.append(namedArg.name());
                    if (namedArg.hasValue()) {
                        sbOp.append(":");
                    }
                } else {
                    sbLongOp.append(namedArg.name());
                    if (namedArg.hasValue()) {
                        sbLongOp.append(":");
                    }
                }

            }

        }
        if (sbOp.length() > 0) {
            usageSB.append("-[").append(sbOp).append("]").append(" ");
        }

        if (sbLongOp.length() > 0) {
            usageSB.append("--[").append(sbLongOp).append("]").append(" ");
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                usageSB.append(indexArg.name()).append(" ");
            }
        }

        if (usageSB.length() > 0) {
            usageSB.append("\n");
        }
        usageSB.append(cmd.summary());

        return usageSB.toString();
    }

    private String drawEg(Cmd cmd) {
        final StringBuilder egSB = new StringBuilder();
        for (String eg : cmd.eg()) {
            egSB.append(eg).append("\n");
        }
        return egSB.toString();
    }
    private String drawOptions(Class<?> clazz) {
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(15, false, TTable.Align.RIGHT),
                new TTable.ColumnDefine(60, false, TTable.Align.LEFT)
        });

        tTable.getBorder().remove(TTable.Border.BORDER_OUTER);
        tTable.padding(1);

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                final String named = "[" + namedArg.name() + (namedArg.hasValue() ? ":" : "") + "]";

                String description = namedArg.summary();
                if (isNotBlank(namedArg.description())) {
                    description += "\n\n" + namedArg.description();
                }
                tTable.addRow(named, description);
            }
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                String description = indexArg.summary();
                if (isNotBlank(indexArg.description())) {
                    description += "\n\n" + indexArg.description();
                }
                tTable.addRow(indexArg.name(), description);
            }
        }

        return tTable.rendering();
    }
}
