package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
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
@Cmd(name = "watch", sort = 8, summary = "Display the details of specified class and method",
        eg = {
                "watch -p org.apache.commons.lang.StringUtils isBlank 返回参数信息",
                "watch -r org.apache.commons.lang.StringUtils isBlank 返回结果信息",
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
    public void excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        for(Class clazz:classes) {

            if (clazz.getName().equals(classPattern)) {

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
    }

    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        StringBuffer result = new StringBuffer();
        result.append("className:"+className+"\r\n");
        result.append("methodName:"+methodName+"\r\n");
        //result.append("return:"+JSON.toJSONString(target)+"\r\n");
        result.append("patameter:"+ JSON.toJSONString(args)+"\r\n");
        ctxT.writeAndFlush(result.toString());
    }

    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {
        StringBuffer result = new StringBuffer();
        result.append("className:"+className+"\r\n");
        result.append("methodName:"+methodName+"\r\n");
        result.append("patameter:"+ JSON.toJSONString(args)+"\r\n");
        result.append("return:"+JSON.toJSONString(returnObject)+"\r\n");
        ctxT.writeAndFlush(result.toString());

    }
}
