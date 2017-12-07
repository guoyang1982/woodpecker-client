package com.gy.woodpecker.Exception;

/**
 * 命令执行错误
 * Created by oldmanpushcart@gmail.com on 15/5/2.
 */
public class CommandException extends Exception {

    private final String command;


    public CommandException(String command) {
        this.command = command;
    }

    public CommandException(String command, Throwable cause) {
        super(cause);
        this.command = command;
    }

    /**
     * 获取出错命令
     *
     * @return 命令名称
     */
    public String getCommand() {
        return command;
    }

}
