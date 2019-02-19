package com.ncipher.km.nfkm;

import com.ncipher.km.marshall.NFKM_ModuleInfo;
import com.ncipher.nfast.NFException;

public class Module {
  public boolean isUsable() {
    throw new RuntimeException("fake nCipher");
  }

  public Slot getSlot(int slotID) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public NFKM_ModuleInfo getData() {
    throw new RuntimeException("fake nCipher");
  }
}
