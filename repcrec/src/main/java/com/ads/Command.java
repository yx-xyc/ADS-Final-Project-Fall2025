package com.ads;

/**
 * Class representing a command with its type and arguments.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-27)
 */

public class Command {
    private final CommandType type;
    private final String[] args;

    public Command(CommandType type, String[] args) {
        this.type = type;
        this.args = args;
    }

    public CommandType getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return type + "(" + String.join(",", args) + ")";
    }
}
