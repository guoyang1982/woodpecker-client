package com.gy.woodpecker.command;

import com.gy.woodpecker.tools.ConfigPropertyUtile;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/8 上午10:41
 */
public abstract class AbstractCommand implements Command{
    ChannelHandlerContext ctxT;
    int sessionId;

    @Override
    public boolean getIfEnhance() {
        return false;
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

    public void setSessionId(int sessionId){
        this.sessionId = sessionId;
    }

    public int getSessionId(){
        return sessionId;
    }

    public void doAction(ChannelHandlerContext ctx, Instrumentation inst) {
        this.ctxT = ctx;
        this.excute(inst);
    }

    public abstract void excute(Instrumentation inst);
}
