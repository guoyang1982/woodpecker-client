package com.gy.woodpecker.tools;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import static java.lang.Integer.toHexString;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 字符串工具类
 * Created by oldmanpushcart@gmail.com on 15/5/18.
 */
public class GaStringUtils {

    /**
     * 命令提示符
     */
    public static final String DEFAULT_PROMPT = "ga?>";

    /**
     * 中断提示
     */
    public static final String ABORT_MSG = "Press Ctrl+D to abort.";

    /**
     * Spy类名
     */
    public static final String SPY_CLASSNAME = "com.github.ompc.greys.agent.Spy";

    /**
     * 解析状态
     */
    private enum SPLIT_FOR_ARGUMENT_STATE {
        ESCAPE_CHAR,
        READ_CHAR
    }

    /**
     * 拆分参数，要求能将命令行字符串拆分成为多个数组
     *
     * @param argumentString 参数行
     * @return 拆分后的参数数组
     */
    public static String[] splitForArgument(String argumentString) {

        final ArrayList<String> stringList = new ArrayList<String>();

        if (StringUtils.isNotBlank(argumentString)) {

            // 字符串片段
            StringBuilder segmentSB = new StringBuilder();

            // 解析状态
            SPLIT_FOR_ARGUMENT_STATE state = SPLIT_FOR_ARGUMENT_STATE.READ_CHAR;

            // 期待片段分隔符
            char splitSegmentChar = ' ';

            // 是否在片段中
            boolean isInSegment = true;


            for (char c : argumentString.toCharArray()) {

                switch (state) {

                    case READ_CHAR: {

                        // 匹配到转义符时，任何时候都跳转到转义处理
                        if (GaCheckUtils.isEquals(c, '\\')) {
                            state = SPLIT_FOR_ARGUMENT_STATE.ESCAPE_CHAR;
                            break;
                        }

                        // 段落中的匹配
                        if (isInSegment) {

                            // 如果匹配到片段结束分隔符，则结束当前片段
                            if (GaCheckUtils.isEquals(c, splitSegmentChar)) {
                                final String segmentString = segmentSB.toString();
                                if (StringUtils.isNotBlank(segmentString)) {
                                    stringList.add(segmentString);
                                }
                                segmentSB = new StringBuilder();
                                isInSegment = false;
                            }

                            // 其他情况则一律添加到片段中
                            else {
                                segmentSB.append(c);
                            }

                        }

                        // 非段落中的匹配
                        else {

                            // 过滤掉连续空格
                            if (GaCheckUtils.isEquals(c, ' ')) {
                                break;
                            }

                            // 命中片段分隔符
                            else if (GaCheckUtils.isIn(c, '\'', '"')) {
                                splitSegmentChar = c;
                                isInSegment = true;
                            }

                            // 其他字符则一律添加到片段中
                            // 同时认为以空格作为片段分隔符
                            else {
                                splitSegmentChar = ' ';
                                isInSegment = true;
                                segmentSB.append(c);
                            }

                        }

                        break;
                    }

                    case ESCAPE_CHAR: {
                        segmentSB.append(c);
                        state = SPLIT_FOR_ARGUMENT_STATE.READ_CHAR;
                        break;
                    }
                }//switch

            }//for

            // 最后循环结束需要强制提交片段
            final String segmentString = segmentSB.toString();
            if (StringUtils.isNotBlank(segmentString)) {
                stringList.add(segmentString);
            }
        }

        return stringList.toArray(new String[stringList.size()]);

    }


}

