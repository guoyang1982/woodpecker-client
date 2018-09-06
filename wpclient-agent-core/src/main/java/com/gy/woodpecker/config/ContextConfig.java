package com.gy.woodpecker.config;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/14 下午7:01
 */
public class ContextConfig {
    public static boolean isdumpClass = false;
    public static Instrumentation inst = null;

    // 类-字节码缓存
    public static Map<Class<?>/*Class*/, byte[]/*bytes of Class*/> classBytesCache
            = new WeakHashMap<Class<?>, byte[]>();

    public static Map<Integer, List> classNameCache
            = new HashMap<Integer, List>();

    public static void setInst(Instrumentation inst) {
        ContextConfig.inst = inst;
    }
}
