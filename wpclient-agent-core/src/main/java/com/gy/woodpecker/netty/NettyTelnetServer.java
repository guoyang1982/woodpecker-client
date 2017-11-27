package com.gy.woodpecker.netty;

import com.gy.woodpecker.tools.ConfigPropertyUtile;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author guoyang
 * @Description: TODO
 */
public class NettyTelnetServer {
    // 指定端口号
    private static int PORT = 8888;
    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyTelnetServer(){
        String port = ConfigPropertyUtile.getProperties().getProperty("log.netty.server.port");
        if(null != port && !port.equals("")){
            PORT = Integer.parseInt(port);
        }
    }

    public void open() throws InterruptedException {

        serverBootstrap = new ServerBootstrap();
        // 指定socket的一些属性
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)  // 指定是一个NIO连接通道
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
                                new StringDecoder(), //
                                new StringEncoder(), //
                                new NettyTelnetHandler());
                    }
                });

        // 绑定对应的端口号,并启动开始监听端口上的连接
        Channel ch = serverBootstrap.bind(PORT).sync().channel();

        // 等待关闭,同步端口
        //ch.closeFuture().sync();
    }
    public void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
