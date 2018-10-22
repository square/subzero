package com.squareup.subzero;

import com.squareup.subzero.exceptions.HsmConnectionException;
import com.squareup.subzero.exceptions.MismatchedVersionException;
import com.squareup.subzero.exceptions.SelfCheckException;
import com.squareup.subzero.exceptions.UnknownException;
import com.squareup.protos.subzero.service.Internal.InternalCommandRequest;
import com.squareup.protos.subzero.service.Internal.InternalCommandResponse;
import java.io.IOException;
import java.net.Socket;

/**
 * InternalCommandConnection is a connection, usually over localhost, to the C server portion
 * of subzero.  This class is used by both the subzero-cli and subzero-integration
 */
public class InternalCommandConnector {
  private String host;
  private int port;

  /**
   * The default constructor is hardcoded to make connections to localhost:32366, which is the
   * port assigned to us in Registry.
   */
  public InternalCommandConnector() {
    this("localhost", 32366);
  }

  public InternalCommandConnector(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Run a command.  Opens a connection, writes the request, reads the response, and returns it
   */
  public InternalCommandResponse run(InternalCommandRequest internalRequest) {
    Socket socket;
    try {
      socket = new Socket(this.host, this.port);
      internalRequest.writeDelimitedTo(socket.getOutputStream());
    } catch (IOException exception) {
      throw new HsmConnectionException("Error sending request", exception);
    }

    InternalCommandResponse commandResponse;
    try {
      commandResponse = InternalCommandResponse.parseDelimitedFrom(socket.getInputStream());
      socket.close();
    } catch (IOException exception) {
      throw new HsmConnectionException("Error reading result", exception);
    }

    if (commandResponse.hasError()) {
      InternalCommandResponse.ErrorResponse error = commandResponse.getError();
      switch (error.getCode()) {
        case VERSION_MISMATCH:
          throw new MismatchedVersionException(error.toString());
        case SELF_CHECK_FAILED:
          throw new SelfCheckException(error.toString());
        default:
          throw new UnknownException("Unknown error:" + error.toString());
      }
    }

    return commandResponse;
  }
}
