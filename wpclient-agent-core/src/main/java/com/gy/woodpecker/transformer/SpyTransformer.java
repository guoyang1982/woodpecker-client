package com.gy.woodpecker.transformer;

import com.gy.woodpecker.command.Command;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.weaver.AdviceWeaver;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;

/**
 * Created by guoyang on 17/10/27.
 */
@Slf4j
public class SpyTransformer implements ClassFileTransformer {

    // 类-字节码缓存
    private final static Map<Class<?>/*Class*/, byte[]/*bytes of Class*/> classBytesCache
            = new WeakHashMap<Class<?>, byte[]>();

    public final static Map<Integer, List> classNameCache
            = new HashMap<Integer, List>();

    String methodName;
    boolean beforeMethod;
   // boolean throwMethod;
    boolean afterMethod;
    Command command;

    public SpyTransformer(String methodName, boolean beforeMethod, boolean afterMethod, Command command) {
        this.methodName = methodName;
        this.beforeMethod = beforeMethod;
       // this.throwMethod = throwMethod;
        this.afterMethod = afterMethod;
        this.command = command;
    }

    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        //每次增强从缓存取 用于多人协助，如果不从缓存取 每次都是从classpath拿最原始字节码
       // byte[] byteCode = classBytesCache.get(classBeingRedefined);

        //if(null == byteCode){
        byte[] byteCode = classfileBuffer;
        //}

        className = className.replace('/', '.');

        List classNames = classNameCache.get(command.getSessionId());
        if(null == classNames){
            classNames = new ArrayList();
            classNameCache.put(command.getSessionId(),classNames);
        }

        if(!classNames.contains(classBeingRedefined)){
            classNames.add(classBeingRedefined);
        }

        if (null == loader) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        byteCode = aopLog(loader, className, byteCode);

        classBytesCache.put(classBeingRedefined,byteCode);
        return byteCode;
    }

    private byte[] aopLog(ClassLoader loader, String className, byte[] byteCode) {
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = null;
            try {
                cc = cp.get(className);
            } catch (NotFoundException e) {
                cp.insertClassPath(new LoaderClassPath(loader));
                cc = cp.get(className);
            }
            byteCode = aopLog(loader,cc, className, byteCode);
        } catch (Exception ex) {
            log.info("the applog exception:{}",ex);
        }
        return byteCode;
    }

    private byte[] aopLog(ClassLoader loader, CtClass cc, String className, byte[] byteCode) throws CannotCompileException, IOException {
        if (null == cc) {
            return byteCode;
        }
        if (!cc.isInterface()) {
            CtMethod[] methods = cc.getDeclaredMethods();
            if (null != methods && methods.length > 0) {
                for (CtMethod m : methods) {
                    if (m.getName().equals(methodName)) {
                        aopLog(loader,className, m);
                    }
                }
                byteCode = cc.toBytecode();
            }
        }
        cc.detach();
        if(ContextConfig.isdumpClass){
            dumpClassIfNecessary("./woodpecker-class-dump/"+className,byteCode);
        }
        return byteCode;
    }


    /*
    * dump class to file
    */
    private static void dumpClassIfNecessary(String className, byte[] data) {
//        if (!GlobalOptions.isDump) {
//            return;
//        }
        final File dumpClassFile = new File( className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs()
                && !classPath.exists()) {
            log.warn("create dump classpath:{} failed.", classPath);
            return;
        }

        // 将类字节码写入文件
        try {
            writeByteArrayToFile(dumpClassFile, data);
        } catch (IOException e) {
            log.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }

    }

    private void aopLog(ClassLoader loader, String className, CtMethod m) throws CannotCompileException {

        if (null == m || m.isEmpty()) {
            return;
        }
        System.out.println("进行插桩类:" + className);
        MethodInfo methodInfo = m.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
        Object[] paramNames = new Object[0];
        try {
            paramNames = new String[m.getParameterTypes().length];
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        int pos = Modifier.isStatic(m.getModifiers()) ? 0 : 1;
        for (int i = 0; i < paramNames.length; i++)
            paramNames[i] = attr.variableName(i + pos);
//        // paramNames即参数名
//        for (int i = 0; i < paramNames.length; i++) {
//            System.out.println(paramNames[i]);
//        }

        StringBuffer params = new StringBuffer();
        params.append("java.util.List spyParamNames = new java.util.ArrayList();");
        for (int i=0;i<paramNames.length;i++){
            params.append("spyParamNames.add("+paramNames[i]+");");
        }
        StringBuffer beforeBody = new StringBuffer();

        if(beforeMethod){
            beforeBody.append("com.gy.woodpecker.agent.Spy.beforeMethod("+command.getSessionId()+",null,\""+className+"\",\""+m.getName()+"\",null,null,spyParamNames.toArray());");
            m.insertBefore(params.toString()+beforeBody.toString());
        }
        StringBuffer afterBody = new StringBuffer();

        if(afterMethod){
            afterBody.append("com.gy.woodpecker.agent.Spy.afterMethod("+command.getSessionId()+",null,\""+className+"\",\""+m.getName()+"\",null,null,spyParamNames.toArray(),$_);");
                m.insertAfter(params.toString()+afterBody.toString());
        }
//        if(throwMethod){
//            m.addCatch("", CtClass.);
//        }

    }


}

