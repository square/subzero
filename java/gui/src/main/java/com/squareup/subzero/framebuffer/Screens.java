package com.squareup.subzero.framebuffer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Screens has methods for user interaction.  It handles the layout on screen and manages getting
 * input from the users.
 */
public class Screens {
  private Framebuffer framebuffer;
  private String teamName;

  // Margin we generally preserve at the edge of the screen.
  private static final int margin = 32;

  public Screens(Framebuffer framebuffer, String teamName) {
    this.framebuffer = framebuffer;
    this.teamName = teamName;
  }

  /**
   *  baseScreenLayout provides a base layout for consistency between screen layouts.
   *  It draws elements common to all screens, plus the provided elements, including a title at the
   *  top of the screen, a main instruction for the user.
   *
   *  The main screen content is then rendered by the caller on top of this, for the particular
   *  types of screen being displayed (prompt for input, action, QR scan, etc).
   *
   */
  private void baseScreenLayout(Graphics2D graphics, String title, String instructions, Color background, Boolean redactStdout) {
    // Clear with background color:
    graphics.setBackground(background);
    graphics.clearRect(0, 0, framebuffer.getWidth(), framebuffer.getHeight());

    // Title bar:  We draw the teamName and Title with a line below them.
    FontMetrics titleFontMetrics = Framebuffer.setFont(18, graphics);
    System.out.println(title);
    graphics.drawString(title, margin, margin);
    int rightAlign = framebuffer.getWidth() - (margin + titleFontMetrics.stringWidth(teamName));
    graphics.drawString(teamName, rightAlign, margin);

    int offset = margin/2 + titleFontMetrics.getHeight();
    graphics.fillRect(0, offset, framebuffer.getWidth(), 5);
    offset += 5;

    // Now we print the instructions in a slightly larger font
    FontMetrics metrics = Framebuffer.setFont(22, graphics);
    for(String line: instructions.split("\n")) {
      offset += metrics.getHeight();
      System.out.println(redactStdout ? "*****" : line);
      graphics.drawString(line, margin, offset);
    }
  }

  private void baseScreenLayout(Graphics2D graphics, String title, String instructions, Color background) {
    baseScreenLayout(graphics, title, instructions, background, false);
  }

  public void renderLoading() throws IOException {
    framebuffer.draw((Graphics2D graphics) ->
      baseScreenLayout(graphics,"Loading", "Please wait...", Color.white)
    );
    framebuffer.flip();
  }


  public boolean approveAction(String action) throws IOException {
    String instructions = action + "\n\nType exactly \"yes\" + <enter> to approve or \"no\" + <enter> to abort.";

    framebuffer.draw((Graphics2D graphics) -> {
      baseScreenLayout(graphics,"Approve", instructions, Color.white);
    });

    int offset = 320;
    while(true) {
      String input = framebuffer.prompt(18, offset, false);

      if (input.equalsIgnoreCase("yes")) {
        renderLoading();
        return true;
      }
      if (input.equalsIgnoreCase("no")) {
        renderLoading();
        return false;
      } else {
        framebuffer.text("That wasn't 'yes' or 'no'. Try again.", 18, offset);
      }
    }
  }

  /**
   * Prompt for a QR code
   * @return the data from a scanned QR Code (or just typed in...)
   */
  public String readQRCode() throws IOException {
    String instructions = "Please scan the printed QR-Code using the red scanner";

    framebuffer.draw((Graphics2D graphics) ->
      baseScreenLayout(graphics,"Scan QR Code", instructions, Color.white)
    );

    return framebuffer.prompt(12, 250, false);
  }

  /**
   * Prompt to insert an Operator Card.  Returns once the card is inserted.
   *
   * @param message Message to display to the user
   */
  public void promptForOperatorCard(String message) throws IOException {
    framebuffer.draw((Graphics2D graphics) ->
      baseScreenLayout(graphics,"Insert Operator Card", message, Color.white)
    );

    // TODO: Wait on the Operator Card instead
    framebuffer.pressEnter();
  }

  /**
   * Prompt to insert an Administrator Card.  Returns once the card is inserted.
   *
   * @param message Message to display to the user
   */
  public void promptForAdministratorCard(String message) throws IOException {
    framebuffer.draw((Graphics2D graphics) ->
        baseScreenLayout(graphics,"Insert Administrator Card", message, Color.white)
    );

    // TODO: Wait on the Administrator Card instead
    framebuffer.pressEnter();
  }

  /**
   * Prompt to remove an Operator Card.  Returns once the card is removed.
   *
   * @param message Message to display to the user
   */
  public void removeOperatorCard(String message) throws IOException {
    framebuffer.draw((Graphics2D graphics) ->
      baseScreenLayout(graphics,"Remove Operator Card", message, Color.white)
    );

    // TODO: Wait on the Operator Card to be removed
    framebuffer.pressEnter();
    renderLoading();
  }

  /**
   * Prompt for a password
   * @return the password typed in
   */
  public String promptPassword(String message) throws IOException {
    framebuffer.draw((Graphics2D g) -> baseScreenLayout(g,"Operator Card Password", message, Color.white));

    return framebuffer.prompt(32, 250, true);
  }

  public String promptAcsPassword(String message) throws IOException {
    framebuffer.draw((Graphics2D g) -> baseScreenLayout(g,"Administrator Card Password", message, Color.white));

    return framebuffer.prompt(32, 250, true);
  }


  public void warnPasswordChange(String newPassword) throws IOException {
    String message = "Updating your Operator Card password.\n" +
        "Please record the following password. You will never see this password again. Hit <enter> to continue.\n\n" +
        newPassword;

    framebuffer.draw((Graphics2D g) -> baseScreenLayout(g,"Operator Card Password", message, Color.white, true));
    framebuffer.pressEnter();
  }

  public void promptPasswordChangeFailed() throws IOException {
    String message = "Failed to confirm password. Hit <enter> to start over.";
    framebuffer.draw((Graphics2D g) -> baseScreenLayout(g,"Operator Card Password", message, Color.white));

    framebuffer.pressEnter();
  }

  public String promptPasswordChange() throws IOException {
    return promptPassword("Please confirm the password");
  }

  public enum ExitOrRestart {
    Exit,
    Restart,
  }

  /**
   * Display a QR Code, and ask what to do next (exit or restart)
   * @param encodedData The data to put in the QR code
   * @return A string, either "exit" or "restart", from the user
   * @throws IOException
   * @throws WriterException
   */
  public ExitOrRestart displayQRCode(String encodedData) throws IOException {

    System.out.println("Displaying QR code. Data: " + encodedData);

    String message = "We are done. Please scan the following QR-code with the blue scanner.\n" +
        "Then type 'exit' + <enter> or 'restart' + <enter>.";

    framebuffer.draw((Graphics2D g) -> {
      baseScreenLayout(g, "QR Code Output", message, Color.white);

      // Draw the QR Code
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix matrix;
      try {
        matrix = qrCodeWriter.encode(encodedData, BarcodeFormat.QR_CODE, 1, 1);
      } catch (WriterException w) {
        throw new RuntimeException(w);
      }
      BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);

      // aim to have the QR code fill 50% of the screen height
      int scale = (framebuffer.getHeight() / 2) / img.getHeight();
      if (scale < 1) {
        scale = 1;
      }
      if (scale > 4) {
        scale = 4;
      }

      AffineTransform at = new AffineTransform();
      at.scale(scale, scale);
      AffineTransformOp scaleOp =
          new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

      g.drawImage(img, scaleOp,
          framebuffer.getWidth()/2 - (scale*img.getWidth())/2,
          framebuffer.getHeight()/2 - (scale*img.getHeight())/2);
    });

    int offset = framebuffer.getHeight() - 60;
    framebuffer.text("Type 'exit' or 'restart'", 18, offset);
    while (true) {
      String command = framebuffer.prompt(18, offset + 20, false);
      if (command.equalsIgnoreCase("restart")) {
        return ExitOrRestart.Restart;
      } else if (command.equalsIgnoreCase("exit")) {
        return ExitOrRestart.Exit;
      }
      framebuffer.text("That wasn't 'exit' or 'restart'. Try again.", 18, offset);
    }
  }

  /**
   * Displays an exception and waits for a user to hit enter.
   * @param exception The exception to draw
   */
  public void exception(Exception exception) throws IOException {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);

    String message = sw.toString() + "\nPress <enter> to continue";

    // Horrible things have happened color:  PINK.
    framebuffer.draw((Graphics2D g) -> baseScreenLayout(g,"Exception", message, Color.PINK));
    framebuffer.pressEnter();
  }
}

