package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static java.lang.management.MemoryType.HEAP;
import static java.lang.management.MemoryType.NON_HEAP;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/21 下午1:38
 */
@Slf4j
@Cmd(name = "jstat", sort = 17, summary = "Display memory information",
        eg = {
                "jstat",
                "jstat -a",
                "jstat -ao jstat.log",
                "jstat -t 10",
                "jstat -ta 10"
        })
public class MemoryCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "times", isRequired = false, summary = "timing interval(s)")
    private String times;

    @NamedArg(name = "t", summary = "is timer")
    private boolean isTimer = false;

    @NamedArg(name = "a", summary = "Display all")
    private boolean isAll = false;

    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";

    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public boolean excute(Instrumentation inst) {
        if (StringUtils.isNotBlank(output)) {
            fileWriter = new DailyRollingFileWriter(outPath + output);
        }
        if (isTimer) {
            if (StringUtils.isBlank(times)) {
                ctxT.writeAndFlush("not timer!\n");
                return false;
            }
            final int timer = Integer.valueOf(times);
            long delay = timer;
            long initDelay = 0;
            executor.scheduleAtFixedRate(
                    new Runnable() {
                        public void run() {
                            System.out.println("print memory!!!");
                            printMemory();
                        }
                    },
                    initDelay,
                    delay,
                    TimeUnit.SECONDS);

            //等待结果
            super.isWait = true;
            return true;
        } else {
            printMemory();
            return true;
        }
    }

    /**
     * PSScavenge（＝“ParallelScavenge的Scavenge”）就是负责minor GC的收集器；
     * 而负责full GC的收集器叫做PSMarkSweep（＝“ParallelScavenge的MarkSweep”）
     */
    private void printMemory() {
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });
        List<MemoryPoolMXBean> mps = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mp : mps) {
            final TKv tKv = new TKv(
                    new TTable.ColumnDefine(TTable.Align.RIGHT),
                    new TTable.ColumnDefine(TTable.Align.LEFT));
            if (isAll) {
                tKv.add("name", mp.getName());
                tKv.add("Usage", mp.getUsage());
                tKv.add("CollectionUsage", mp.getCollectionUsage());
                tKv.add("PeakUsage", mp.getPeakUsage());
                tKv.add("type", mp.getType());
                tTable.addRow(tKv.rendering());
            } else {
                if (mp.getType() == HEAP) {
                    tKv.add("name", mp.getName());
                    tKv.add("Usage", mp.getUsage());
                    tKv.add("CollectionUsage", mp.getCollectionUsage());
                    tKv.add("PeakUsage", mp.getPeakUsage());
                    tKv.add("type", mp.getType());
                    tTable.addRow(tKv.rendering());
                }
            }
        }
        //垃圾收集
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            final TKv tKv = new TKv(
                    new TTable.ColumnDefine(TTable.Align.RIGHT),
                    new TTable.ColumnDefine(TTable.Align.LEFT));
            tKv.add(garbageCollectorMXBean.getName() + " count", garbageCollectorMXBean.getCollectionCount());
            tKv.add(garbageCollectorMXBean.getName() + " time", garbageCollectorMXBean.getCollectionTime());
            tTable.addRow(tKv.rendering());
        }

        if (StringUtils.isNotBlank(output)) {
            fileWriter.append(tTable.rendering());
            fileWriter.flushAppend();
        } else {
            ctxT.writeAndFlush(tTable.rendering());
        }
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
        if(null != fileWriter){
            fileWriter.closeFile();
        }
    }
}
