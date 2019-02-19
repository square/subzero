package com.ncipher.km.nfkm;

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

  public void makeBlobs(SoftCard softcard) throws NFException {
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

  public void unLoad() throws NFException {
    throw new RuntimeException("fake nCipher");
  }
}
