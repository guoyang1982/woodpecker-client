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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.gy.woodpecker.tools.GaCheckUtils.isEquals;
import static java.io.File.separatorChar;
import static java.lang.System.getProperty;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/8/23 下午5:19
 */
@Slf4j
@Cmd(name = "scan", sort = 19, summary = "scan the execution of specified Class and its all method",
        eg = {
                "scan -c 5 org.apache.commons.lang.StringUtils",
        })
public class TopMethodTimeCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @NamedArg(name = "c", hasValue = true, summary = "The cycle of monitor")
    private int cycle = 120;

    @NamedArg(name = "t", hasValue = true, summary = "The counts of monitor")
    private int top = 10;

    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";

    /*
     * 输出定时任务
    */
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    /*
     * 监控数据
     */
    private final ConcurrentHashMap<Key, AtomicReference<Data>> monitorData
            = new ConcurrentHashMap<Key, AtomicReference<Data>>();

    private final InvokeCost invokeCost = new InvokeCost();

    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;

    public TopMethodTimeCommand() {
    }

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for (Class clazz : classes) {

            if (clazz.getName().startsWith(classPattern) && !clazz.getName().startsWith("com.gy.woodpecker")) {
                //只要正常的普通类
                if (clazz.isEnum() || clazz.isAnnotation() || clazz.isInterface() || clazz.isLocalClass() || clazz.isMemberClass() || clazz.isPrimitive()) {
                    continue;
                }
                has = true;
                SpyTransformer transformer = new SpyTransformer(null, true, true, this);
                inst.addTransformer(transformer, true);
                try {
                    inst.retransformClasses(clazz);
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
        long delay = cycle;
        long initDelay = 0;
        executor.scheduleAtFixedRate(
                new Runnable() {
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

                        List<Data> lists = new ArrayList<Data>();
                        //List<Data> lists = Lists.newArrayList();
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
                                data.className = entry.getKey().className;
                                data.methodName = entry.getKey().methodName;
                                lists.add(data);
                            }
                        }

                        //@排序
//                        Comparator<Data> dataComparator = Ordering
//                                .from(new Comparator<Data>() {
//                                    @Override
//                                    public int compare(Data o1, Data o2) {
//                                        return div(o1.cost, o1.total) > div(o2.cost, o2.total) ? -1
//                                                : (div(o1.cost, o1.total) == div(o2.cost, o2.total) ? 0 : 1);
//                                    }
//                                }).compound(new Comparator<Data>() {
//                                    @Override
//                                    public int compare(Data o1, Data o2) {
//                                        return o1.failed > o2.failed ? -1
//                                                : (o1.failed == o2.failed ? 0 : 1);
//                                    }
//                                });
//
//                        Collections.sort(lists, dataComparator);

                        Collections.sort(lists, new Comparator<Data>() {
                            @Override
                            public int compare(Data o1, Data o2) {
                                return div(o1.cost, o1.total) > div(o2.cost, o2.total) ? -1
                                        : (div(o1.cost, o1.total) == div(o2.cost, o2.total) ? 0 : 1);
                            }
                        });


                        for (int i = 0; i < lists.size(); i++) {
                            if (i >= top) {
                                break;
                            }
                            Data data = lists.get(i);
                            final DecimalFormat df = new DecimalFormat("00.00");
                            tTable.addRow(
                                    SimpleDateFormatHolder.getInstance().format(new Date()),
                                    data.className,
                                    data.methodName,
                                    data.total,
                                    data.success,
                                    data.failed,
                                    df.format(100.0d * div(data.failed, data.total)) + "%",
                                    df.format(div(data.cost, data.total)),
                                    data.minCost,
                                    data.maxCost
                            );
                        }

                        tTable.padding(1);
                        if (StringUtils.isNotBlank(output)) {
                            fileWriter.append(tTable.rendering());
                            fileWriter.flushAppend();
                        } else {
                            ctxT.writeAndFlush(tTable.rendering());
                        }
                    }
                },
                initDelay,
                delay,
                TimeUnit.SECONDS);
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
     **/
    private static class Data {
        private String className;
        private String methodName;
        private int total;
        private int success;
        private int failed;
        private long cost;
        private Long maxCost;
        private Long minCost;
    }

    private double div(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
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
        executor.shutdownNow();
        if(null != fileWriter){
            fileWriter.closeFile();
        }
    }
}
