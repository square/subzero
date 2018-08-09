package com.squareup.plutus;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.protobuf.TextFormat;
import com.squareup.plutus.framebuffer.Framebuffer;
import com.squareup.plutus.framebuffer.Screens;
import com.squareup.plutus.ncipher.NCipher;
import com.squareup.plutus.shared.PlutusUtils;
import com.squareup.protos.plutus.service.Service.CommandRequest;
import com.squareup.protos.plutus.service.Service.CommandResponse;
import java.util.Base64;

public class PlutusCli {
  @Parameter(names = "--help", help = true)
  private boolean help = false;

  @Parameter(names = "--debug") public String debug = null;

  // UI test runs through all the screens without needing an HSM or Plutus server
  @Parameter(names = "--uitest") public Boolean uiTest = false;

  @Parameter(names = "--ncipher") public Boolean nCipher = false;

  // If missing or incorrect, will prompt for a password on stdin.
  @Parameter(names = "--ocs-password") public String ocsPassword;

  // By default, plutus listens on this port, which is allocated in Registry
  @Parameter(names = "--port") public int port = 32366;

  // Almost always you want to talk to plutus on localhost
  @Parameter(names = "--hostname") public String hostname = "localhost";

  public PlutusConfig config;
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
    PlutusCli plutus = new PlutusCli();
    plutus.config = PlutusConfig.load();

    JCommander jCommander = JCommander.newBuilder()
        .addObject(plutus)
        .build();
    jCommander.setProgramName("Plutus");
    jCommander.parse(args);
    if (plutus.help) {
      jCommander.usage();
      return;
    }

    System.out.println("This program draws to a framebuffer. If you are only seeing this output,");
    System.out.println("then something has gone wrong. Please report this error.");

    if (plutus.uiTest) {
      plutus.uiTest();
    } else if (plutus.debug != null) {
      plutus.debugMode();
    } else {
      plutus.interactive();
    }
  }

  private void debugMode() throws Exception {
    byte[] rawCmd = Base64.getDecoder().decode(debug);
    CommandRequest commandRequest = CommandRequest.parseFrom(rawCmd);

    InternalCommandConnector conn = new
        InternalCommandConnector(hostname, port);
    CommandResponse commandResponse = CommandHandler.dispatch(this, conn, commandRequest);
    String response = Base64.getEncoder().encodeToString(commandResponse.toByteArray());

    // Pretty print the response
    String debugString = TextFormat.shortDebugString(commandResponse);
    System.out.println(debugString);

    // The response is what the server will receive via QR-Code.
    PlutusUtils.printQrCode(response);
    System.out.println(response);
  }

  private void interactive() throws Exception {
    screens = new Screens(new Framebuffer(config), config.getTeamName());

    try {
      if (nCipher) {
        new NCipher().healthCheck();
      }

      while (true) {
        String input = screens.readQRCode();

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] proto = decoder.decode(input);

        CommandRequest commandRequest = CommandRequest.parseFrom(proto);

        CommandResponse response =
            CommandHandler.dispatch(this, new InternalCommandConnector(hostname, port), commandRequest);
        System.out.println(response.toString());

        String encoded = Base64.getEncoder().encodeToString(response.toByteArray());

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
   * worry about any system state, run Plutus, etc.
   */
  private void uiTest() throws Exception {
    screens = new Screens(new Framebuffer(config), config.getTeamName());

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
