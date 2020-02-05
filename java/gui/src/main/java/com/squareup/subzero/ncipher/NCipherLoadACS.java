package com.squareup.subzero.ncipher;

import com.ncipher.km.nfkm.CmdCallBack;
import com.ncipher.km.nfkm.Slot;
import com.squareup.subzero.framebuffer.Screens;
import java.io.IOException;

/**
 * Load existing ACS
 *
 * 1. Tell the user to insert their ACS.
 * 2. Prompt for the password.
 *
 * For ease of development purpose, the prompt can be overridden with a hardcoded password.
 */
public class NCipherLoadACS implements CmdCallBack {
  private Screens screens;
  private String passwordPrompt = "Enter Administrator Card password";

  public NCipherLoadACS(Screens screens) {
    this.screens = screens;
  }

  /**
   * Sample values for var1, var2 and var3:
   * "card", "DecryptFailed", "Error whilst trying to load the card: DecryptFailed"
   *
   * If we don't throw an exception, the underlying code will automatically prompt for the password
   * again.
   *
   * Return true to abort or false to prompt for the password again.
   */
  public boolean errorCallBack(String reason, String code, String message) {
    if (code.equals("PhysTokenNotPresent")) {
      try {
        screens.promptForAdministratorCard("Please insert Administrator Card and then hit <enter>");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    if (code.equals("IncorrectToken")) {
      System.out.println("Incorrect card. Please remove card and insert ACS. Hit <enter>.");
      try {
        screens.promptForAdministratorCard("Incorrect card. Please insert correct Administrator Card. Hit <enter>.");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    this.passwordPrompt = "Incorrect password. Enter Administrator Card password";
    return false;
  }

  /**
   * Used to show the right message to the user when dealing with M of N cards.
   */
  public Slot reqCardCallBack(String reqCardType, String reqCardAction, int cardsDone, Slot slot) {
    return slot;
  }

  /**
   * Action is e.g. "load"
   */
  public String reqPPCallBack(String reqPPAction) {
    // Record that we prompted the user for a password so we can change our prompt
    try {
      String s = screens.promptAcsPassword(passwordPrompt);
      return s;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
