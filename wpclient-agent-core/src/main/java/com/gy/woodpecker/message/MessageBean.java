package com.gy.woodpecker.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageBean implements Serializable{

    /**
     * the name of application
     */
    private String appName ;

    /**
     * the host of application
     */
    private String ip;

    /**
     * the error message of the application
     */
    private String msg;


    /**
     * the time of error occurrence
     */
    private String createTime;
}
