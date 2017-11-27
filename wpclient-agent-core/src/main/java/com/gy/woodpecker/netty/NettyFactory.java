package com.gy.woodpecker.netty;

import com.gy.woodpecker.tools.ConfigPropertyUtile;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/11/24 下午3:03
 */
public class NettyFactory {

    public static void init(){
        String nettyS = ConfigPropertyUtile.getProperties().getProperty("log.netty.server");
        if(null == nettyS || nettyS.equals("")){
            nettyS = "true";
        }
        if(nettyS.equals("true")){
            NettyTelnetServer nettyTelnetServer = new NettyTelnetServer();
            try {
                nettyTelnetServer.open();
            } catch (InterruptedException e) {
                nettyTelnetServer.close();
            }
        }
    }
}
