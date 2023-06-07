package com.squareup.subzero;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SystemShutdownTest {
    @Test
    public void testSystemShutdownCommand(){
        String shutdownCommand = SystemShutdown.getShutdownCommand();
        if (System.getProperty("os.name").contains("Linux")) {
            // On linux, we expect to issue a shutdown command
            assertEquals("shutdown now", shutdownCommand);
        } else {
            // On non-linux, SystemShutdown is a NOP
            assertEquals("", shutdownCommand);
        }
    }
}
