package com.gy.woodpecker.agent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 间谍类<br/>
 * 藏匿在各个ClassLoader中
 * Created by oldmanpushcart@gmail.com on 15/8/23.
 */
public class Spy {


    // -- 各种Advice的钩子引用 --
    public static volatile Method ON_BEFORE_METHOD;
    public static volatile Method ON_RETURN_METHOD;
    public static volatile Method ON_THROWS_METHOD;
    public static volatile Method BEFORE_INVOKING_METHOD;
    public static volatile Method AFTER_INVOKING_METHOD;
    public static volatile Method THROW_INVOKING_METHOD;

    public static volatile Method PRINT_METHOD;

    /**
     * 代理重设方法
     */
    public static volatile Method AGENT_RESET_METHOD;

    public static void beforeMethod(int adviceId,
                                   ClassLoader loader, String className, String methodName, String methodDesc,
                                   Object target, Object[] args){

        try {
            ON_BEFORE_METHOD.invoke(null, adviceId,loader,className,methodName, methodDesc,target,args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void afterMethod(int adviceId,
                                    ClassLoader loader, String className, String methodName, String methodDesc,
                                    Object target, Object[] args,Object reObj){

        try {
            ON_RETURN_METHOD.invoke(null, adviceId,loader,className,methodName, methodDesc,target,args,reObj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        try {
            BEFORE_INVOKING_METHOD.invoke(null, adviceId,lineNumber,owner,name,desc);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        try {
            AFTER_INVOKING_METHOD.invoke(null, adviceId,lineNumber,owner,name,desc);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void printMethod(int adviceId,
                                   ClassLoader loader, String className, String methodName,
                                   Object printTarget){
        try {
            PRINT_METHOD.invoke(null, adviceId,loader,className,methodName,printTarget);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void methodOnInvokeThrowTracing(int adviceId, int lineNumber, String owner, String name, String desc, Object throwException) {

        try {
            THROW_INVOKING_METHOD.invoke(null, adviceId,lineNumber,owner,name,desc,throwException);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /*
     * 用于普通的间谍初始化
     */
    public static void init(
            @Deprecated
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod,
            Method onThrowsMethod,
            Method beforeInvokingMethod,
            Method afterInvokingMethod,
            Method throwInvokingMethod,
            Method printMethod) {
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
        ON_THROWS_METHOD = onThrowsMethod;
        BEFORE_INVOKING_METHOD = beforeInvokingMethod;
        AFTER_INVOKING_METHOD = afterInvokingMethod;
        THROW_INVOKING_METHOD = throwInvokingMethod;
        PRINT_METHOD = printMethod;
    }

    /*
     * 用于启动线程初始化
     */
    public static void initForAgentLauncher(
            @Deprecated
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod,
            Method onThrowsMethod,
            Method beforeInvokingMethod,
            Method afterInvokingMethod,
            Method throwInvokingMethod,
            Method agentResetMethod,
            Method printMethod) {
        System.out.println("?????????????????????"+onBeforeMethod+onReturnMethod+onThrowsMethod+beforeInvokingMethod+afterInvokingMethod+throwInvokingMethod+agentResetMethod+printMethod);
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
        ON_THROWS_METHOD = onThrowsMethod;
        BEFORE_INVOKING_METHOD = beforeInvokingMethod;
        AFTER_INVOKING_METHOD = afterInvokingMethod;
        THROW_INVOKING_METHOD = throwInvokingMethod;
        AGENT_RESET_METHOD = agentResetMethod;
        PRINT_METHOD = printMethod;
    }


    public static void clean() {
        ON_BEFORE_METHOD = null;
        ON_RETURN_METHOD = null;
        ON_THROWS_METHOD = null;
        BEFORE_INVOKING_METHOD = null;
        AFTER_INVOKING_METHOD = null;
        THROW_INVOKING_METHOD = null;
        AGENT_RESET_METHOD = null;
        PRINT_METHOD = null;
    }

}
