package com.gy.woodpecker.tools;

/**
 * @author guoyang
 * @Description: TODO
 * @date 2018/6/22 下午3:56
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MsgPackUtil {

    private static ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());

    /**
     * @Title: toBytes
     * @Description: 对象转byte数组
     * @author Jecced
     * @param obj
     * @return
     */
    public static <T> byte[] toBytes(T obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Title: toList
     * @Description: byte转list集合
     * @author Jecced
     * @param bytes
     * @param clazz
     * @return
     */
    public static <T> List<T> toList(byte[] bytes, Class<T> clazz) {
        List<T> list = null;
        try {
            list = mapper.readValue(bytes, MsgPackUtil.List(clazz));
        } catch (IOException e) {
            list = new ArrayList<T>();
            e.printStackTrace();
        }
        return list;
    }

    /**
     * @Title: toObject
     * @Description: byte转指定对象
     * @author Jecced
     * @param bytes
     * @param clazz
     * @return
     */
    public static <T> T toObject(byte[] bytes, Class<T> clazz) {
        T value = null;
        try {
            value = mapper.readValue(bytes, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * @Title: List
     * @Description: 私有方法,获取泛型的TypeReference
     * @author Jecced
     * @param clazz
     * @return
     */
    private static <T> JavaType List(Class<?> clazz) {
        return mapper.getTypeFactory().constructParametricType(ArrayList.class, clazz);
    }

}
