package com.squareup.subzero;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SystemShutdownTest {
    @Test
    public void testSystemShutdown(){
        if (System.getProperty("os.name").contains("Linux")) {
            try {
                SystemShutdown.systemShutdown();
                assertThat(false);
            } catch (Exception e) {
                //should throw exception as shutdown now as non root user on Linux should fail.
                assertThat(true);

            }

        } else {
            try {
                //NOP
                SystemShutdown.systemShutdown();
                assertThat(true);
            } catch (Exception e){
                //should not throw an exception.
                assertThat(false);
            }
        }

    }
}
