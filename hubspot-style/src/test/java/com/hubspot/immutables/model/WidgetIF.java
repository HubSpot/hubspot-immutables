package com.hubspot.immutables.model;

import com.hubspot.immutables.style.HubSpotStyle;
import com.hubspot.immutables.validation.ImmutableConditions;
import java.util.Optional;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
public interface WidgetIF {
  Optional<String> getAnOptionalString();
  int getAnInt();

  @Value.Check
  default void validate() {
    ImmutableConditions.checkValid(
      getAnInt() > 0,
      "int %s must be greater than 0",
      getAnInt()
    );
    ImmutableConditions.checkValid(getAnInt() < 10, "int must be less than 10");
  }
}
