package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.gy.woodpecker.Advice;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.textui.ext.TObject;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import com.gy.woodpecker.transformer.SpyTransformer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: 获取运行时类方法的参数和返回值及调用时间
 * @date 2017/12/7 下午6:26
 */
@Slf4j
@Cmd(name = "watch", sort = 8, summary = "The method parameters and return values of the display class",
        eg = {
                "watch -p org.apache.commons.lang.StringUtils isBlank",
                "watch -r org.apache.commons.lang.StringUtils isBlank",
                "watch -rj org.apache.commons.lang.StringUtils isBlank",
                "watch -rx 2 org.apache.commons.lang.StringUtils isBlank",
                "watch -rx 2 -o watch.log org.apache.commons.lang.StringUtils isBlank",
                "watch -rx 2 org.apache.commons.lang.StringUtils isBlank params[0]==123",
                "watch -pn 6 org.apache.commons.lang.StringUtils isBlank"
        })
public class WatchCommand extends AbstractCommand {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "condition-express", isRequired = false,
            summary = "Conditional expression")
    private String conditionExpress;

    @NamedArg(name = "p", summary = "Watch the parameter")
    private boolean isParam = false;

    @NamedArg(name = "r", summary = "Watch the return")
    private boolean isReturn = false;

    @NamedArg(name = "x", hasValue = true, summary = "Expand level of object (0 by default)")
    private Integer expend;

    @NamedArg(name = "j", summary = "use json")
    private boolean isUsingJson = false;

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
    public boolean getIfAllNotify() {
        return true;
    }

    private final AtomicInteger timesRef = new AtomicInteger();

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for (Class clazz : classes) {
            if (clazz.getName().equals(classPattern)) {
                has = true;
                SpyTransformer transformer = new SpyTransformer(methodPattern, isParam, isReturn, this);
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
        if (has && StringUtils.isNotBlank(output)) {
            fileWriter = new DailyRollingFileWriter(outPath + output);
        }
        //等待结果
        super.isWait = true;
        return has;
    }

    private boolean isOverThreshold(int currentTimes) {
        return null != threshold
                && currentTimes > threshold;
    }

    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {

        Advice advice = new Advice(null, className, methodName, args, null, null);

        //调用次数判断
        if (isOverThreshold(timesRef.incrementAndGet())) {
            //超过设置的调用次数 结束
            timesRef.set(0);
            rollbackClass();
            return;
        }
        print(advice);
    }

    @Override
    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {

        //调用次数判断
        if (isOverThreshold(timesRef.incrementAndGet())) {
            rollbackClass();
            //超过设置的调用次数 结束
            timesRef.set(0);
            return;
        }

        Advice advice = new Advice(null, className, methodName, args, returnObject, null);
        print(advice);

    }

    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args, Throwable returnObject) {

        Advice advice = new Advice(null, className, methodName, args, null, returnObject);
        print(advice);
    }

    private void print(Advice advice) {
        if (StringUtils.isNotBlank(conditionExpress)) {
            if(conditionExpress.startsWith("params")){
                try {
                    Boolean res = (Boolean) MVEL.eval(conditionExpress, advice);
                    if (res.booleanValue()) {
                        if (StringUtils.isNotBlank(output)) {
                            fileWriter.append(new TObject(advice, expend, isUsingJson).rendering() + "\n");
                            fileWriter.flushAppend();
                        } else {
                            ctxT.writeAndFlush(new TObject(advice, expend, isUsingJson).rendering() + "\n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    rollbackClass();
                    ctxT.writeAndFlush("condition-express is fail!\n");
                    return;
                }
            }else {
                rollbackClass();
                ctxT.writeAndFlush("condition-express is fail!\n");
                return;
            }
        } else {
            if (StringUtils.isNotBlank(output)) {
                fileWriter.append(new TObject(advice, expend, isUsingJson).rendering() + "\n");
                fileWriter.flushAppend();
            } else {
                ctxT.writeAndFlush(new TObject(advice, expend, isUsingJson).rendering() + "\n");
            }
        }
    }

    @Override
    public void destroy() {
        if (null != fileWriter) {
            fileWriter.closeFile();
        }
    }
}
