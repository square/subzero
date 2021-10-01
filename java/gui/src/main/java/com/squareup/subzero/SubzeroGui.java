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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.List;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

  // Blackbox signTx Test
  @Parameter(names = "--signtx-test") public Boolean signtxTest = false;

  public SubzeroConfig config;
  private Screens screens;

  /**
   * We pass the cli object into functions which can use it to draw screens. If null, you're running
   * in debug mode and screens should probably use text instead.
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

    if (subzero.signtxTest) {
      System.out.println(
          "Transaction signing regression test. Please make sure subzero core is up and running.");
    } else {
      System.out.println(
          "This program draws to a framebuffer. If you are only seeing this output,");
      System.out.println("then something has gone wrong. Please report this error.");
    }

    subzero.config = SubzeroConfig.load(subzero.nCipher, subzero.configFile);

    if (subzero.uiTest) {
      subzero.uiTest();
    } else if (subzero.signtxTest) {
      subzero.signTxTest();
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
            CommandHandler.dispatch(this, new InternalCommandConnector(hostname, port),
                commandRequest);
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
   * This goes through the various screens, so you can test changes to them without needing to worry
   * about any system state, run Subzero, etc.
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

  private static class IllegalTxSignTestFileFormatException extends Exception {
    public IllegalTxSignTestFileFormatException(String msg) {
      super(msg);
    }
  }

  private void signTxTest() throws Exception {

    // Passed and failed test cases, for valid and invalid test vectors.
    // For a valid test vector, test passes (ok) if and only if subzero response matches
    // expected response. This is for happy path testing.
    // For an invalid test vector, test passes (ok) if and only if subzero response does not
    // match expected response. This is for sad path testing.
    int ok_valid = 0;
    int fail_valid = 0;
    int ok_invalid = 0;
    int fail_invalid = 0;

    // Read request & expected response from src/main/resources/txsign-testvectors/.
    // The request and expected response are pre-generated, based64-encoded proto buffers.
    // These test vectors are the same for TARGET=dev and TARGET=nCipher subzero core build types
    FileResourceUtils util = new FileResourceUtils();
    List<Path> txsignRegressionTestsPath = util.getPathsFromResourcesJAR("txsign-testvectors");
    for (Path path : txsignRegressionTestsPath) {
      String filePathInJAR = path.toString();
      if (filePathInJAR.startsWith("/")) {
        filePathInJAR = filePathInJAR.substring(1, filePathInJAR.length());
      }

      try {
        InputStreamReader inStreamReader =
            new InputStreamReader(util.getFileFromResourceAsStream(filePathInJAR),
                StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inStreamReader);

        // Get input from file
        String request = reader.readLine();
        if (request == null || !request.startsWith("request:")) {
          throw new IllegalTxSignTestFileFormatException(
              path.toString() + ": malformed file, request not found");
        }

        // Get expected output from file
        String expectedResponse = reader.readLine();
        if (expectedResponse == null || !expectedResponse.startsWith("response:")) {
          throw new IllegalTxSignTestFileFormatException(
              filePathInJAR + ": malformed file, response not found");
        }
        request = request.substring("request:".length(), request.length());
        expectedResponse =
            expectedResponse.substring("response:".length(), expectedResponse.length());

        byte[] proto = base64().decode(request);

        CommandRequest commandRequest = CommandRequest.parseFrom(proto);
        // If the filename for the test vector has a "negative" then it's
        // expected that subzero core will return an error code.
        // These test vectors are handcrafted. Like the `bad_qr_signature` one was crafted
        // by temporarily changing the Server code in `GenerateQrCodeResource.java`.
        if (filePathInJAR.contains("negative")) {
            try {
                CommandResponse response =
                        CommandHandler.dispatch(this, new InternalCommandConnector(hostname, port),
                                commandRequest);
            } catch (Exception e) {
                // A list of negative test cases can be added here.
                if (
                        filePathInJAR.contains("bad_qrsignature") && e.toString().contains("QRSIG_CHECK_FAILED") ||
                                filePathInJAR.contains("required_fields_not_present") && e.toString().contains("REQUIRED_FIELDS_NOT_PRESENT")
                ) {
                    System.out.println("testcase " + filePathInJAR + " : OK");
                    ok_valid++;
                } else {
                    System.out.println("testcase " + filePathInJAR + " : FAIL");
                    fail_valid++;
                }
                continue;
            }
            fail_valid++;
            System.out.println("testcase " + filePathInJAR + " : FAIL");
            continue;

        }
          CommandResponse response =
            CommandHandler.dispatch(this, new InternalCommandConnector(hostname, port),
                commandRequest);

        String encoded = base64().encode(response.toByteArray());

        if (encoded.equals(expectedResponse)) {
          if (path.getFileName().toString().startsWith("valid-")) {
            System.out.println("testcase " + filePathInJAR + " : OK");
            ok_valid++;
          } else if (path.getFileName().toString().startsWith("invalid-")) {
            System.out.println("testcase " + filePathInJAR + " : FAIL");
            fail_invalid++;
          }
        } else {
          if (path.getFileName().toString().startsWith("valid-")) {
            System.out.println("testcase " + filePathInJAR + " : FAIL");
            fail_valid++;
          } else if (path.getFileName().toString().startsWith("invalid-")) {
            System.out.println("testcase " + filePathInJAR + " : OK");
            ok_invalid++;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println("-------------------------------------------");
    System.out.printf("positive (valid) test cases: %d, passed %d, failed %d%n",
        (ok_valid + fail_valid), ok_valid, fail_valid);
    System.out.printf("negative (invalid) test cases: %d, passed %d, failed %d%n",
        (ok_invalid + fail_invalid), ok_invalid, fail_invalid);
    System.out.printf("test cases total: %d, passed %d, failed %d%n",
        (ok_valid + ok_invalid + fail_valid + fail_invalid), (ok_valid + ok_invalid),
        (fail_valid + fail_invalid));
    System.out.println("-------------------------------------------");
    if (fail_valid + fail_invalid == 0) {
      System.out.println("ALL TESTS PASSED");
    } else {
      System.out.println("SOME TESTS FAILED");
    }
  }
}
