package com.gy.woodpecker.asm;

import org.objectweb.asm.MethodVisitor;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/9/3 下午2:06
 */
public class AsmClassVisitor extends ClassVisitor implements Opcodes {

    int adviceId;
    boolean isTracing;
    String className;
    String methodName;
    boolean beforeMethod;
    boolean afterMethod;

    public AsmClassVisitor(ClassVisitor cv, final int adviceId,
                           final boolean isTracing, boolean beforeMethod, boolean afterMethod,
                           final String className, String methodName) {
        super(ASM5, cv);
        this.adviceId = adviceId;
        this.isTracing = isTracing;
        this.beforeMethod = beforeMethod;
        this.afterMethod = afterMethod;
        this.className = className;
        this.methodName = methodName;
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {

        if (StringUtils.isEmpty(methodName)) {
            //这个类的所有方法都增强 但是排除set get is打头的方法
            if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            return new AsmMethodAdapter(ASM5, adviceId, isTracing, beforeMethod, afterMethod, className, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc);
        } else if (name.equals(methodName)) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new AsmMethodAdapter(ASM5, adviceId, isTracing, beforeMethod, afterMethod, className, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
