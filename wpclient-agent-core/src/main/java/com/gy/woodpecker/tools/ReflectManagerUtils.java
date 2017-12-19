package com.gy.woodpecker.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/15 上午10:38
 */
public class ReflectManagerUtils {

    public static Set<Method> listVisualMethod(final Class<?> clazz) {
        final Set<Method> methodSet = new LinkedHashSet<Method>();

        // 首先查出当前类所声明的所有方法
        final Method[] classDeclaredMethodArray = clazz.getDeclaredMethods();
        if (null != classDeclaredMethodArray) {
            for (Method declaredMethod : classDeclaredMethodArray) {
                methodSet.add(declaredMethod);
            }
        }

        // 查出当前类所有的父类
        final Collection<Class<?>> superClassSet = recGetSuperClass(clazz);

        // 查出所有父类的可见方法
        for (Class<?> superClass : superClassSet) {
            final Method[] superClassDeclaredMethodArray = superClass.getDeclaredMethods();
            if (null != superClassDeclaredMethodArray) {
                for (Method superClassDeclaredMethod : superClassDeclaredMethodArray) {

                    final int modifier = superClassDeclaredMethod.getModifiers();

                    // 私有方法可以过滤掉
                    if (Modifier.isPrivate(modifier)) {
                        continue;
                    }

                    // public & protected 这两种情况是可以通过继承可见
                    // 所以放行
                    else if (Modifier.isPublic(modifier)
                            || Modifier.isProtected(modifier)) {
                        methodSet.add(superClassDeclaredMethod);
                    }

                    // 剩下的情况只剩下默认, 默认的范围需要同包才能生效
                    else if (null != clazz
                            && null != superClassDeclaredMethod
                            && null != superClassDeclaredMethod.getDeclaringClass()
                            && GaCheckUtils.isEquals(clazz.getPackage(), superClassDeclaredMethod.getDeclaringClass().getPackage())) {
                        methodSet.add(superClassDeclaredMethod);
                    }

                }
            }
        }

        return methodSet;
    }

    /**
     * 获取目标类的父类
     * 因为Java的类继承关系是单父类的，所以按照层次排序
     *
     * @param targetClass 目标类
     * @return 目标类的父类列表(顺序按照类继承顺序倒序)
     */
    public static ArrayList<Class<?>> recGetSuperClass(Class<?> targetClass) {

        final ArrayList<Class<?>> superClassList = new ArrayList<Class<?>>();
        Class<?> currentClass = targetClass;
        do {
            final Class<?> superClass = currentClass.getSuperclass();
            if (null == superClass) {
                break;
            }
            superClassList.add(currentClass = superClass);
        } while (true);
        return superClassList;

    }
}
