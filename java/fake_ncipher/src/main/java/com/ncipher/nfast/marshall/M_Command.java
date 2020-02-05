package com.ncipher.nfast.marshall;

public class M_Command implements Marshallable {
  public long cmd;

  public M_Command(long cmd, long flags, M_Cmd_Args args) {
    throw new RuntimeException("fake nCipher");
  }

  public M_Command set_certs(M_CertificateList var1) {
    throw new RuntimeException("fake nCipher");
  }
}
