package com.ads;

/**
 * Parses command lines into Command objects.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-27)
 */

public class CommandParser {
    public Command parse(String line) throws IllegalArgumentException {
        // Remove comments and trim whitespace
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        line = line.trim();

        if (line.isEmpty()) {
            return null; // No command to process
        }

        final String commandType = line.substring(0, line.indexOf('('));
        final String[] argsArray = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim().split(",");

        switch (commandType) {
            case "begin" -> {
                return new Command(CommandType.BEGIN, argsArray);
            }
            case "R" -> {
                return new Command(CommandType.READ, argsArray);
            }
            case "W" -> {
                return new Command(CommandType.WRITE, argsArray);
            }
            case "dump" -> {
                return new Command(CommandType.DUMP, argsArray);
            }
            case "end" -> {
                return new Command(CommandType.END, argsArray);
            }
            case "fail" -> {
                return new Command(CommandType.FAIL, argsArray);
            }
            case "recover" -> {
                return new Command(CommandType.RECOVER, argsArray);
            }
            default -> throw new IllegalArgumentException("Unknown command: " + commandType);
        }   
    }
}
