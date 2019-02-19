package com.ncipher.km.nfkm;

import com.ncipher.km.marshall.NFKM_SoftCardIdent;
import com.ncipher.nfast.NFException;

public class SoftCard {
  public void load(Module module, CmdCallBack callback) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public String getName() {
    throw new RuntimeException("fake nCipher");
  }

  public NFKM_SoftCardIdent getID() {
    throw new RuntimeException("fake nCipher");
  }
}
