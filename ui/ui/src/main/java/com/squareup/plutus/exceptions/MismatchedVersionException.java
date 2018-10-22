package com.squareup.subzero.exceptions;

public class MismatchedVersionException extends RuntimeException {
  public MismatchedVersionException(String message) {
    super(message);
  }
}
