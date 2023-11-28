package com.hubspot.immutables.validation;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.Strings;

public class ImmutableConditions {

  public static void checkValid(boolean expression, String template, Object... arguments) {
    if (!expression) {
      throw new InvalidImmutableStateException(Strings.lenientFormat(template, arguments));
    }
  }

  public static void checkNotEmpty(Collection<?> collection, String template, Object... arguments) {
    checkValid(!collection.isEmpty(), template, arguments);
  }

  public static void checkNotEmpty(String string, String template, Object... arguments) {
    checkValid(!string.isEmpty(), template, arguments);
  }

  @Nonnull
  public static <T> T checkNotNull(T ref, String template, Object... arguments) {
    checkValid(ref != null, template, arguments);
    return ref;
  }

  public static <T> T checkPresent(Optional<T> maybe, String template, Object... arguments) {
    checkValid(maybe.isPresent(), template, arguments);
    return maybe.get();
  }
}
