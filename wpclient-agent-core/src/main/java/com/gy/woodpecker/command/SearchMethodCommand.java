package com.gy.woodpecker.command;

import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.textui.TLadder;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.textui.ext.TClassInfo;
import com.gy.woodpecker.textui.ext.TGaMethodInfo;
import com.gy.woodpecker.tools.GaMethod;
import com.gy.woodpecker.tools.ReflectManagerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/15 上午10:17
 */
@Slf4j
@Cmd(name = "sm", sort = 13, summary = "Search the method of classes loaded by JVM",
        eg = {
                "sm org.apache.commons.lang.StringUtils"
        })
public class SearchMethodCommand extends AbstractCommand {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean result = false;
        for(Class clazz:classes){
            if(clazz.getName().equals(classPattern)){
                result = true;
                final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                        new TTable.ColumnDefine(30, TTable.Align.LEFT),
                        new TTable.ColumnDefine(100, TTable.Align.LEFT),
                })
                        .addRow("DECLARED-CLASS", "VISIBLE-METHOD")
                        .padding(1);

                renderingMethodSummary(tTable, clazz);
                ctxT.writeAndFlush(tTable.rendering());
            }
        }
        return result;
    }

    /*
     * 渲染类方法摘要信息
     */
    private void renderingMethodSummary(final TTable view, final Class<?> clazz) {

        final TLadder classLadderView = new TLadder();

        Set<Method> methods = ReflectManagerUtils.listVisualMethod(clazz);

        for (Method method : methods) {
            final GaMethod gaMethod = new GaMethod.MethodImpl(method);
            view.addRow(classLadderView.rendering(), new TGaMethodInfo(gaMethod).rendering());
        }

    }
}
