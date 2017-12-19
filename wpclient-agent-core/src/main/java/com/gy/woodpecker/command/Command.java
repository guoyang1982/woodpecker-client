package com.gy.woodpecker.command;

import com.gy.woodpecker.enumeration.ClassTypeEnum;
import com.gy.woodpecker.enumeration.CommandEnum;
import io.netty.channel.ChannelHandlerContext;

import java.lang.instrument.Instrumentation;


public interface Command {
    /**
     * 获取是否增强类
     * @return
     */
    public boolean getIfEnhance();

    /**
     * 获取上下文信息
     * @return
     */
    public ChannelHandlerContext getCtxT();

    /**
     * 命令类型
     * @return
     */
    public CommandEnum getCommandType();

    /**
     * 获取是否全部广播
     * 多人客户端增强时使用,如果为false，只通知客户端自己的增强动作
     * @return
     */
    public boolean getIfAllNotify();

    /**
     * 获取命令动作
     *
     * @return 返回命令所对应的命令动作
     */
    public void doAction(ChannelHandlerContext ctx,Instrumentation inst);

    /**
     * 前置通知
     *
     * @param loader     类加载器
     * @param className  类名
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     目标类实例
     *                   若目标为静态方法,则为null
     * @param args       参数列表
     * @throws Throwable 通知过程出错
     */
    void before(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable;

    void after(ClassLoader loader, String className, String methodName, String methodDesc,
                              Object target, Object[] args,Object returnObject) throws Throwable;

    void invokeBeforeTracing(int lineNumber, String owner, String name, String desc);

    void invokeAfterTracing(int lineNumber, String owner, String name, String desc);

    void invokePrint(ClassLoader loader, String className, String methodName,
                Object printTarget);

    void invokeThrowTracing(int lineNumber, String owner, String name, String desc, Object throwException);

    public void setSessionId(int sessionId);

    public int getSessionId();

    public String getValue();

    public String getLineNumber();

    public boolean getRes();

    public void setRes(boolean res);
}
