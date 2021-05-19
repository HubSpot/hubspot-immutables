package com.hubspot.immutables.model;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;
import com.hubspot.immutables.style.HubSpotImmutableStyle;
import com.hubspot.immutables.validation.ImmutableConditions;

@HubSpotImmutableStyle
@Value.Immutable
public interface WidgetGuavaIF {
  Optional<String> getAnOptionalString();
  int getAnInt();

  Set<String> getSomeVals();
  ImmutableSet<String> getSomeOtherVals();

  @Value.Check
  default void validate() {
    ImmutableConditions.checkValid(getAnInt() > 0, "int %s must be greater than 0", getAnInt());
    ImmutableConditions.checkValid(getAnInt() < 10, "int must be less than 10");
  }
}
