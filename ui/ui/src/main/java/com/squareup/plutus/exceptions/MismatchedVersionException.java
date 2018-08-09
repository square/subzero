package com.squareup.plutus.exceptions;

public class MismatchedVersionException extends RuntimeException {
  public MismatchedVersionException(String message) {
    super(message);
  }
}
