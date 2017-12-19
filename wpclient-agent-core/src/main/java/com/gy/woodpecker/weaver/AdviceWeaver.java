package com.gy.woodpecker.weaver;

import com.gy.woodpecker.command.Command;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/7 下午6:20
 */
@Slf4j
public class AdviceWeaver {
    // 通知监听器集合
    private final static Map<Integer/*ADVICE_ID*/, Command> advices
            = new ConcurrentHashMap<Integer, Command>();


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
     */
    public static void methodOnBegin(
            int adviceId,
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) {

        if (!advices.containsKey(adviceId)) {
            return;
        }

        try {

            Command command = advices.get(adviceId);
            // 获取通知器并做前置通知
            before(command, loader, className, methodName, methodDesc, target, args);

        } finally {
        }

    }

    /**
     * 方法结束
     * @param adviceId
     * @param loader
     * @param className
     * @param methodName
     * @param methodDesc
     * @param target
     * @param args
     * @param returnObj
     */
    public static void methodOnEnd(
            int adviceId,
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Object returnObj) {

        if (!advices.containsKey(adviceId)) {
            return;
        }

        try {

            Command command = advices.get(adviceId);
            // 获取通知器并做后置通知
            after(command, loader, className, methodName, methodDesc, target, args,returnObj);

        } finally {
        }

    }

    /**
     * 方法内部调用开始
     *
     * @param adviceId   通知ID
     * @param lineNumber 代码行号
     * @param owner      调用类名
     * @param name       调用方法名
     * @param desc       调用方法描述
     */
    public static void methodOnInvokeBeforeTracing(int adviceId, int lineNumber, String owner, String name, String desc) {
        if (!advices.containsKey(adviceId)) {
            return;
        }
        Command command = advices.get(adviceId);
        try{
            command.invokeBeforeTracing(lineNumber, owner, name, desc);

        }finally {

        }
    }


    /**
     * 方法内部调用结束(正常返回)
     *
     * @param adviceId   通知ID
     * @param lineNumber 代码行号
     * @param owner      调用类名
     * @param name       调用方法名
     * @param desc       调用方法描述
     */
    public static void methodOnInvokeAfterTracing(int adviceId, int lineNumber, String owner, String name, String desc) {
        if (!advices.containsKey(adviceId)) {
            return;
        }
        Command command = advices.get(adviceId);
        try{
            command.invokeAfterTracing(lineNumber, owner, name, desc);

        }finally {

        }
    }

    private static void before(Command command,
                               ClassLoader loader, String className, String methodName, String methodDesc,
                               Object target, Object[] args) {

        if (null != command) {
            try {
                command.before(loader, className, methodName, methodDesc, target, args);
            } catch (Throwable t) {
                log.warn("advice before failed.", t);
            }
        }

    }

    private static void after(Command command,
                               ClassLoader loader, String className, String methodName, String methodDesc,
                               Object target, Object[] args,Object returnObject) {

        if (null != command) {
            try {
                command.after(loader, className, methodName, methodDesc, target, args,returnObject);
            } catch (Throwable t) {
                log.warn("advice before failed.", t);
            }
        }

    }

    public static void printMethod(int adviceId,
                                   ClassLoader loader, String className, String methodName,
                                   Object printTarget){
        if (!advices.containsKey(adviceId)) {
            return;
        }
        Command command = advices.get(adviceId);
        try{
            command.invokePrint(loader, className,methodName,printTarget);
        }finally {

        }
    }

    /**
     * 方法内部调用(异常返回)
     *
     * @param adviceId       通知ID
     * @param lineNumber     代码行号
     * @param owner          调用类名
     * @param name           调用方法名
     * @param desc           调用方法描述
     * @param throwException 抛出的异常
     */
    public static void methodOnInvokeThrowTracing(int adviceId, int lineNumber, String owner, String name, String desc, Object throwException) {
        if (!advices.containsKey(adviceId)) {
            return;
        }
        Command command = advices.get(adviceId);
        try{
            command.invokeThrowTracing(lineNumber, owner,name,desc,throwException);
        }finally {

        }
    }


    private static Command getListener(int adviceId) {
        return advices.get(adviceId);
    }


    /**
     * 注册监听器
     *
     * @param adviceId 通知ID
     * @param command 命令
     */
    public static void reg(int adviceId, Command command) {

        // 触发监听器创建
        //listener.create();

        // 注册监听器
        advices.put(adviceId, command);

        log.info("reg adviceId={};listener={}", adviceId, command);
    }
}
