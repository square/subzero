package com.squareup.subzero.framebuffer;

import com.google.common.base.Strings;
import com.squareup.subzero.SubzeroGui;
import com.squareup.subzero.SubzeroConfig;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Framebuffer is a class for drawing images to Linux framebuffer.
 * You must provide it with the path to the framebuffer and resolution
 * It only supports BGRA 32 bits per pixel, which is what every Linux framebuffer I've found to be
 * running.  Which is only like four computers, mind you.
 */
public class Framebuffer {
  /* backBuffer is what we're about to draw */
  private BufferedImage backbuffer;
  /* framebuffer is an open handle to the output device */
  private RandomAccessFile framebuffer;
  private int width;
  private int height;
  private int bytesPerPixel;
  private Refresh refreshThread;
  private SubzeroConfig config;
  Canvas canvas;
  private AtomicReference<Integer> asyncInput = new AtomicReference<>();
  private static Map<Integer, Font> fontCache = new HashMap<>();

  /**
   * Open a framebuffer for reading and writing.
   * @param config
   * @throws IOException Exceptions from opening and mapping the framebuffer.
   */
  public Framebuffer(SubzeroConfig config) throws IOException {
    this.config = config;
    readFramebufferSize();

    if (config.window) {
      createWindow();
    }

    // TODO: Support something other than 32 bpp BGRA.
    this.bytesPerPixel = 4;

    this.backbuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    this.framebuffer = new RandomAccessFile(Paths.get(config.framebuffer).toFile(), "rw");

    // Spawn a thread which redraws the screen every second.
    // This is needed in case something else draws to the screen, the computer goes to sleep
    // and blanks the display, etc.
    refreshThread = new Refresh(this);
    refreshThread.start();
  }

  private void createWindow() {
    // When running in Intellij, AWT thinks we are headless.
    System.setProperty("java.awt.headless", "false");

    // Create and set up the window.
    Frame frame = new Frame("Subzero");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    canvas = new Canvas();
    Dimension dimension = new Dimension(width, height);
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    frame.add(canvas);
    frame.pack();
    frame.setResizable(false);
    frame.setVisible(true);

    // Keyboard input in a GUI toolkit is async. So we do some ugly hackery to simulate the console.
    KeyEventDispatcher keyEventDispatcher = e -> {
      if (e.getID() == KeyEvent.KEY_TYPED) {
        asyncInput.set((int)e.getKeyChar());
      }
      return false;
    };
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);
  }

  private void readFramebufferSize() throws IOException {
    String metadata = config.framebufferSize;
    String[] sizes = metadata.split(",", 2);
    if (sizes.length == 2) {
      width = Integer.valueOf(sizes[0]);
      height = Integer.valueOf(sizes[1]);
    }
  }

  private class Refresh extends Thread {
    private Framebuffer framebuffer;

    Refresh(Framebuffer framebuffer) {
      this.framebuffer = framebuffer;
    }

    public void run() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // Exit this thread when we get interrupted.
        return;
      }
      try {
        framebuffer.flip();
      } catch (IOException exception) {
        // We had some kind of exception writing to the framebuffer.
        // Log and continue.  If the framebuffer is really broken, the program will exit.
        System.out.println("Error drawing framebuffer from background");
        System.out.println(exception.toString());
      }
    }
  }

  /**
   *  This method takes a lambda which can draw on the framebuffer via the provided
   *  Graphics2D object.
   *
   *  It is synchronized so drawing does not race with flip() drawing the backbuffer to the
   *  framebuffer.
   */
  synchronized void draw(Consumer<Graphics2D> a) {
    Graphics2D graphics = this.backbuffer.createGraphics();
    a.accept(graphics);
  }

  /**
   * Write this.backbuffer into this.framebuffer.  Call this to update what is displayed.
   *
   */
  public synchronized void flip() throws IOException {
    // Ideally we'd just mmap the framebuffer, but Java doesn't properly
    // mmaping character devices because it tries to resize them.
    byte[] newBuf = new byte[width * height * bytesPerPixel];
    int index = 0;
    for(int y = 0; y < backbuffer.getHeight(); y++) {
      for(int x = 0; x < backbuffer.getWidth(); x++) {
        Color color = new Color(backbuffer.getRGB(x, y));
        newBuf[index++] = (byte)color.getBlue();
        newBuf[index++] = (byte)color.getGreen();
        newBuf[index++] = (byte)color.getRed();
        // The fourth value here is alpha, which we leave unchanged at zero
        ++index;
      }
    }
    framebuffer.seek(0);
    framebuffer.write(newBuf);

    if (canvas != null) {
      BufferStrategy bufferStrategy = canvas.getBufferStrategy();
      if (bufferStrategy == null) {
        canvas.createBufferStrategy(2);
      }
      canvas.getGraphics().drawImage(backbuffer, 0, 0, null);
    }
  }

  public synchronized void stopRefresh() {
    refreshThread.interrupt();
  }

  /**
   * Draws one line of text, centered, with the top of the text at yOffset
   *
   * @param line The text to draw.  Should fit on a line.
   * @param size Font size.
   * @param yOffset How far down the framebuffer to start the text
   */
  void text(String line, int size, int yOffset) {
    draw((Graphics2D graphics) -> {
      FontMetrics fontMetrics = setFont(size, graphics);

      int offset = yOffset;
      int width = fontMetrics.stringWidth(line);
      Rectangle2D bounds = fontMetrics.getStringBounds(line, graphics);
      int x = (this.width - width) / 2;
      graphics.setColor(Color.white);
      // We add an extra pixel on each side (the -1 and +2) because antialiasing can leak out
      graphics.fillRect(0, offset - 1, this.getWidth(), (int) bounds.getHeight() + 2);
      graphics.setColor(Color.black);
      offset += fontMetrics.getAscent();
      graphics.drawString(line, x, offset);
    });
  }

  void ltext(String line, int size, int yOffset) {
    draw((Graphics2D graphics) -> {
      FontMetrics fontMetrics = setFont(size, graphics);

      int offset = yOffset;
      Rectangle2D bounds = fontMetrics.getStringBounds(line, graphics);
      graphics.setColor(Color.white);
      // We add an extra pixel on each side (the -1 and +2) because antialiasing can leak out
      graphics.fillRect(0, offset - 1, this.getWidth(), (int) bounds.getHeight() + 2);
      graphics.setColor(Color.black);
      offset += fontMetrics.getAscent();
      graphics.drawString(line, 32, offset);
    });
  }

  /**
   * A small static helper function to set the font to a given size.
   *
   * @param size Size to set to
   * @param graphics Graphics2D object to have an effect on
   */
  public static FontMetrics setFont(int size, Graphics2D graphics) {
    Font font = null;
    if (fontCache.containsKey(size)) {
      font = fontCache.get(size);
    } else {
      // Load SqMarket font
      try {
        ClassLoader classLoader = SubzeroGui.class.getClassLoader();
        URL resource = classLoader.getResource("SQMarket-Regular.otf");
        if (resource != null) {
          font = Font.createFont(Font.TRUETYPE_FONT, resource.openStream())
              .deriveFont(Font.PLAIN, size);
        }
      } catch (FontFormatException | IOException e) {
        // empty on purpose
      }
      if (font == null) {
        font = new Font("Arial", Font.BOLD, size);
      }
      fontCache.put(size, font);
    }

    graphics.setFont(font);
    graphics.setPaint(Color.black);
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

    return graphics.getFontMetrics();
  }

  // Supporting either newline or carriage returns makes it easier to be robust to terminal settings
  private static final int NEWLINE = 13;
  private static final int CARRIAGE = 10;
  private static final int SPACE = 32;
  private static final int BACKSPACE = 8;
  private static final int DELETE = 127;

  /**
   * Prompt for a string
   *
   * @param size font size, like framebuffer.text()
   * @param yOffset Y offset, like framebuffer.text()
   * @param password Should I echo or use * to show input?
   * @return The string the user typed
   * @throws IOException From reading input
   */
  public String prompt(int size, int yOffset, boolean password) throws IOException {
    if (password) {
      this.flip(); // draw prompt to the framebuffer
      return new String(System.console().readPassword());
    }
    String input = "";
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    while(true) {
      if (!br.ready()) {
        this.flip();  // Draw when there's no character to process.
      }
      Integer read = null;
      while (read == null) {
        if (br.ready()) {
          read = br.read();
          if (read == -1) {
            throw new IOException("Unexpected EOF waiting for input");
          }
        } else {
          read = asyncInput.getAndSet(null);
        }
      }
      // Backspace.
      if ((read == BACKSPACE) || (read == DELETE)) {
        if (input.length() > 0) {
          input = input.substring(0, input.length() - 1);
        }
      } else if (read == NEWLINE || read == CARRIAGE) {
        // Strip whitespace and return if the buffer is nonempty.
        input = input.replaceAll("\\s+", "");
        if (input.length() > 0) {
          return input;
        }
      } else if (read < SPACE || read > DELETE) {
        // Everything below space or above BACKSPACE is a control character
        // or otherwise funny business that we don't want in our input string.
        input += "?";
      } else {
        input += (char) read.intValue();
      }

      String output;
      output = input;
      if (output.length() > 100) {
        ltext("...".concat(output.substring(output.length() - 100)), size, yOffset);
      } else {
        ltext(output, size, yOffset);
      }
    }
  }

  /**
   * Reads until you hit enter.
   * @throws IOException From reading input
   */
  public void pressEnter() throws IOException {
    flip();
    while(true) {
      int read = System.in.read();
      if (read == -1) {
        throw new IOException("Unexpected EOF waiting for input");
      } else if (read == NEWLINE || read == CARRIAGE) {
        return;
      }
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
