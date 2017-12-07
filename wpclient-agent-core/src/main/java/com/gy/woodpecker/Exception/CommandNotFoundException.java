package com.gy.woodpecker.Exception;

/**
 * 命令不存在
 */
public class CommandNotFoundException extends CommandException {

    public CommandNotFoundException(String command) {
        super(command);
    }

}
