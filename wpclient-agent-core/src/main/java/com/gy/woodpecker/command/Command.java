package com.gy.woodpecker.command;

import io.netty.channel.ChannelHandlerContext;

import java.lang.instrument.Instrumentation;


public interface Command {


    /**
     * 获取命令动作
     *
     * @return 返回命令所对应的命令动作
     */
    public void doAction(ChannelHandlerContext ctx,Instrumentation inst);
}
