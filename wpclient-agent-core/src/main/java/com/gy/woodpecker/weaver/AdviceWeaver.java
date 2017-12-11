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
     * 方法开始<br/>
     * 用于编织通知器,外部不会直接调用
     *
     * @param loader     类加载器
     * @param adviceId   通知ID
     * @param className  类名
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     返回结果
     *                   若为无返回值方法(void),则为null
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
