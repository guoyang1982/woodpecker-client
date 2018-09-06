package com.gy.woodpecker.transformer;

import com.gy.woodpecker.asm.AsmClassVisitor;
import com.gy.woodpecker.command.Command;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.enumeration.CommandEnum;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/9/3 下午1:54
 */
@Slf4j
public class SpyAsmTransformer implements ClassFileTransformer {

    private static final String WORKING_DIR = getProperty("user.home");
    Command command;
    String methodName;

    boolean beforeMethod;
    boolean afterMethod;

    public SpyAsmTransformer(Command command, String methodName, boolean beforeMethod, boolean afterMethod) {
        this.command = command;
        this.methodName = methodName;
        this.beforeMethod = beforeMethod;
        this.afterMethod = afterMethod;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        byte[] byteCode = classfileBuffer;
        //}
        if (classBeingRedefined == null) {
            //类对象为null 不转换
            return null;
        }
        //className = className.replace('/', '.');

        List classNames = ContextConfig.classNameCache.get(command.getSessionId());
        if (null == classNames) {
            classNames = new ArrayList();
            ContextConfig.classNameCache.put(command.getSessionId(), classNames);
        }

        if (!classNames.contains(classBeingRedefined)) {
            classNames.add(classBeingRedefined);
        }

        if (null == loader) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        ClassReader cr = null;
        try {
            cr = new ClassReader(classfileBuffer);
            //COMPUTE_FRAMES | COMPUTE_MAXS 自动计算栈贞大小 还有写优化看greys
            ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);
            boolean trace = command.getCommandType().equals(CommandEnum.TRACE);
            ClassVisitor cv = new AsmClassVisitor(cw, command.getSessionId(), trace, beforeMethod, afterMethod, className, methodName);
            cr.accept(cv, EXPAND_FRAMES);
            byteCode = cw.toByteArray();

            if (ContextConfig.isdumpClass) {
                dumpClassIfNecessary(WORKING_DIR + separatorChar + "woodpecker-class-dump/" + className, byteCode);
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        //ContextConfig.classBytesCache.put(classBeingRedefined, byteCode);

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

}
