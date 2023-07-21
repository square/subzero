package com.squareup.subzero;

import com.squareup.subzero.exceptions.HsmConnectionException;
import com.squareup.subzero.exceptions.MismatchedVersionException;
import com.squareup.subzero.exceptions.UnknownException;
import com.squareup.subzero.observers.InternalCommandRequestObserver;
import com.squareup.subzero.observers.NoopObserver;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Internal.InternalCommandResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * InternalCommandConnection is a connection, usually over localhost, to the C server portion
 * of subzero.  This class is used by both the subzero-cli and subzero-integration
 */
public class InternalCommandConnector {
  private String host;
  private int port;
  private final InternalCommandRequestObserver observer;

  /**
   * The default constructor is hardcoded to make connections to localhost:32366, which is the
   * port assigned to us in Registry.
   */
  public InternalCommandConnector() {
    this("localhost", 32366, new NoopObserver());
  }

  public InternalCommandConnector(String host, int port) {
    this(host, port, new NoopObserver());
  }

  public InternalCommandConnector(String host, int port, final InternalCommandRequestObserver observer) {
    this.host = Objects.requireNonNull(host);
    this.port = port;
    this.observer = Objects.requireNonNull(observer);
  }

  /**
   * Run a command.  Opens a connection, writes the request, reads the response, and returns it
   */
  public InternalCommandResponse run(InternalCommandRequest internalRequest) {
    try {
      observer.observe(internalRequest);
    } catch (Exception e) {
      System.out.println("Error from InternalCommandRequestObserver: " + e);
    }

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
        default:
          throw new UnknownException("Unknown error:" + error.toString());
      }
    }

    return commandResponse;
  }
}
