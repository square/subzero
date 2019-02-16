package com.ncipher.provider.km;

import com.ncipher.km.nfkm.SecurityWorld;
import java.security.Provider;

public class nCipherKM extends Provider {
  public nCipherKM() {
    super("fake-nCipherKM", 0, "fake-nCiperKM");
    throw new RuntimeException("fake nCipher");
  }

  public static SecurityWorld getSW() {
    throw new RuntimeException("fake nCipher");
  }
}
