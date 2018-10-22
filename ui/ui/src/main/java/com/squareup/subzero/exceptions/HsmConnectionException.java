package com.squareup.subzero.exceptions;

public class HsmConnectionException extends RuntimeException {
  public HsmConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
