package com.gy.woodpecker;


/**
 * 通知点
 */
public final class Advice {

    public final ClassLoader loader;
    public final Object target;
    public final Object[] params;
    public final Object returnObj;
    public final Throwable throwExp;
    public final String methodName;


    /**
     *
     * @param loader    类加载器
     * @param target    目标类
     * @param params    调用参数
     * @param returnObj 返回值
     * @param throwExp  抛出异常
     */
    public Advice(
            ClassLoader loader,
            Object target,
            String methodName,
            Object[] params,
            Object returnObj,
            Throwable throwExp) {
        this.loader = loader;
        this.target = target;
        this.methodName = methodName;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
    }

}
