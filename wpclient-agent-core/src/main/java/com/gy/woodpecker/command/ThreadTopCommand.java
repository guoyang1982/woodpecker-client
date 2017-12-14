package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author guoyang
 * @Description: 查看线程占用cpu资源命令
 * @date 2017/12/14 下午4:02
 */
@Cmd(name = "top", sort = 13, summary = "Display The Threads Of Top CPU TIME",
        eg = {
                "top",
                "top -t 5",
                "top -d"
        })
public class ThreadTopCommand extends AbstractCommand{
        @NamedArg(name = "i", hasValue = true, summary = "The thread id info")
        private String tid;

        @NamedArg(name = "t", hasValue = true, summary = "The top NUM of thread cost CPU times")
        private Integer top;

        @NamedArg(name = "d", summary = "Display the thread stack detail")
        private boolean isDetail = false;

        private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        private String stackToString(StackTraceElement[] stackTraceElementArray) {
                final TKv tKv = new TKv(
                        new TTable.ColumnDefine(),
                        new TTable.ColumnDefine(80)
                );
                if (ArrayUtils.isNotEmpty(stackTraceElementArray)) {
                        for (StackTraceElement ste : stackTraceElementArray) {
                                tKv.add("at", ste.toString());
                        }
                }
                return tKv.rendering();
        }
        @Override
        public void excute(Instrumentation inst) {
                final ArrayList<ThreadInfoData> threadInfoDatas = new ArrayList<ThreadInfoData>();

                long totalCpuTime = threadMXBean.getCurrentThreadCpuTime();
                for (ThreadInfo tInfo : threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE)) {
                        final long tId = tInfo.getThreadId();
                        final String tName = tInfo.getThreadName();
                        final long cpuTime = threadMXBean.getThreadCpuTime(tId);
                        final String tStateStr = tInfo.getThreadState().toString();
                        final String tStackStr = isDetail
                                ? stackToString(tInfo.getStackTrace())
                                : StringUtils.EMPTY;
                        totalCpuTime += cpuTime;
                        threadInfoDatas.add(new ThreadInfoData(tId, cpuTime, tName, tStateStr, tStackStr));
                }


                final int topFix = top == null ? threadInfoDatas.size() : Math.min(top, threadInfoDatas.size());
                Collections.sort(threadInfoDatas);

                final TTable tTable = new TTable(
                        isDetail
                                ?
                                new TTable.ColumnDefine[]{
                                        new TTable.ColumnDefine(TTable.Align.LEFT),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(),
                                        new TTable.ColumnDefine(20),
                                        new TTable.ColumnDefine(),
                                }
                                :
                                new TTable.ColumnDefine[]{
                                        new TTable.ColumnDefine(TTable.Align.LEFT),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(),
                                        new TTable.ColumnDefine(50)
                                }
                )
                        .addRow("ID", "CPU%", "USR%", "STATE", "THREAD_NAME", "THREAD_STACK")
                        .padding(1);


                final DecimalFormat df = new DecimalFormat("00.00");
                for (int index = 0; index < topFix; index++) {
                        final ThreadInfoData data = threadInfoDatas.get(index);
                        if (StringUtils.isNotBlank(tid)) {
                                final String fixTid = StringUtils.replace(tid, "#", "");
                                if (!StringUtils.equals("" + data.tId, fixTid)) {
                                        continue;
                                }
                        }
                        final String cpuTimeRateStr = (totalCpuTime > 0 ? df.format(data.cpuTime * 100d / totalCpuTime) : "00.00") + "%";
                        tTable.addRow("#" + data.tId, cpuTimeRateStr, data.tStateStr, data.tName, data.stackStr);
                }

                ctxT.writeAndFlush(tTable.rendering());

                //printer.println(tTable.rendering()).finish();
        }

        private class ThreadInfoData implements Comparable<ThreadInfoData> {

                private final long tId;
                private final long cpuTime;
                private final String tName;
                private final String tStateStr;
                private final String stackStr;

                private ThreadInfoData(long tId, long cpuTime, String tName, String tStateStr, String stackStr) {
                        this.tId = tId;
                        this.cpuTime = cpuTime;
                        this.tName = tName;
                        this.tStateStr = tStateStr;
                        this.stackStr = stackStr;
                }

                @Override
                public int compareTo(ThreadInfoData o) {
                        return Long.valueOf(o.cpuTime).compareTo(cpuTime);
                }
        }
}
