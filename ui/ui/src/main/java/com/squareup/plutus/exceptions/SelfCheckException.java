package com.squareup.subzero.exceptions;

public class SelfCheckException extends RuntimeException {
  public SelfCheckException(String message) {
    super(message);
  }
}
