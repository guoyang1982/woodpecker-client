package com.gy.woodpecker.netty;

import com.gy.woodpecker.log.LoggerFacility;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/11/16 上午10:54
 */
public class NettyTelnetHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection.
        ctx.write("欢迎来到啄木鸟控制端!\r\n");
        ctx.write("请输入控制命令.\r\n");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {

        String response;
        boolean close = false;
        if (request.isEmpty()) {
            response = "请输入命令.\r\n";
        }else if ("help".equals(request.toLowerCase())){
            response = "==================命令解释======================\r\n" +
                    "命令:m t\r\n监听发送日志消息线程池相关信息!\r\n命令:close hc\r\n关闭健康检查!\r\n命令:open hc\r\n打开健康检查!\r\n" +
                    "命令:close log\r\n关闭日志监控,不发送日志消息!\r\n命令:open log\r\n打开日志监控,发送日志消息!\r\n" +
                    "命令:log info\r\n获取应用日志类命令,可以过滤制定某些包下的类 如:log info com.!\r\n" +
                    "命令:log level 参数1 参数2\r\n设置应用类的日志级别,如 log level com. debug." +
                    "也可以设置root级别如 log level root debug,支持error warn info debug\r\n" +
                    "命令:help\r\n帮助命令!\r\n" +
                    "===============================================\r\n";
        } else if ("bye".equals(request.toLowerCase())) {
            response = "退出客户端!\r\n";
            close = true;
        } else if ("m t".equals(request.toLowerCase())){
            response = LoggerFacility.threadPoolsMonitor()+"\r\n";
        } else if ("close hc".equals(request.toLowerCase())){
            LoggerFacility.getInstall(null).telHealthCheck = false;
            response = "成功关闭健康检查!\r\n";
        } else if ("open hc".equals(request.toLowerCase())){
            LoggerFacility.getInstall(null).telHealthCheck = true;
            response = "成功打开健康检查!\r\n";
        } else if ("close log".equals(request.toLowerCase())){
            LoggerFacility.f = false;
            response = "成功关闭日志监控!\r\n";
        } else if ("open log".equals(request.toLowerCase())){
            LoggerFacility.f = true;
            response = "成功打开日志监控!\r\n";
        } else if (request.toLowerCase().startsWith("log info")){
            String[] loginfo = request.split(" ");
            String value = "";
            if(loginfo.length>2){
                value = loginfo[2];
            }
            response = LoggerFacility.getLogInfor(value)+"\r\n";
        } else if (request.toLowerCase().startsWith("log level ")){
            String[] loginfo = request.split(" ");
            String className = "";
            String level = "";
            if(loginfo.length == 4){
                className = loginfo[2];
                level = loginfo[3];
                //ERROR、WARN、INFO、DEBUG
                if(level.toLowerCase().equals("error") || level.toLowerCase().equals("warn") || level.toLowerCase().equals("info") || level.toLowerCase().equals("debug")){
                    LoggerFacility.setLogLevel(className,level);
                    response = "设置日志成功!\r\n";
                }else{
                    response = "您输入的命令参数错误!\r\n";
                }
            }else{
                response = "您输入的命令参数错误!\r\n";
            }
        }else{
            response = "您输入的命令不存在 '" + request + "'!\r\n";
        }

        ChannelFuture future = ctx.write(response);
        ctx.flush();
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
