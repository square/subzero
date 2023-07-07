package com.squareup.subzero.framebuffer;

import com.squareup.subzero.SubzeroConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FramebufferTest {
  private SubzeroConfig config = new SubzeroConfig();

  private void assertColor(File file, Color color, int pixels) throws IOException {
    byte[] data = Files.readAllBytes(file.toPath());
    for(int i = 0; i < 80 * 60; i++) {
      assertEquals((byte) color.getBlue(), data[(i * 4)]);
      assertEquals((byte) color.getGreen(), data[(i * 4) + 1]);
      assertEquals((byte) color.getRed(), data[(i * 4) + 2]);
      // ignore alpha at (i * 4) + 3
    }
  }

  /**
   * This test basically goes through them motions of drawing to the framebuffer to ensure
   * it has some basic functionality.
   * @throws IOException
   */
  @Test
  public void test() throws Exception {
    // Framebuffer supports writing to a regular file instead of /dev/fb0
    File file = File.createTempFile( "testoutput", "fb");
    file.deleteOnExit();
    config.framebuffer = file.getAbsolutePath();
    config.framebufferSize = "80,60";

    Framebuffer framebuffer = new Framebuffer(config);

    Color blue = new Color(14, 103, 247);
    Color green = new Color(54, 237, 100);

    // Draw some stuff to the framebuffer:
    framebuffer.draw((Graphics2D graphics) -> {
      graphics.setColor(blue);
      graphics.fillRect(0, 0, 80, 60);
    });

    framebuffer.flip();

    framebuffer.stopRefresh();  // Don't want background refresh to interfere

    // The framebuffer should now be all blue

    // Now we draw some green but don't flip yet
    framebuffer.draw((Graphics2D graphics) -> {
      graphics.setColor(green);
      graphics.fillRect(0, 0, 80, 60);
    });

    assertColor(file, blue, 80*60);

    // Now we flip, and that should make the green draw out.
    framebuffer.flip();

    assertColor(file, green, 80*60);
  }
}
