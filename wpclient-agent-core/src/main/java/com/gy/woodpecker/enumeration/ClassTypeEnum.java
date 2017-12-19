package com.gy.woodpecker.enumeration;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2017/12/19 下午1:48
 */
public enum ClassTypeEnum {
    INT("int"),LONG("long"),FLOAT("float"),DOUBLE("double"),OBJECT("object");
    String type;
    private ClassTypeEnum(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
}
