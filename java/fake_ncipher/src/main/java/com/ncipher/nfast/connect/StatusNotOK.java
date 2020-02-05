package com.ncipher.nfast.connect;

import com.ncipher.nfast.NFException;

public class StatusNotOK extends NFException {
  public StatusNotOK(String var1) {
    throw new RuntimeException("fake nCipher");
  }
}
