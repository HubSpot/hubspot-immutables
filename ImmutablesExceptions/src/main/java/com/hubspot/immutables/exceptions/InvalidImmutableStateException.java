package com.hubspot.immutables.exceptions;

public class InvalidImmutableStateException extends RuntimeException {
  public InvalidImmutableStateException(String message) {
    super(message);
  }
}
