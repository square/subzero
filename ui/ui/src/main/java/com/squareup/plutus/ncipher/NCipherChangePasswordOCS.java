package com.squareup.plutus.ncipher;

import com.ncipher.km.nfkm.CmdCallBack;
import com.ncipher.km.nfkm.Slot;

import static java.lang.String.format;

/**
 * Change password on existing OCS
 */
public class NCipherChangePasswordOCS implements CmdCallBack {
  private String changeFrom;
  private String changeTo;

  public final static String PLACEHOLDER_PASSWORD = "plutus";

  public NCipherChangePasswordOCS(String changeFrom, String changeTo) {
    this.changeFrom = changeFrom;
    this.changeTo = changeTo;
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
    return true;
  }

  /**
   * Used to show the right message to the user when dealing with M of N cards.
   */
  public Slot reqCardCallBack(String reqCardType, String reqCardAction, int cardsDone, Slot slot) {
    return slot;
  }

  /**
   * When changing passwords, action is "load" followed by "create".
   */
  public String reqPPCallBack(String reqPPAction) {
    switch (reqPPAction) {
      case "load":
        return changeFrom;
      case "create":
        return changeTo;
      default:
        throw new IllegalStateException(format("unexpected reqPPAction: %s", reqPPAction));
    }
  }
}
