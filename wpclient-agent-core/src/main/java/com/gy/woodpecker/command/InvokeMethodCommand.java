package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author guoyang
 * @Description: 远程调用类的方法命令
 * @date 2017/12/7 下午6:24
 */
@Slf4j
@Cmd(name = "invoke", sort = 9, summary = "The method of calling the execution class remotely",
        eg = {
                "invoke class method"
        })
public class InvokeMethodCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "s", summary = "String是实例化的bean")
    private boolean isSpring = false;

    @Override
    public boolean excute(Instrumentation inst) {
        if(isSpring){
            Class[] classes = inst.getAllLoadedClasses();
            for(Class clazz:classes) {
                if (clazz.getName().equals(classPattern)) {
                    ClassLoader appClassLoader = clazz.getClassLoader();
                    try {
//                        final Class<?> classOfContextLoader = appClassLoader.loadClass("org.springframework.web.context.ContextLoader");
//                        final Object objectOfWebApplicationContext = classOfContextLoader.getMethod("getCurrentWebApplicationContext").invoke(null);
//                        final Class<?> classOfWebApplicationContext = appClassLoader.loadClass("org.springframework.web.context.WebApplicationContext");
//                        final Object objectOfCommand = classOfWebApplicationContext.getMethod("getBean",Class.class).invoke(objectOfWebApplicationContext,clazz);
//
//                        final Object reObj = clazz.getMethod(methodPattern).invoke(objectOfCommand);


                       // ctxT.writeAndFlush(JSON.toJSONString(reObj)+"\r\n");


                    } catch (Exception e) {
                        log.error("invoke method fail!", e);
                        ctxT.writeAndFlush("invoke method fail!\r\n");

                    }

                }
            }

        }else {
            try {
                //在两个classloader里会存在两份，应用的和agent的，先这样，从inst里获取还要是已经加载的
                final Class<?> classOfCommand = Class.forName(classPattern);
                //final Class<?> classOfCommand = classLoader.loadClass(classPattern);
                Method method = classOfCommand.getMethod(methodPattern);
                if (Modifier.isStatic(method.getModifiers())) {
                    //静态方法
                    Object reObj = method.invoke(null);
                    ctxT.writeAndFlush(JSON.toJSONString(reObj)+"\n");
                } else {
                    //非静态方法
                    Object obj = classOfCommand.newInstance();
                    Object reObj = method.invoke(obj);
                    ctxT.writeAndFlush(JSON.toJSONString(reObj)+"\n");
                }
            } catch (Exception e) {
                log.error("invoke method fail!", e);
            }
        }
        return true;
    }
}
