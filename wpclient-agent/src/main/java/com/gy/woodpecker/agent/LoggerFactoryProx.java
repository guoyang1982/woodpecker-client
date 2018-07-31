package com.gy.woodpecker.agent;

import java.lang.reflect.InvocationTargetException;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/11/24 下午4:52
 */
public class LoggerFactoryProx {

    private static Class<?> classOfLog = null;
    private static Object objectOfLog = null;

    public static void init(Class<?> classOfLogT,Object objectOfLogT){
        classOfLog = classOfLogT;
        objectOfLog = objectOfLogT;
    }

    public static void sendToRedis(final String msg,Object[] params,Throwable t){
        try {
            classOfLog.getMethod("sendToRedis",String.class,Object[].class,Throwable.class).invoke(objectOfLog,msg,params,t);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void sendToRedis(final String msg){
        try {
            classOfLog.getMethod("sendToRedis",String.class,Object[].class).invoke(objectOfLog,msg,null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void healthCheck(){
        try {
            classOfLog.getMethod("healthCheck").invoke(objectOfLog);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
