package com.hubspot.immutables.validation;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;

public class ImmutableConditions {

  @FormatMethod
  public static void checkValid(boolean expression, @FormatString String template, Object... arguments) {
    if (!expression) {
      throw new InvalidImmutableStateException(String.format(template, arguments));
    }
  }

  @FormatMethod
  public static void checkNotEmpty(Collection<?> collection, @FormatString String template, Object... arguments) {
    checkValid(!collection.isEmpty(), template, arguments);
  }

  @FormatMethod
  public static void checkNotEmpty(String string,@FormatString String template, Object... arguments) {
    checkValid(!string.isEmpty(), template, arguments);
  }

  @Nonnull
  @FormatMethod
  public static <T> T checkNotNull(T ref, @FormatString String template, Object... arguments) {
    checkValid(ref != null, template, arguments);
    return ref;
  }

  @FormatMethod
  public static <T> T checkPresent(Optional<T> maybe, @FormatString String template, Object... arguments) {
    checkValid(maybe.isPresent(), template, arguments);
    return maybe.get();
  }
}
