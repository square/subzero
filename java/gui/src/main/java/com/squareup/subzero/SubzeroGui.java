package com.squareup.subzero;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.protobuf.TextFormat;
import com.squareup.subzero.framebuffer.Framebuffer;
import com.squareup.subzero.framebuffer.Screens;
import com.squareup.subzero.ncipher.NCipher;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import com.squareup.subzero.shared.SubzeroUtils;

import static com.google.common.io.BaseEncoding.base64;

public class SubzeroGui {
  @Parameter(names = "--help", help = true)
  private boolean help = false;

  @Parameter(names = "--config")
  private String configFile;

  @Parameter(names = "--init-nvram")
  private boolean initNvram = false;

  @Parameter(names = "--debug") public String debug = null;

  // UI test runs through all the screens without needing an HSM or Subzero server
  @Parameter(names = "--uitest") public Boolean uiTest = false;

  @Parameter(names = "--ncipher") public Boolean nCipher = false;

  // If missing or incorrect, will prompt for a password on stdin.
  @Parameter(names = "--ocs-password") public String ocsPassword;

  // By default, subzero listens on this port. This port was picked randomly. We don't have to care
  // about port conflicts since we also build the Linux image.
  @Parameter(names = "--port") public int port = 32366;

  // Almost always you want to talk to subzero on localhost
  @Parameter(names = "--hostname") public String hostname = "localhost";

  public SubzeroConfig config;
  private Screens screens;

  /**
   * We pass the cli object into functions which can use it to draw screens.
   * If null, you're running in debug mode and screens should probably use text instead.
   *
   * @return a screens object to interact with the user
   */
  public Screens getScreens() {
    return screens;
  }

  public static void main(String[] args) throws Exception {
    SubzeroGui subzero = new SubzeroGui();

    JCommander jCommander = JCommander.newBuilder()
        .addObject(subzero)
        .build();
    jCommander.setProgramName("Subzero");
    jCommander.parse(args);
    if (subzero.help) {
      jCommander.usage();
      return;
    }

    System.out.println("This program draws to a framebuffer. If you are only seeing this output,");
    System.out.println("then something has gone wrong. Please report this error.");

    subzero.config = SubzeroConfig.load(subzero.nCipher, subzero.configFile);

    if (subzero.uiTest) {
      subzero.uiTest();
    } else if (subzero.debug != null) {
      subzero.debugMode();
    } else {
      subzero.interactive();
    }
  }

  private void debugMode() throws Exception {
    byte[] rawCmd = base64().decode(debug);
    CommandRequest commandRequest = CommandRequest.parseFrom(rawCmd);

    InternalCommandConnector conn = new
        InternalCommandConnector(hostname, port);
    CommandResponse commandResponse = CommandHandler.dispatch(this, conn, commandRequest);
    String response = base64().encode(commandResponse.toByteArray());

    // Pretty print the response
    String debugString = TextFormat.shortDebugString(commandResponse);
    System.out.println(debugString);

    // The response is what the server will receive via QR-Code.
    SubzeroUtils.printQrCode(response);
    System.out.println(response);
  }

  private void interactive() throws Exception {
    screens = new Screens(new Framebuffer(config), config.teamName);

    try {
      if (nCipher) {
        NCipher nCipher = new NCipher();
        nCipher.healthCheck();

        if (initNvram) {
          nCipher.initNvram(config.dataSignerKey, screens);
        }
      }

      while (true) {
        String input = screens.readQRCode();

        byte[] proto = base64().decode(input);

        CommandRequest commandRequest = CommandRequest.parseFrom(proto);

        CommandResponse response =
            CommandHandler.dispatch(this, new InternalCommandConnector(hostname, port), commandRequest);
        System.out.println(response.toString());

        String encoded = base64().encode(response.toByteArray());

        Screens.ExitOrRestart command = screens.displayQRCode(encoded);
        if (command == Screens.ExitOrRestart.Exit) {
          return;
        }
      }
    } catch (Exception e) {
      screens.exception(e);
    }
  }

  /**
   * This goes through the various screens, so you can test changes to them without needing to
   * worry about any system state, run Subzero, etc.
   */
  private void uiTest() throws Exception {
    screens = new Screens(new Framebuffer(config), config.teamName);

    try {
      while (true) {
        String input = screens.readQRCode();

        if (!screens.approveAction(
            "You are trying to transfer 10000 btc to hackers. Sounds cool?")) {
          System.out.println("Rejected!");
          return;
        }

        screens.promptForOperatorCard("Please insert Operator Card and then press enter");

        String passwordPrompt = "Please type your Operator Card password";
        while (true) {
          String password = screens.promptPassword(passwordPrompt);
          if (password.equals("ponies")) {
            break;
          }
          passwordPrompt = "Incorrect. Please type your Operator Card password";
        }

        screens.removeOperatorCard("Please remove Operator card and then hit <enter>.");

        // Please wait screen should now be displayed
        Thread.sleep(3000);

        // Generate a big QR code:
        String big = new String(new char[1999]).replace("\0", "M");
        screens.displayQRCode(big); // return value ignored so exit doesn't work
        // reflect back the original scanned QR code:
        Screens.ExitOrRestart command = screens.displayQRCode(input);
        if (command == Screens.ExitOrRestart.Exit) {
          return;
        }
        // otherwise command was restart, and we loop.
      }
    } catch (Exception e) {
      screens.exception(e);
    }
  }
}
