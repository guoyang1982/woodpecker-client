package com.gy.woodpecker.transformer;

import com.gy.woodpecker.command.Command;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.enumeration.CommandEnum;
import javassist.*;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;
import javassist.expr.MethodCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
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
    private static final String WORKING_DIR = getProperty("user.home");

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
        if(classBeingRedefined == null){
            //类对象为null 不转换
            return null;
        }

        className = className.replace('/', '.');

        List classNames = classNameCache.get(command.getSessionId());
        if (null == classNames) {
            classNames = new ArrayList();
            classNameCache.put(command.getSessionId(), classNames);
        }

        if (!classNames.contains(classBeingRedefined)) {
            classNames.add(classBeingRedefined);
        }

        if (null == loader) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        byteCode = aopLog(loader, className, byteCode);

        classBytesCache.put(classBeingRedefined, byteCode);
        return byteCode;
    }

    private byte[] aopLog(ClassLoader loader, String className, byte[] byteCode) {
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = null;
            cp.insertClassPath(new LoaderClassPath(loader));
            cc = cp.get(className);
            byteCode = aopLog(loader, cc, className, byteCode);
        } catch (Exception ex) {
            log.info("the applog exception:{}", ex);
            this.command.setRes(false);
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
                    if (StringUtils.isEmpty(methodName)) {
                        //这个类的所有方法都增强 但是排除set get is打头的方法
                        if (m.getName().startsWith("get") || m.getName().startsWith("set") || m.getName().startsWith("is")) {
                            continue;
                        }
                        aopLog(loader, className, m);
                    } else if (m.getName().equals(methodName)) {
                        aopLog(loader, className, m);
                    }
                }
                byteCode = cc.toBytecode();
            }
        }
        cc.detach();
        if (ContextConfig.isdumpClass) {
            dumpClassIfNecessary(WORKING_DIR + separatorChar + "woodpecker-class-dump/" + className, byteCode);
        }
        return byteCode;
    }

    /*
    * dump class to file
    */
    private static void dumpClassIfNecessary(String className, byte[] data) {

        final File dumpClassFile = new File(className + ".class");
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
        String classLoad = className + ".class.getClassLoader()";


        //先在before之前做子函数调用增强，以免把before增强的代码给增强
        if (command.getCommandType().equals(CommandEnum.TRACE)) {
            m.instrument(new ExprEditor() {
                public void edit(MethodCall m)
                        throws CannotCompileException {
                    Integer lineNumber = m.getLineNumber();
                    String clazzName = m.getClassName();
                    String methodName = m.getMethodName();
                    String methodDes = "";
                    try {
                        MethodInfo methodInfo1 = m.getMethod().getMethodInfo();
                        methodDes = methodInfo1.getDescriptor();
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                    String before = "com.gy.woodpecker.agent.Spy.methodOnInvokeBeforeTracing(" + command.getSessionId() + "," + lineNumber + ",\"" + clazzName + "\",\"" + methodName + "\",\"" + methodDes + "\");";
                    String after = "com.gy.woodpecker.agent.Spy.methodOnInvokeAfterTracing(" + command.getSessionId() + "," + lineNumber + ",\"" + clazzName + "\",\"" + methodName + "\",\"" + methodDes + "\");";
                    m.replace("{ " + before + " $_ = $proceed($$); " + after + "}");
                }
            });
        }


        //插入addcatch,这里的不需要插入自己的间谍分析代码，但是要获取异常信息和返回信息
        /**
         * addCatch() 指的是在方法中加入try catch 块，需要注意的是，必须在插入的代码中，加入return 值$e代表 异常值。比如：
         CtMethod m = ...;
         CtClass etype = ClassPool.getDefault().get("java.lang.Exception");
         m.addCatch("{ System.out.println($e); throw $e; }", etype);
         实际代码如下：
         try {
         the original method body
         }
         catch (java.lang.Exception e) {
         System.out.println(e);
         throw e;
         }

         */
        if (afterMethod) {
            StringBuffer afterThrowsBody = new StringBuffer();

            CtClass etype = null;
            try {
                etype = ClassPool.getDefault().get("java.lang.Exception");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            // 判断是否为静态方法
            if (Modifier.isStatic(m.getModifiers())) {
                afterThrowsBody.append("com.gy.woodpecker.agent.Spy.methodOnThrowingEnd(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,null,$args,$e);");
            } else {
                afterThrowsBody.append("com.gy.woodpecker.agent.Spy.methodOnThrowingEnd(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,this,$args,$e);");
            }

            m.addCatch("{" + afterThrowsBody.toString() + "; throw $e; }", etype);
        }


        /**
         * Handler 代表的是一个try catch 声明。
         */
        if (command.getCommandType().equals(CommandEnum.TRACE)) {
            m.instrument(new ExprEditor() {
                public void edit(Handler h)
                        throws CannotCompileException {
                    Integer lineNumber = h.getLineNumber();
                    String clazzName = "";
                    String methodName = "";
                    String methodDes = "";
                    String throwException = "$1";
                    String before = "com.gy.woodpecker.agent.Spy.methodOnInvokeThrowTracing("
                            + command.getSessionId() + "," + lineNumber + ",\"" + clazzName + "\",\"" + methodName + "\",\"" + methodDes + "\",$1);";
                    if (!h.isFinally()) {
                        h.insertBefore(before);
                    }
                }
            });
        }

        if (command.getCommandType().equals(CommandEnum.PRINT)) {
            String objPrintValue = command.getValue();
            String printInfo = "com.gy.woodpecker.agent.Spy.printMethod(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\"," + objPrintValue + ");";
            m.insertAt(Integer.parseInt(command.getLineNumber()), printInfo);

        }

        StringBuffer beforeBody = new StringBuffer();
        if (beforeMethod) {
            // 判断是否为静态方法
            if (Modifier.isStatic(m.getModifiers())) {
                beforeBody.append("com.gy.woodpecker.agent.Spy.beforeMethod(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,null,$args);");
            } else {
                beforeBody.append("com.gy.woodpecker.agent.Spy.beforeMethod(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,this,$args);");
            }
            m.insertBefore(beforeBody.toString());
        }

        StringBuffer afterBody = new StringBuffer();

        if (afterMethod) {
            Object result = "$_";

            try {
                CtClass cc = m.getReturnType();
                String retype = cc.getName();

                if (retype.equals("boolean") || retype.equals("double") || retype.equals("int")
                        || retype.equals("long") || retype.equals("float") || retype.equals("byte") || retype.equals("char")) {

                    result = "String.valueOf($_)";

                }

            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            // 判断是否为静态方法
            if (Modifier.isStatic(m.getModifiers())) {
                afterBody.append("com.gy.woodpecker.agent.Spy.afterMethod(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,null,$args," + result + ");");

            } else {
                afterBody.append("com.gy.woodpecker.agent.Spy.afterMethod(" + command.getSessionId() + "," + classLoad + ",\"" + className + "\",\"" + m.getName() + "\",null,this,$args," + result + ");");
            }
            m.insertAfter(afterBody.toString());
        }
    }

}

