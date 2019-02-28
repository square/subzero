package com.ncipher.km.nfkm;

import com.ncipher.km.marshall.NFKM_Key;
import com.ncipher.nfast.NFException;
import com.ncipher.nfast.marshall.M_KeyID;

public class Key {
  public void setName(String name) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public void setAppName(String appname) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public void makeBlobs(CardSet cardset) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public void save() throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public String getIdent() {
    throw new RuntimeException("fake nCipher");
  }

  public M_KeyID getKeyID(Module m) {
    throw new RuntimeException("fake nCipher");
  }

  public M_KeyID load(CardSet cardset, Module m) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public M_KeyID load(SoftCard softcard, Module m) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public M_KeyID load(Slot var1) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public NFKM_Key getData() {
    throw new RuntimeException("fake nCipher");
  }
}
