package com.gy.woodpecker.handler;

import com.gy.woodpecker.Exception.CommandException;
import com.gy.woodpecker.command.Command;
import com.gy.woodpecker.command.Commands;
import com.gy.woodpecker.weaver.AdviceWeaver;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/7 下午1:05
 */
public class DefaultCommandHandler implements CommandHandler{
    private static final AtomicInteger commandIndexSequence = new AtomicInteger(0);

    @Override
    public void executeCommand(String line, ChannelHandlerContext ctx,Instrumentation inst,int sessionId) throws IOException {

        try {
            Command command = Commands.getInstance().newCommand(line);
            if(null == command){
                ctx.writeAndFlush("无此命令!\r\n");
                return;
            }
            //判断是否需要动态增强
            if(command.getIfEnhance()){
                //需要增强
                command.setSessionId(sessionId);
                command.doAction(ctx,inst);
               // ctx.channel().
                AdviceWeaver.reg(sessionId,command);

            }else{
                command.doAction(ctx,inst);
            }
        } catch (CommandException e) {
            e.printStackTrace();
        }
    }
}
