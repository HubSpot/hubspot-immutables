package com.hubspot.immutables.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.hubspot.immutables.styles.HubSpotStyle;

@HubSpotStyle
@Value.Immutable
public interface WrapperIF {
  int getId();

  @JsonUnwrapped
  Wrapped getWrapped();
}
