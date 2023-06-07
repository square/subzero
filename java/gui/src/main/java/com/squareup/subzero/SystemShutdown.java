package com.squareup.subzero;


public class SystemShutdown {
    /**
     * Initiate a graceful shutdown of the machine(Linux only, NOP for others(Mac)).
     */
    public static void systemShutdown() throws Exception{
        String command = getShutdownCommand();
        if (command.length() > 0) {
            Runtime.getRuntime().exec(command);
        }
    }

    public static String getShutdownCommand() {
        if (System.getProperty("os.name").contains("Linux")) {
            return "shutdown now";
        } else {
            return "";
        }
    }
}
