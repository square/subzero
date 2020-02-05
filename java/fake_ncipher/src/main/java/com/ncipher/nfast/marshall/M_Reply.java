package com.ncipher.nfast.marshall;

public class M_Reply implements Marshallable {
  public M_Cmd_Reply reply;
  public long status;
  public M_Status_ErrorInfo errorinfo;
}
