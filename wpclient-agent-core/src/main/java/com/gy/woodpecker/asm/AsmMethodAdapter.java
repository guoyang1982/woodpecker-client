package com.gy.woodpecker.asm;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/9/3 下午2:36
 */
public class AsmMethodAdapter extends AdviceAdapter {
    int adviceId;
    String className;
    String methodName;
    String methodDescriptor;
    boolean isTracing;
    boolean beforeMethod;
    boolean afterMethod;

    public AsmMethodAdapter(int api, int adviceId, boolean isTracing,boolean beforeMethod, boolean afterMethod, String className, MethodVisitor methodVisitor,
                            int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
        this.adviceId = adviceId;
        this.className = className;
        this.methodName = name;
        this.methodDescriptor = descriptor;
        this.isTracing = isTracing;
        this.beforeMethod = beforeMethod;
        this.afterMethod = afterMethod;
    }

    private Label start = new Label();// 方法方法字节码开始位置
    private Label end = new Label();// 方法方法字节码结束位置

    private final Type ASM_TYPE_CLASS = Type.getType(Class.class);
    private final Type ASM_TYPE_OBJECT = Type.getType(Object.class);

    /**
     * 是否静态方法
     *
     * @return true:静态方法 / false:非静态方法
     */
    private boolean isStaticMethod() {
        return (methodAccess & ACC_STATIC) != 0;
    }

    /**
     * 翻译类名称<br/>
     * 将 java/lang/String 的名称翻译成 java.lang.String
     *
     * @param className 类名称 java/lang/String
     * @return 翻译后名称 java.lang.String
     */
    public static String tranClassName(String className) {
        return StringUtils.replace(className, "/", ".");
    }

    /**
     * 加载ClassLoader<br/>
     * 这里分开静态方法中ClassLoader的获取以及普通方法中ClassLoader的获取
     * 主要是性能上的考虑
     */
    private void loadClassLoader() {

        if (this.isStaticMethod()) {

//                    // fast enhance
//                    if (GlobalOptions.isEnableFastEnhance) {
//                        visitLdcInsn(Type.getType(String.format("L%s;", internalClassName)));
//                        visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
//                    }

            // normal enhance
//                    else {

            // 这里不得不用性能极差的Class.forName()来完成类的获取,因为有可能当前这个静态方法在执行的时候
            // 当前类并没有完成实例化,会引起JVM对class文件的合法性校验失败
            // 未来我可能会在这一块考虑性能优化,但对于当前而言,功能远远重要于性能,也就不打算折腾这么复杂了
            visitLdcInsn(tranClassName(className));
            invokeStatic(ASM_TYPE_CLASS, Method.getMethod("Class forName(String)"));
            invokeVirtual(ASM_TYPE_CLASS, Method.getMethod("ClassLoader getClassLoader()"));
//                    }
        } else {
            loadThis();
            invokeVirtual(ASM_TYPE_OBJECT, Method.getMethod("Class getClass()"));
            invokeVirtual(ASM_TYPE_CLASS, Method.getMethod("ClassLoader getClassLoader()"));
        }
    }

    /**
     * 加载this/null
     */
    private void loadThisOrPushNullIfIsStatic() {
        if (isStaticMethod()) {
            pushNull();
        } else {
            loadThis();
        }
    }


    private void putArgs2Before() {
        push(adviceId);
        //classloader
        loadClassLoader();
        push(className);
        push(methodName);
        push(methodDescriptor);
        //对象
        loadThisOrPushNullIfIsStatic();
        //参数
        loadArgArray();
    }

    @Override
    protected void onMethodEnter() {

        if(beforeMethod){
            putArgs2Before();
            mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "beforeMethod",
                    "(ILjava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V", false);

        }else {
            super.onMethodEnter();
        }
        //做为方法的开始
        mark(start);
    }


    /**
     * 将NULL推入堆栈
     */
    private void pushNull() {
        push((Type) null);
    }

    /**
     * 加载返回值
     *
     * @param opcode 操作吗
     */
    private void loadReturn(int opcode) {
        switch (opcode) {

            case RETURN: {
                pushNull();
                break;
            }

            case ARETURN: {
                dup();
                break;
            }

            case LRETURN:
            case DRETURN: {
                dup2();
                box(Type.getReturnType(methodDesc));
                break;
            }

            default: {
                dup();
                box(Type.getReturnType(methodDesc));
                break;
            }

        }
    }

    /**
     * 方法退出处 如return 等
     *
     * @param opcode
     */
    @Override
    protected void onMethodExit(final int opcode) {
        if(afterMethod){
            putArgs2After(opcode);
            //throw new Exception(); 这种异常结束的不增强，因为每个方法代码都会加try catch 直接在这里抛出打印异常 在visitMaxs
            if (!isThrow(opcode)) {
                mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "afterMethod", "(ILjava/lang/ClassLoader;" +
                        "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)V", false);
            }
        }else {
            super.onMethodExit(opcode);
        }
    }

    private void putArgs2After(int opcode) {
        loadReturn(opcode);
        push(adviceId);
        swap();
        //classloader
        loadClassLoader();
        swap();
        push(className);
        swap();
        push(methodName);
        swap();
        push(methodDescriptor);
        swap();
        //对象
        loadThisOrPushNullIfIsStatic();
        swap();
        //参数
        loadArgArray();
        swap();
    }

    private Integer currentLineNumber;

    @Override
    public void visitLineNumber(int line, Label start) {
        currentLineNumber = line;
        super.visitLineNumber(line, start);
//        if (line == 41) {
////            dup();
////
////            push(adviceId);
////            swap();
////            loadClassLoader();
////            swap();
////            push(className);
////            swap();
////            push(methodName);
////            swap();
////            loadArgArray();
////            swap();
////
////            mv.visitMethodInsn(INVOKESTATIC, "com/gy/asm/test/Spy", "printMethod", "(ILjava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)V", false);
        //  }
    }

    /**
     * 是否抛出异常返回(通过字节码判断)
     *
     * @param opcode 操作码
     * @return true:以抛异常形式返回 / false:非抛异常形式返回(return)
     */
    private boolean isThrow(int opcode) {
        return opcode == ATHROW;
    }

    private final Type ASM_TYPE_THROWABLE = Type.getType(Throwable.class);

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {

        if(isTracing){
            putArgs2Trace(owner, name, desc);
            mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "methodOnInvokeBeforeTracing", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
            final Label beginLabel = new Label();
            final Label endLabel = new Label();
            final Label finallyLabel = new Label();
            // try
            // {

            mark(beginLabel);
            // System.out.println("::" + name);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            mark(endLabel);

            putArgs2Trace(owner, name, desc);
            mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "methodOnInvokeAfterTracing", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
            goTo(finallyLabel);

            // }
            // catch
            // {

            catchException(beginLabel, endLabel, ASM_TYPE_THROWABLE);
            putArgsThrowTrace(owner, name, desc);
            mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "methodOnInvokeThrowTracing", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
            throwException();
            // }
            // finally
            // {
            mark(finallyLabel);
            // }
        }else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private void putArgsThrowTrace(String owner, String name, String desc) {
        loadThrow();
        push(adviceId);
        swap();

        push(currentLineNumber);
        swap();

        push(owner);
        swap();

        push(name);
        swap();

        push(desc);
        swap();
    }

    private void putArgs2Trace(String owner, String name, String desc) {
        push(adviceId);
        push(currentLineNumber);
        push(owner);
        push(name);
        push(desc);
    }


    /**
     * 加载异常
     */
    private void loadThrow() {
        dup();
    }

    /**
     * 在方法里的字节码结束位置 插入catch
     *
     * @param maxStack
     * @param maxLocals
     */
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mark(end);
        visitTryCatchBlock(start, end, mark(), ASM_TYPE_THROWABLE.getInternalName());

        putArgsThrow();
        // 调用方法
        mv.visitMethodInsn(INVOKESTATIC, "com/gy/woodpecker/agent/Spy", "methodOnThrowingEnd", "(ILjava/lang/ClassLoader;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Throwable;)V", false);
        throwException();

        super.visitMaxs(maxStack, maxLocals);

    }

    private void putArgsThrow() {
        loadThrow();
        push(adviceId);
        swap();
        //classloader
        loadClassLoader();
        swap();
        push(className);
        swap();
        push(methodName);
        swap();
        push(methodDescriptor);
        swap();
        //对象
        loadThisOrPushNullIfIsStatic();
        swap();
        //参数
        loadArgArray();
        swap();
    }

    /**
     * TryCatch块,用于ExceptionsTable重排序
     */
    class AsmTryCatchBlock {

        protected final Label start;
        protected final Label end;
        protected final Label handler;
        protected final String type;

        AsmTryCatchBlock(Label start, Label end, Label handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }

    }

    // 用于try-catch的冲排序,目的是让tracing的try...catch能在exceptions tables排在前边
    private final Collection<AsmTryCatchBlock> asmTryCatchBlocks = new ArrayList<AsmTryCatchBlock>();

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        asmTryCatchBlocks.add(new AsmTryCatchBlock(start, end, handler, type));
    }

    @Override
    public void visitEnd() {
        for (AsmTryCatchBlock tcb : asmTryCatchBlocks) {
            super.visitTryCatchBlock(tcb.start, tcb.end, tcb.handler, tcb.type);
        }
        super.visitEnd();
    }
}
