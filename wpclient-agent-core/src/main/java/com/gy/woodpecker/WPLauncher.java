package com.gy.woodpecker;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;


/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/19 下午5:52
 */
@Slf4j
public class WPLauncher {

    public static void main(String[] args){
        final OptionParser parser = new OptionParser();
        parser.accepts("pid").withRequiredArg().ofType(String.class).required();
        parser.accepts("config").withOptionalArg().ofType(String.class).required();
        final OptionSet os = parser.parse(args);

        String pid = (String) os.valueOf("pid");
        String config = (String) os.valueOf("config");

        String agentPath = WPLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        agentPath = agentPath.replace("wpclient-agent-core.jar","wpclient-agent.jar");
        //String vid = args[0];
        try{
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            //final Class<?> vmdClass = loader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
            final Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");
            Object vmObject = null;
            vmObject = vmClass.getMethod("attach",String.class).invoke(null,pid);

            vmClass.getMethod("loadAgent",String.class,String.class).invoke(vmObject,agentPath,agentPath.replace("wpclient-agent.jar",config));

        }catch (Exception e){
            log.error("launcher fail!",e);
        }
    }
}
