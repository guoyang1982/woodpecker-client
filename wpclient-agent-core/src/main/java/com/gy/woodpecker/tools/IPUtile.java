package com.gy.woodpecker.tools;

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


}

