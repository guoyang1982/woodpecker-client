package com.gy.woodpecker.handler;

import com.gy.woodpecker.Exception.CommandException;
import com.gy.woodpecker.command.Command;
import com.gy.woodpecker.command.Commands;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/7 下午1:05
 */
public class DefaultCommandHandler implements CommandHandler{

    @Override
    public void executeCommand(String line, ChannelHandlerContext ctx,Instrumentation inst) throws IOException {

        try {
            Command command = Commands.getInstance().newCommand(line);
            if(null == command){
                ctx.writeAndFlush("无此命令!\r\n");
                return;
            }
            command.doAction(ctx,inst);
        } catch (CommandException e) {
            e.printStackTrace();
        }
    }
}
