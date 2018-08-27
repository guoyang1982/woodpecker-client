package com.gy.woodpecker.command;

import com.alibaba.fastjson.JSON;
import com.gy.woodpecker.command.annotation.Cmd;
import com.gy.woodpecker.command.annotation.IndexArg;
import com.gy.woodpecker.command.annotation.NamedArg;
import com.gy.woodpecker.enumeration.ClassTypeEnum;
import com.gy.woodpecker.enumeration.CommandEnum;
import com.gy.woodpecker.textui.TKv;
import com.gy.woodpecker.textui.TTable;
import com.gy.woodpecker.tools.DailyRollingFileWriter;
import com.gy.woodpecker.transformer.SpyTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/18 下午4:37
 */
@Slf4j
@Cmd(name = "print", sort = 16, summary = "Get the variables in the method",
        eg = {
                "print org.apache.commons.lang.StringUtils isBlank linenumber variable"
        })
public class PrintValueCommand extends AbstractCommand{
    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;
    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "lineNumber", summary = "Insert code line number")
    private String lineNumber;
    @IndexArg(index = 3, name = "variable", summary = "The variables you need to print")
    private String variable;

    @NamedArg(name = "i", summary = "the vatiable is int")
    private boolean isInt = false;
    @NamedArg(name = "l", summary = "the vatiable is long")
    private boolean isLong = false;
    @NamedArg(name = "f", summary = "the vatiable is float")
    private boolean isFloat = false;
    @NamedArg(name = "d", summary = "the vatiable is double")
    private boolean isDouble = false;
//    @NamedArg(name = "o", summary = "the vatiable is object")
//    private boolean isObject = false;

    @NamedArg(name = "b", summary = "the vatiable is boolean")
    private boolean isBoolean = false;

    @NamedArg(name = "s", summary = "is not vatiable")
    private boolean isNo = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;

    @NamedArg(name = "o", hasValue = true, summary = "The output path")
    private String output = "";
    /**
     * log writer
     */
    private DailyRollingFileWriter fileWriter;
    private final AtomicInteger timesRef = new AtomicInteger();

    @Override
    public boolean getIfEnhance() {
        return true;
    }

    @Override
    public CommandEnum getCommandType() {
        return CommandEnum.PRINT;
    }

    @Override
    public String getValue() {
        return variable;
    }
    @Override
    public String getLineNumber(){
        return lineNumber;
    }

    @Override
    public boolean excute(Instrumentation inst) {
        Class[] classes = inst.getAllLoadedClasses();
        boolean has = false;
        for(Class clazz:classes) {
            if (clazz.getName().equals(classPattern)) {
                has = true;
                if(isInt){
                    variable = "Integer.valueOf("+variable+")";
                }
                if(isLong){
                    variable = "Long.valueOf("+variable+")";
                }
                if(isFloat){
                    variable = "Float.valueOf("+variable+")";
                }
                if(isDouble){
                    variable = "Double.valueOf("+variable+")";
                }
                if(isBoolean){
                    variable = "Boolean.valueOf("+variable+")";
                }
                //不打印变量 只在特定行做标记
                if(isNo){
                    variable = "\""+variable+"\"";
                }
                //默认是对象
//                if(isObject){
//                }

                SpyTransformer transformer = new SpyTransformer(methodPattern,false,false,this);
                inst.addTransformer(transformer, true);
                try {
                    inst.retransformClasses(clazz);
                } catch (Exception e){
                    log.error("执行PrintValueCommand异常{}",e);
                }finally {
                    inst.removeTransformer(transformer);
                }
            }
        }

        if (has && StringUtils.isNotBlank(output)) {
            fileWriter = new DailyRollingFileWriter(outPath + output);
        }

        if(!has){
            setRes(has);
        }
        //等待结果
        super.isWait = true;
        return res;
    }
    //判断是否超过次数
    private boolean isOverThreshold(int currentTimes) {
        return null != threshold
                && currentTimes > threshold;
    }
    @Override
    public void invokePrint(ClassLoader loader, String className, String methodName,
                            Object printTarget){
        //调用次数判断
        if (isOverThreshold(timesRef.incrementAndGet())) {
            //超过设置的调用次数 结束
            timesRef.set(0);
            ctxT.writeAndFlush("\n\0");
            return;
        }
        final TKv tKv = new TKv(
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT));
        tKv.add("className",className);
        tKv.add("methodName",methodName);
        tKv.add("variable",variable);
        tKv.add("value", JSON.toJSONString(printTarget));

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine()
        });

        tTable.addRow(tKv.rendering());
        if (StringUtils.isNotBlank(output)) {
            fileWriter.append(tTable.rendering());
            fileWriter.flushAppend();
        } else {
            ctxT.writeAndFlush(tKv.rendering());
        }
    }

    @Override
    public void destroy() {
        if(null != fileWriter){
            fileWriter.closeFile();
        }
    }
}
