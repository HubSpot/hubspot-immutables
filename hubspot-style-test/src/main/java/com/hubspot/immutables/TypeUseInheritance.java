package com.hubspot.immutables;

import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;

/**
 * Demo for a specific bug involving codegen and type use annotations
 * (specifically @Nullable).
 */
@Value.Immutable
public interface TypeUseInheritance {
  String getString();

  @Nullable
  ImmutableMyField getMyField();

  @Value.Immutable
  interface MyField {}

  @Value.Immutable
  abstract class FirstChild implements TypeUseInheritance {}

  @Value.Immutable
  interface SecondChild extends TypeUseInheritance {}
}
