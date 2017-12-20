package com.gy.woodpecker.config;

import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/14 下午7:01
 */
public class ContextConfig {
    public static boolean isdumpClass = false;
    public static Instrumentation inst = null;

    public static void setInst(Instrumentation inst) {
        ContextConfig.inst = inst;
    }
}
