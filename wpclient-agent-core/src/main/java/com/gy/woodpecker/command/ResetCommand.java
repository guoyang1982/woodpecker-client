package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.config.ContextConfig;
import com.gy.woodpecker.transformer.SpyTransformer;
import com.gy.woodpecker.weaver.AdviceWeaver;
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
    public boolean excute(Instrumentation inst) {
        Map<Integer, List> classNames = ContextConfig.classNameCache;
        if (classNames.size() == 0) {
            //还需要清空一遍命令
            Map<Integer, Command> mapCommand = AdviceWeaver.getAdvices();
            if (isAllClass) {
                //回调清理命令
                for (Map.Entry<Integer, Command> entry : mapCommand.entrySet()) {
                    Command command = entry.getValue();
                    if(null != command){
                        command.destroy();
                    }

                }
            } else {
                //回调清理命令
                Command command = mapCommand.get(getSessionId());
                if(null != command){
                    command.destroy();
                }
            }

            ctxT.writeAndFlush("无需要恢复的类!\n");
            return false;
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
        if (null == classNameList) {
            return false;
        }
        try {
            inst.addTransformer(resetClassFileTransformer, true);

            if (isAllClass) {
                Set set = new HashSet();
                for (List list : classNames.values()) {
                    //去重复
                    set.addAll(list);
                }
                // 批量增强
                final Class<?>[] classArray = new Class<?>[set.size()];
                arraycopy(set.toArray(), 0, classArray, 0, set.size());
                inst.retransformClasses(classArray);
            } else {
                // 批量增强
                final Class<?>[] classArray = new Class<?>[classNameList.size()];
                arraycopy(classNameList.toArray(), 0, classArray, 0, classNameList.size());
                //重转 保存的Class信息，只要transform为null 就恢复了
                inst.retransformClasses(classArray);
            }

        } catch (Exception e) {
            log.error("恢复增强类失败!", e);
        } finally {
            inst.removeTransformer(resetClassFileTransformer);
            Map<Integer, Command> mapCommand = AdviceWeaver.getAdvices();
            if (isAllClass) {
                classNames.clear();
                //回调清理命令
                for (Map.Entry<Integer, Command> entry : mapCommand.entrySet()) {
                    Command command = entry.getValue();
                    if(null != command){
                        command.destroy();
                    }
                }
            } else {
                classNames.remove(getSessionId());
                //回调清理命令
                Command command = mapCommand.get(getSessionId());
                if(null != command){
                    command.destroy();
                }
            }

            ctxT.writeAndFlush("已经恢复!\n");
        }
        return true;
    }
}
