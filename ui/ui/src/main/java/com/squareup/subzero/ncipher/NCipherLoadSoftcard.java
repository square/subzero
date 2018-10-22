package com.squareup.subzero.ncipher;

import com.ncipher.km.nfkm.CmdCallBack;
import com.ncipher.km.nfkm.Slot;

/**
 * Load a softcard. There's no user interaction in this case.
 */
public class NCipherLoadSoftcard implements CmdCallBack {
  private String password;

  public NCipherLoadSoftcard(String password) {
    this.password = password;
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
    System.out.println("incorrect softcard password");
    return true;
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
    return password;
  }
}
