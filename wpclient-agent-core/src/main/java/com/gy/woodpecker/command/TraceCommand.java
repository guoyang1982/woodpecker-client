package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.enumeration.CommandEnum;
import com.gy.woodpecker.textui.TTree;
import com.gy.woodpecker.tools.InvokeCost;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gy.woodpecker.tools.GaStringUtils.getThreadInfo;
import static com.gy.woodpecker.tools.GaStringUtils.tranClassName;

/**
 * @author guoyang
 * @Description: trace 跟踪代码 显示代码调用方法执行时间、异常信息等
 * @date 2017/12/15 下午3:39
 */
@Slf4j
@Cmd(name = "trace", sort = 14, summary = "Display the detailed thread stack of specified class and method",
        eg = {
                "trace org.apache.commons.lang.StringUtils isBlank"
        })
public class TraceCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @Override
    public boolean getIfEnhance() {
        return true;
    }

    @Override
    public CommandEnum getCommandType() {
        return CommandEnum.TRACE;
    }

    private final AtomicInteger timesRef = new AtomicInteger();
    private final InvokeCost invokeCost = new InvokeCost();
    private final ThreadLocal<Trace> traceRef = new ThreadLocal<Trace>();

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
                } catch (Exception e) {
                    log.error("执行trace命令异常{}", e);
                } finally {
                    inst.removeTransformer(transformer);
                }
            }
        }
        if(!has){
            setRes(has);
        }
        //等待结果
        super.isWait = true;
        return res;
    }

    @Override
    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        invokeCost.begin();
        traceRef.set(
                new Trace(
                        new TTree(true, getTitle())
                                .begin(className + ":" + methodName + "(costTime)")
                )
        );
    }

    private String getTitle() {
        final StringBuilder titleSB = new StringBuilder("Tracing for : ")
                .append(getThreadInfo());
        return titleSB.toString();
    }

    @Override
    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {
        final Trace trace = traceRef.get();
        if (null == trace) {
            return;
        }
        if (!trace.tTree.isTop()) {
            trace.tTree.end();
        }
        //合并输出最终结果
        final Long cost = invokeCost.cost();
        final Trace traceR = traceRef.get();
        String result = traceR.tTree.rendering();
        if (cost != null) {
            result = result.replaceFirst("costTime", String.valueOf(cost));

        }
        ctxT.writeAndFlush(result);
    }

    @Override
    public void invokeBeforeTracing(int lineNumber, String owner, String name, String desc) {
        final Trace trace = traceRef.get();
        if (null == trace) {
            return;
        }
        trace.tTree.begin(tranClassName(owner) + ":" + name + "(@" + lineNumber + ")");
    }

    @Override
    public void invokeThrowTracing(int lineNumber, String owner, String name, String desc, Object throwException){
        final Trace trace = traceRef.get();
        if (!trace.tTree.isTop()) {
            trace.tTree.set(trace.tTree.get() + "[throw " + throwException + "]").end();
        }
    }

    @Override
    public void invokeAfterTracing(int lineNumber, String owner, String name, String desc) {
        final Trace trace = traceRef.get();
        if (null == trace) {
            return;
        }
        if (!trace.tTree.isTop()) {
            trace.tTree.end();
        }
    }

    private class Trace {
        private final TTree tTree;

        private Trace(TTree tTree) {
            this.tTree = tTree;
        }
    }
}
