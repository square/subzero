package com.squareup.plutus.exceptions;

public class SelfCheckException extends RuntimeException {
  public SelfCheckException(String message) {
    super(message);
  }
}
