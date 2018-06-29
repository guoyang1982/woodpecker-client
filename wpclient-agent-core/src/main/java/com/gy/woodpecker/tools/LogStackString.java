package com.gy.woodpecker.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author guoyang
 * @Description: 日志输出栈
 * @date 2018/4/2 下午3:48
 */
public class LogStackString {

    public static String errInfo(Exception e) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            // 将出错的栈信息输出到printWriter中
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return sw.toString();
    }
}
