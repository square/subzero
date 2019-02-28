package com.ncipher.km.nfkm;

import com.ncipher.km.marshall.NFKM_CardSetIdent;
import com.ncipher.km.marshall.NFKM_WorldInfo;
import com.ncipher.nfast.NFException;
import com.ncipher.nfast.connect.NFConnection;

public class SecurityWorld {
  public static final int NFKM_KNSO = 0;

  public boolean isInitialised() throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public Module getModule(int id) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public Module[] getModules() throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public Key getKey(String appname, String keyident) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public NFKM_WorldInfo getData() {
    throw new RuntimeException("fake nCipher");
  }

  public CardSet[] getCardSets(NFKM_CardSetIdent ocsid) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public void changePP(Slot slot, CmdCallBack c) throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public KeyGenerator getKeyGenerator() {
    throw new RuntimeException("fake nCipher");
  }

  public NFConnection getConnection() {
    throw new RuntimeException("fake nCipher");
  }

  public SoftCard[] getSoftCards() throws NFException {
    throw new RuntimeException("fake nCipher");
  }

  public AdminKeys loadAdminKeys(Slot var1, int[] var2, CmdCallBack var3) throws NFException {
    throw new RuntimeException("fake nCipher");
  }
}
