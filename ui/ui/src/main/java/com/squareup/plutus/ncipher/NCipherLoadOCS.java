package com.squareup.plutus.ncipher;

import com.google.common.base.Strings;
import com.ncipher.km.nfkm.CmdCallBack;
import com.ncipher.km.nfkm.Slot;
import com.squareup.plutus.framebuffer.Screens;
import java.io.IOException;

/**
 * Load existing OCS
 *
 * 1. Tell the user to insert their OCS.
 * 2. Prompt for the password.
 *
 * For ease of development purpose, the prompt can be overridden with a hardcoded password.
 */
public class NCipherLoadOCS implements CmdCallBack {
  private String defaultPassword;
  private Screens screens;
  private boolean userEnteredPassword = false;
  private String passwordPrompt = "Enter Operator Card password";

  private String requireChange;
  private boolean requirePasswordChange = false;

  public NCipherLoadOCS(String defaultPassword, Screens screens, String requireChange) {
    this.defaultPassword = defaultPassword;
    this.screens = screens;
    this.requireChange = requireChange;
  }

  public boolean getRequirePasswordChange() {
    return requirePasswordChange;
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
        screens.promptForOperatorCard("Please insert Operator Card and then hit <enter>");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    if (code.equals("IncorrectToken")) {
      System.out.println("Incorrect card. Please remove card and insert OCS. Hit <enter>.");
      try {
        screens.promptForOperatorCard("Incorrect card. Please insert correct Operator Card. Hit <enter>.");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    if(this.userEnteredPassword) {
      this.passwordPrompt = "Incorrect password. Enter Operator Card password";
    }
    // clear defaultPassword so we prompt for a password on the console.
    this.defaultPassword = null;
    requirePasswordChange = false;
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
    if (!Strings.isNullOrEmpty(defaultPassword)) {
      return defaultPassword;
    }
    // Record that we prompted the user for a password so we can change our prompt
    userEnteredPassword = true;
    try {
      String s = screens.promptPassword(passwordPrompt);
      if (s.equals(requireChange)) {
        requirePasswordChange = true;
      }
      return s;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
