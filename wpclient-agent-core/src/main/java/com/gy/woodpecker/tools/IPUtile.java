package com.gy.woodpecker.tools;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;

public class IPUtile {

    /**
     * 获取内网ip
     * @return
     */
    public static String getIntranetIP()
    {
        Enumeration allNetInterfaces = null;
        try
        {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress ip = null;
        while (allNetInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
            Enumeration addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                ip = (InetAddress) addresses.nextElement();
                if (ip != null && ip instanceof Inet4Address) {
                    String netIP = ip.getHostAddress();
                    if (netIP.startsWith("10.") || netIP.startsWith("172.") || netIP.startsWith("192.")) {
                        return netIP;
                    }
                }
            }
        }
        return "127.0.0.1";
    }

    public static String getIP() {
        String infor;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            infor = addr.getHostAddress().toString();
            if(infor != null && !infor.equals("127.0.0.1")) {
                return infor;
            }
        } catch (UnknownHostException var15) {
            var15.printStackTrace();
        }

        infor = null;
        BufferedReader bufferedReader = null;
        Process process = null;

        String ip;
        try {
            process = Runtime.getRuntime().exec("ifconfig eth0");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            ip = null;
            boolean ips = true;

            while((ip = bufferedReader.readLine()) != null) {
                int ips1 = ip.toLowerCase().indexOf("inet addr");
                if(ips1 >= 0) {
                    infor = ip.substring(ips1 + "inet addr".length() + 1);
                    break;
                }
            }
        } catch (IOException var16) {
            var16.printStackTrace();
        } finally {
            try {
                if(bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException var14) {
                var14.printStackTrace();
            }

            bufferedReader = null;
            process = null;
        }

        ip = "";
        if(infor != null && !infor.equals("")) {
            String[] ips2 = infor.split(" ");
            ip = ips2[0];
        }

        return ip != null && !ip.equals("")?ip.trim():getBondIp();
    }


    public static String getBondIp() {
        String infor = null;
        BufferedReader bufferedReader = null;
        Process process = null;

        String ip;
        try {
            process = Runtime.getRuntime().exec("ifconfig bond1");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            ip = null;
            boolean ips = true;

            while((ip = bufferedReader.readLine()) != null) {
                int ips1 = ip.toLowerCase().indexOf("inet addr");
                if(ips1 >= 0) {
                    infor = ip.substring(ips1 + "inet addr".length() + 1);
                    break;
                }
            }
        } catch (IOException var16) {
            var16.printStackTrace();
        } finally {
            try {
                if(bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException var14) {
                var14.printStackTrace();
            }

            bufferedReader = null;
            process = null;
        }

        ip = "";
        if(infor != null && !infor.equals("")) {
            String[] ips2 = infor.split(" ");
            ip = ips2[0];
        }
        return ip != null && !ip.equals("")?ip.trim():"127.0.0.1";
    }

    /**
     * 判断ip是否在ip网段中
     * @param ip
     * @param cidr ip 10.2.3.5或者ip网段 10.2.3.0/24
     * @return
     */
    public static boolean isInRange(String ip, String cidr) {
        if (StringUtils.isEmpty(cidr)) {
            return true;
        }
        if (cidr.indexOf("/") < 0 && cidr.equals(ip)) {
            return true;
        }
        if (cidr.indexOf("/") < 0 && !cidr.equals(ip)) {
            return false;
        }
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
        int type = Integer.parseInt(cidr.replaceAll(".*/", ""));
        int mask = 0xFFFFFFFF << (32 - type);
        String cidrIp = cidr.replaceAll("/.*", "");
        String[] cidrIps = cidrIp.split("\\.");
        int cidrIpAddr = (Integer.parseInt(cidrIps[0]) << 24)
                | (Integer.parseInt(cidrIps[1]) << 16)
                | (Integer.parseInt(cidrIps[2]) << 8)
                | Integer.parseInt(cidrIps[3]);

        return (ipAddr & mask) == (cidrIpAddr & mask);
    }
}

