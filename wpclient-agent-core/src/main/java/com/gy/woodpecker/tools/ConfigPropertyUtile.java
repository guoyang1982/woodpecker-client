package com.gy.woodpecker.tools;

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
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }

    }
}
