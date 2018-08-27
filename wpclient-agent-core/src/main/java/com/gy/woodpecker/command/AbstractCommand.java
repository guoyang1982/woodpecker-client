package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.enumeration.ClassTypeEnum;
import com.gy.woodpecker.enumeration.CommandEnum;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/8 上午10:41
 */
public abstract class AbstractCommand implements Command{
    ChannelHandlerContext ctxT;
    int sessionId;
    boolean res = true;
    public  boolean isWait = false;
    String outPath = getProperty("user.home") + separatorChar + "woodpecker-analysis" + separatorChar;

    public void setCtxT(ChannelHandlerContext ctxT) {
        this.ctxT = ctxT;
    }
    public ChannelHandlerContext getCtxT() {
        return ctxT;
    }

    @Override
    public boolean getIfEnhance() {
        return false;
    }

    @Override
    public CommandEnum getCommandType(){
        return CommandEnum.OTHER;
    }

    @Override
    public boolean getIfAllNotify() {
        return false;
    }

    @Override
    public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
    }

    @Override
    public void after(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args,
                      Object returnObject) throws Throwable {
    }
    @Override
    public void afterOnThrowing(ClassLoader loader, String className, String methodName, String methodDesc,
                             Object target, Object[] args,Throwable returnObject){
    }

    @Override
    public void invokeBeforeTracing(int lineNumber, String owner, String name, String desc){
    }

    @Override
    public void invokeAfterTracing(int lineNumber, String owner, String name, String desc){
    }
    @Override
    public void invokePrint(ClassLoader loader, String className, String methodName,
                     Object printTarget){
    }
    @Override
    public void invokeThrowTracing(int lineNumber, String owner, String name, String desc, Object throwException){
    }
    @Override
    public void destroy(){
    }
    public void setSessionId(int sessionId){
        this.sessionId = sessionId;
    }

    public int getSessionId(){
        return sessionId;
    }

    public String getValue(){
        return "";
    }
    public String getLineNumber(){
        return "";
    }

    public boolean getRes(){return res;};

    public void setRes(boolean res){
        this.res = res;
    }


    public void doAction(ChannelHandlerContext ctx, Instrumentation inst) {
        this.ctxT = ctx;
        boolean re = this.excute(inst);
        if(!re){
            ctxT.writeAndFlush("excute fail!\n");
            ctxT.writeAndFlush("\0");
            return;
        }
        if(!isWait){
            ctxT.writeAndFlush("\0");
        }else {
            //类增强 并 等待结果
            ctxT.writeAndFlush("Press Ctrl+D to abort.\n");
        }
    }

    public abstract boolean excute(Instrumentation inst);
}
