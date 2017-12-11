package com.gy.woodpecker.handler;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/7 下午1:05
 */
public interface CommandHandler {

    void executeCommand(final String line, final ChannelHandlerContext ctx,Instrumentation inst,int sessionId) throws IOException;
}
