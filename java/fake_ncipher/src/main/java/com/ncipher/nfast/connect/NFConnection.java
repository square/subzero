package com.ncipher.nfast.connect;

import com.ncipher.nfast.marshall.M_Command;
import com.ncipher.nfast.marshall.M_Reply;
import com.ncipher.nfast.marshall.MarshallTypeError;

public class NFConnection {
  public M_Reply transact(M_Command com) throws MarshallTypeError, CommandTooBig, ClientException, ConnectionClosed {
    throw new RuntimeException("fake nCipher");
  }
}
