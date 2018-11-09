package com.gy.woodpecker.tools;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author guoyang
 * @Description: 获取配置文件信息
 */
public class ConfigPropertyUtile {
    private static Properties properties;

    public static Properties getProperties(){
        return properties;
    }

    public  static void initProperties(String propertiesFileName) {
        if(null == properties){
            properties = new Properties();
            InputStream input = null;

            if (null != propertiesFileName && !propertiesFileName.equals("")) {
                try {
                    input = new FileInputStream(new File(propertiesFileName));
                    properties.load(input);
                    //获取jvm参数，如果存在对应的参数则以jvm参数为准
                    String applicationName = System.getProperty("woodpecker.applicationName");
                    String consolePort = System.getProperty("woodpecker.consolePort");

                    if(StringUtils.isNotBlank(applicationName)){
                        properties.setProperty("application.name",applicationName);

                    }
                    if(StringUtils.isNotBlank(consolePort)){
                        properties.setProperty("log.netty.server.port",consolePort);

                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }

    }
}
