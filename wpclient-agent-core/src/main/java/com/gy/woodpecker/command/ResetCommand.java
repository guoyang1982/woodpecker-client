package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;

import static java.lang.System.arraycopy;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/12 上午10:06
 */
@Slf4j
@Cmd(name = "reset", sort = 9, summary = "Restore the enhanced class",
        eg = {
                "reset",
                "reset -a"
        })
public class ResetCommand extends AbstractCommand {
    @NamedArg(name = "a", summary = "是否需要恢复所有的类")
    private boolean isAllClass = false;

    @Override
    public void excute(Instrumentation inst) {

        Map<Integer, List> classNames = SpyTransformer.classNameCache;
        if(classNames.size() == 0){
            ctxT.writeAndFlush("无需要恢复的类!\r\n");
            return;
        }

        final ClassFileTransformer resetClassFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {
                return null;
            }
        };

        List classNameList = classNames.get(getSessionId());
        try {
            inst.addTransformer(resetClassFileTransformer, true);

            if(isAllClass){
                Set set = new HashSet();
                for(List list : classNames.values()){
                    //去重复
                    set.addAll(list);
                }
                // 批量增强
                final Class<?>[] classArray = new Class<?>[set.size()];
                arraycopy(set.toArray(), 0, classArray, 0, set.size());
                inst.retransformClasses(classArray);
            }else{
                // 批量增强
                final Class<?>[] classArray = new Class<?>[classNameList.size()];
                arraycopy(classNameList.toArray(), 0, classArray, 0, classNameList.size());
                inst.retransformClasses(classArray);
            }

        } catch (Exception e){
            log.error("恢复增强类失败!",e);
        }finally {
            inst.removeTransformer(resetClassFileTransformer);
            if(isAllClass){
                classNames.clear();
            }else {
                classNames.remove(getSessionId());
            }
            ctxT.writeAndFlush("已经恢复!\r\n");
        }
    }
}
