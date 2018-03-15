package com.hubspot.immutables.validation;

public class InvalidImmutableStateException extends RuntimeException {
  public InvalidImmutableStateException(String message) {
    super(message);
  }
}
