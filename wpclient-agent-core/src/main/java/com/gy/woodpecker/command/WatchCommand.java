package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.transformer.SpyTransformer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

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
        })
public class WatchCommand extends AbstractCommand{

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "p", summary = "Watch the parameter")
    private boolean isParam = false;

    @NamedArg(name = "r", summary = "Watch the return")
    private boolean isReturn = false;

    @Override
    public boolean getIfEnhance() {
        return true;
    }

    @Override
    public boolean getIfAllNotify() {
        return true;
    }

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for(Class clazz:classes) {
            if (clazz.getName().equals(classPattern)) {
                has = true;
                SpyTransformer transformer = new SpyTransformer(methodPattern,isParam,isReturn,this);
                inst.addTransformer(transformer, true);
                try {
                    inst.retransformClasses(clazz);
                } catch (Exception e){
                    log.error("执行异常{}",e);
                }finally {
                    inst.removeTransformer(transformer);
                }
            }
        }
        //等待结果
        super.isWait = true;
        return has;
    }

    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {

        final TKv tKv = new TKv(
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT));
        tKv.add("className",className);
        tKv.add("methodName",methodName);
        tKv.add("patameter",JSON.toJSONString(args));

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });

        tTable.addRow(tKv.rendering());
        ctxT.writeAndFlush(tTable.rendering());
    }

    @Override
    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {

        printReturn(className, methodName, args, returnObject);
    }

    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                                Object target, Object[] args,Throwable returnObject){

        final TKv tKv = new TKv(
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT));
        tKv.add("className",className);
        tKv.add("methodName",methodName);
        tKv.add("patameter", JSON.toJSONString(args));
        tKv.add("return",returnObject.toString());

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });

        tTable.addRow(tKv.rendering());
        ctxT.writeAndFlush(tKv.rendering());
    }

    private void printReturn(String className, String methodName, Object[] args, Object returnObject) {
        final TKv tKv = new TKv(
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT));
        tKv.add("className",className);
        tKv.add("methodName",methodName);
        tKv.add("patameter", JSON.toJSONString(args));
        tKv.add("return",JSON.toJSONString(returnObject));

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });

        tTable.addRow(tKv.rendering());
        ctxT.writeAndFlush(tKv.rendering());
    }

}
