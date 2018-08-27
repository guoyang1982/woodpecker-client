package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import com.gy.woodpecker.tools.InvokeCost;
import com.gy.woodpecker.tools.SimpleDateFormatHolder;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.gy.woodpecker.tools.GaCheckUtils.isEquals;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/3/8 下午6:49
 */
@Slf4j
@Cmd(name = "monitor", sort = 18, summary = "Monitor the execution of specified Class and its method",
        eg = {
                "monitor -c 5 org.apache.commons.lang.StringUtils isBlank",
        })
public class MonitorCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "c", hasValue = true, summary = "The cycle of monitor")
    private int cycle = 120;

    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";
    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;
    /*
     * 输出定时任务
    */
    private Timer timer;

    /*
     * 监控数据
     */
    private final ConcurrentHashMap<Key, AtomicReference<Data>> monitorData
            = new ConcurrentHashMap<Key, AtomicReference<Data>>();

    private final InvokeCost invokeCost = new InvokeCost();

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for (Class clazz : classes) {
            if (clazz.getName().equals(classPattern)) {
                has = true;
                SpyTransformer transformer = new SpyTransformer(methodPattern, true, true, this);
                inst.addTransformer(transformer, true);
                try {
                    inst.retransformClasses(clazz);
//                    //接收结果打印
//                    create();
                } catch (Exception e) {
                    log.error("执行异常{}", e);
                } finally {
                    inst.removeTransformer(transformer);
                }
            }
        }

        if (has) {
            if (StringUtils.isNotBlank(output)) {
                fileWriter = new DailyRollingFileWriter(outPath + output);
            }
            //接收结果打印
            create();
        }

        //等待结果
        super.isWait = true;
        return has;
    }

    public void create() {
        timer = new Timer("Timer-for-greys-monitor-" + sessionId, true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final TTable tTable = new TTable(10)
                        .addRow(
                                "TIMESTAMP",
                                "CLASS",
                                "METHOD",
                                "TOTAL",
                                "SUCCESS",
                                "FAIL",
                                "FAIL-RATE",
                                "AVG-RT(ms)",
                                "MIN-RT(ms)",
                                "MAX-RT(ms)"
                        );

                for (Map.Entry<Key, AtomicReference<Data>> entry : monitorData.entrySet()) {
                    final AtomicReference<Data> value = entry.getValue();

                    Data data;
                    while (true) {
                        //一直循环 如果data没有被覆盖的话 值设置为new Data，并返回true，并输出结果
                        //反之一直循环 直到不覆盖  是原子操作
                        data = value.get();
                        if (value.compareAndSet(data, new Data())) {
                            break;
                        }
                    }

                    if (null != data) {
                        final DecimalFormat df = new DecimalFormat("00.00");
                        tTable.addRow(
                                SimpleDateFormatHolder.getInstance().format(new Date()),
                                entry.getKey().className,
                                entry.getKey().methodName,
                                data.total,
                                data.success,
                                data.failed,
                                df.format(100.0d * div(data.failed, data.total)) + "%",
                                df.format(div(data.cost, data.total)),
                                data.minCost,
                                data.maxCost
                        );

                    }
                }

                tTable.padding(1);
                if (StringUtils.isNotBlank(output)) {
                    fileWriter.append(tTable.rendering());
                    fileWriter.flushAppend();
                } else {
                    ctxT.writeAndFlush(tTable.rendering());
                }
            }

        }, 0, cycle * 1000);
    }


    private double div(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }

    /**
     * 数据监控用的Key
     */
    private static class Key {
        private final String className;
        private final String methodName;

        private Key(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public int hashCode() {
            return className.hashCode() + methodName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj
                    || !(obj instanceof Key)) {
                return false;
            }
            Key oKey = (Key) obj;
            return isEquals(oKey.className, className)
                    && isEquals(oKey.methodName, methodName);
        }

    }

    /**
     * 数据监控用的value
     *
     * @author oldmanpushcart@gmail.com
     */
    private static class Data {
        private int total;
        private int success;
        private int failed;
        private long cost;
        private Long maxCost;
        private Long minCost;
    }

    @Override
    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        invokeCost.begin();
    }

    @Override
    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {
        finishing(className, methodName, true);
    }

    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args, Throwable returnObject) {

        try {
            finishing(className, methodName, false);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void finishing(String className, String methodName, boolean isSuccess) throws Throwable {
        final Key key = new Key(className, methodName);
        final long cost = invokeCost.cost();

        while (true) {
            final AtomicReference<Data> value = monitorData.get(key);
            if (null == value) {
                monitorData.putIfAbsent(key, new AtomicReference<Data>(new Data()));
                // 这里不去判断返回值，用continue去强制获取一次
                continue;
            }

            while (true) {
                Data oData = value.get();
                Data nData = new Data();
                nData.cost = oData.cost + cost;
                if (!isSuccess) {
                    nData.failed = oData.failed + 1;
                    nData.success = oData.success;
                } else {
                    nData.failed = oData.failed;
                    nData.success = oData.success + 1;
                }
                nData.total = oData.total + 1;

                // setValue max-cost
                if (null == oData.maxCost) {
                    nData.maxCost = cost;
                } else {
                    nData.maxCost = Math.max(oData.maxCost, cost);
                }

                // setValue min-cost
                if (null == oData.minCost) {
                    nData.minCost = cost;
                } else {
                    nData.minCost = Math.min(oData.minCost, cost);
                }

                //原子操作和上面的一对  oData没有被修改的话 则值设置nData 判断变化都是oData引用
                if (value.compareAndSet(oData, nData)) {
                    break;
                }
            }
            break;
        }
    }

    @Override
    public void destroy() {
        if (null != timer) {
            timer.cancel();
        }
        if(null != fileWriter){
            fileWriter.closeFile();
        }
    }
}
