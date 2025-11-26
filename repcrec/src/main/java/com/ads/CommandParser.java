package com.ads;

/**
 * Parses command lines into Command objects.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
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

        String[] parts = line.split("\\s+");
        String commandType = parts[0];

        switch (commandType) {
            case "begin" -> {
                return new Command(CommandType.BEGIN, new String[]{parts[1]});
            }
            case "R" -> {
                return new Command(CommandType.READ, new String[]{parts[1], parts[2]});
            }
            case "W" -> {
                return new Command(CommandType.WRITE, new String[]{parts[1], parts[2], parts[3]});
            }
            case "dump" -> {
                return new Command(CommandType.DUMP, new String[]{});
            }
            case "end" -> {
                return new Command(CommandType.END, new String[]{parts[1]});
            }
            case "fail" -> {
                return new Command(CommandType.FAIL, new String[]{parts[1]});
            }
            case "recover" -> {
                return new Command(CommandType.RECOVER, new String[]{parts[1]});
            }
            default -> throw new IllegalArgumentException("Unknown command: " + commandType);
        }   
    }
}
