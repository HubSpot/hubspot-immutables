package com.hubspot.immutables.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.hubspot.immutables.style.HubSpotStyle;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
public interface WrapperIF {
  int getId();

  @JsonUnwrapped
  Wrapped getWrapped();
}
