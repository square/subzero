package com.squareup.subzero;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemShutdown {
    /**
     * Initiate a graceful shutdown of the machine(Linux only, NOP for others(Mac)).
     */
    public static void systemShutdown() throws Exception{

        if (System.getProperty("os.name").contains("Linux")) {
            Runtime.getRuntime().exec("shutdown now");
        }
        return;
    }
}
