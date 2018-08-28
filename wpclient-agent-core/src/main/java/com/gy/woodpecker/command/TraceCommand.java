package com.gy.woodpecker.command;

import com.gy.woodpecker.Advice;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.enumeration.CommandEnum;
import com.gy.woodpecker.session.SessionManager;
import com.gy.woodpecker.textui.TTree;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import com.gy.woodpecker.tools.InvokeCost;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.Serializable;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
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
                "trace org.apache.commons.lang.StringUtils isBlank",
                "trace -o trace.log org.apache.commons.lang.StringUtils isBlank",
                "trace org.apache.commons.lang.StringUtils isBlank cost>5",
                "trace -n 2 org.apache.commons.lang.StringUtils isBlank",
                "trace org.apache.commons.lang.StringUtils isBlank params[0]=='gg'"
        })
public class TraceCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "condition-express", isRequired = false,
            summary = "Conditional expression")
    private String conditionExpress;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;
    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";
    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;

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
    ScriptEngineManager manager = new ScriptEngineManager();

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
        if (!has) {
            setRes(has);
        }
        if (has && StringUtils.isNotBlank(output)) {
            fileWriter = new DailyRollingFileWriter(outPath + output);
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
        printResult(args);
    }

    private boolean isOverThreshold(int currentTimes) {
        return null != threshold
                && currentTimes > threshold;
    }

    private void printResult(Object[] args) {
        final Trace trace = traceRef.get();
        if (null == trace) {
            return;
        }
        if (!trace.tTree.isTop()) {
            trace.tTree.end();
        }
        //合并输出最终结果
        final Long cost = invokeCost.cost();

        //调用次数判断
        if (isOverThreshold(timesRef.incrementAndGet())) {
            //恢复类
            rollbackClass();
            //超过设置的调用次数 结束
            timesRef.set(0);
            return;
        }

        //时间判断
        if (StringUtils.isNotBlank(conditionExpress)) {
            //符合表达式时间的才输出
//            ScriptEngine engine = manager.getEngineByName("js");
//            engine.put("cost", cost);
            Object obj = null;
            if (conditionExpress.startsWith("cost")) {
                Map<String, Long> map = new HashMap();
                map.put("cost", cost);
                obj = map;
            }
            if (conditionExpress.startsWith("params")) {
                Advice advice = new Advice(null, null, null, args, null, null);
                obj = advice;
            }

            if (null != obj) {
                try {
                    //Boolean res = (Boolean) engine.eval(conditionExpress);
                    Boolean res = (Boolean) MVEL.eval(conditionExpress, obj);
                    if (res.booleanValue()) {
                        print(cost);
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    //恢复类
                    rollbackClass();
                    ctxT.writeAndFlush("condition-express is fail!\n");
                    return;
                }
            }else {
                //恢复类
                rollbackClass();
                ctxT.writeAndFlush("condition-express is fail!\n");
                return;
            }

        } else {
            print(cost);
        }

    }

//    public static void main(String args[]) {
//        //ScriptEngineManager manager = new ScriptEngineManager();
//        Map map = new HashMap();
//        map.put("a", 123);
//        map.put("b", "ddd");
//        Object[] args1 = {13123, "ddddd", map};
//        Advice advice = new Advice(null, null, null, args1, null, null);
//
//        //解释模式
//        long c = System.currentTimeMillis();
//
//        // Map context = new HashMap();
//        String expression = "params[1]==111";
////        VariableResolverFactory functionFactory = new MapVariableResolverFactory(context);
////        context.put("advice", advice);
////        Map map1 = new HashMap();
////        map1.put("cost", 2);
//        Boolean result = (Boolean) MVEL.eval(expression, advice);
//        System.out.println(result);
//
//        System.out.println(System.currentTimeMillis() - c);
//    }


    private void print(Long cost) {
        final Trace traceR = traceRef.get();
        String result = traceR.tTree.rendering();
        if (cost != null) {
            result = result.replaceFirst("costTime", String.valueOf(cost));

        }
        if (StringUtils.isNotBlank(output)) {
            fileWriter.append(result);
            fileWriter.flushAppend();
        } else {
            ctxT.writeAndFlush(result);
        }
    }


    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args, Throwable returnObject) {

        printResult(args);
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
    public void invokeThrowTracing(int lineNumber, String owner, String name, String desc, Object throwException) {
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

    @Override
    public void destroy() {
        if (null != fileWriter) {
            fileWriter.closeFile();
        }
    }
}
