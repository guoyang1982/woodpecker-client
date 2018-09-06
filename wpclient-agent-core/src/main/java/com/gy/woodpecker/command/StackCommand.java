package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import com.gy.woodpecker.tools.InvokeCost;
import com.gy.woodpecker.transformer.SpyAsmTransformer;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gy.woodpecker.tools.GaStringUtils.getStack;
import static com.gy.woodpecker.tools.GaStringUtils.getThreadInfo;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/18 下午12:06
 */
@Slf4j
@Cmd(name = "stack", sort = 15, summary = "Display the stack trace of specified class and method",
        eg = {
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack -o stack.log org.apache.commons.lang.StringUtils isBlank"
        })
public class StackCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;

    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";
    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;
    private final AtomicInteger timesRef = new AtomicInteger();

    @Override
    public boolean getIfEnhance() {
        return true;
    }

    private final ThreadLocal<String> stackInfoRef = new ThreadLocal<String>();
    private final InvokeCost invokeCost = new InvokeCost();

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for (Class clazz : classes) {
            if (clazz.getName().equals(classPattern)) {
                has = true;
                SpyAsmTransformer transformer = new SpyAsmTransformer(this,methodPattern,true,true);
                //SpyTransformer transformer = new SpyTransformer(methodPattern, true, true, this);
                inst.addTransformer(transformer, true);
                try {
                    inst.retransformClasses(clazz);
                } catch (Exception e) {
                    log.error("执行stack命令异常{}", e);
                } finally {
                    inst.removeTransformer(transformer);
                }
            }
        }

        if (has && StringUtils.isNotBlank(output)) {
            fileWriter = new DailyRollingFileWriter(outPath + output);
        }
        //等待结果
        super.isWait = true;
        return has;
    }
    //判断是否超过次数
    private boolean isOverThreshold(int currentTimes) {
        return null != threshold
                && currentTimes > threshold;
    }
    private String getTitle() {
        final StringBuilder titleSB = new StringBuilder(getThreadInfo());
        return titleSB.toString();
    }

    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        //调用次数判断
        if (isOverThreshold(timesRef.incrementAndGet())) {
            rollbackClass();
            //超过设置的调用次数 结束
            timesRef.set(0);
            return;
        }
        stackInfoRef.set(getStack(getTitle()));
        invokeCost.begin();
    }

    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {

        if (StringUtils.isNotBlank(output)) {
            fileWriter.append(stackInfoRef.get());
            fileWriter.flushAppend();
        } else {
            ctxT.writeAndFlush(stackInfoRef.get());
        }
    }

    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args, Throwable returnObject) {
        if (StringUtils.isNotBlank(output)) {
            fileWriter.append(stackInfoRef.get());
            fileWriter.flushAppend();
        } else {
            ctxT.writeAndFlush(stackInfoRef.get());
        }
    }

    @Override
    public void destroy() {
        if (null != fileWriter) {
            fileWriter.closeFile();
        }
    }
}
