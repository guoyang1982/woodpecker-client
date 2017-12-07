package com.gy.woodpecker.Exception;

/**
 * 命令初始化出错
 */
public class CommandInitializationException extends CommandException {

    public CommandInitializationException(String command, Throwable cause) {
        super(command, cause);
    }

}
