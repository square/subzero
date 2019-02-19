package com.ncipher.km.nfkm;

import com.ncipher.km.marshall.NFKM_CardSetIdent;
import com.ncipher.nfast.NFException;

public class CardSet {
  public void load(Slot slot, CmdCallBack c) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public void unLoad() throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public NFKM_CardSetIdent getID() {
    throw new RuntimeException("fake nCipher");
  }
}
