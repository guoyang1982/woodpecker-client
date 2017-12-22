package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.tools.InvokeCost;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

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
                "stack org.apache.commons.lang.StringUtils isBlank"
        })
public class StackCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

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
                SpyTransformer transformer = new SpyTransformer(methodPattern, true, true, this);
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
        //等待结果
        super.isWait = true;
        return has;
    }

    private String getTitle() {
        final StringBuilder titleSB = new StringBuilder(getThreadInfo());
        return titleSB.toString();
    }

    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        stackInfoRef.set(getStack(getTitle()));
        invokeCost.begin();
    }

    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {

        ctxT.writeAndFlush(stackInfoRef.get());
    }

    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args,Throwable returnObject){

        ctxT.writeAndFlush(stackInfoRef.get());
    }

}
