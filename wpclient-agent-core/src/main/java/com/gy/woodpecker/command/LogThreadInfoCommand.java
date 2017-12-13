package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.textui.TTable;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: 发送消息线程信息
 * @date 2017/12/7 下午3:17
 */
@Slf4j
@Cmd(name = "thread", sort = 5, summary = "Sending message thread information",
        eg = {
                "thread"
        })
public class LogThreadInfoCommand extends AbstractCommand {
    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public void excute(Instrumentation inst) {
       String threadInfo = LoggerFacility.threadPoolsMonitor()+"\r\n";
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });
        tTable.addRow(threadInfo);
        ctxT.writeAndFlush(tTable.rendering());
    }
}
