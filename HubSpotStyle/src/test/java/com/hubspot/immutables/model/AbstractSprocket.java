package com.hubspot.immutables.model;

import java.util.Optional;

import org.immutables.value.Value;

import com.hubspot.immutables.styles.HubSpotStyle;

@HubSpotStyle
@Value.Immutable
public abstract class AbstractSprocket {
  public abstract Optional<String> getAnOptionalString();
  public abstract int getAnInt();
}
