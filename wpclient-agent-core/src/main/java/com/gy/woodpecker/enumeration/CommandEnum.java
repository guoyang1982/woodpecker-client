package com.gy.woodpecker.enumeration;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/18 下午4:56
 */
public enum CommandEnum {
    TRACE("trace"),PRINT("print"),OTHER("other");
    String type;
    private CommandEnum(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
}
