package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.textui.ext.TClassInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * @author guoyang
 * @Description: 展示类信息
 * @date 2017/12/13 下午4:29
 */
@Slf4j
@Cmd(name = "sc", sort = 10, summary = "Display class information",
        eg = {
                "sc org.apache.commons.lang.StringUtils isBlank"
        })
public class SearchClassInfoCommand extends AbstractCommand {
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;
    @Override
    public void excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        for(Class clazz:classes){
            if(clazz.getName().equals(classPattern)){
                ctxT.writeAndFlush(new TClassInfo(clazz, true).rendering());
            }
        }
    }
}
