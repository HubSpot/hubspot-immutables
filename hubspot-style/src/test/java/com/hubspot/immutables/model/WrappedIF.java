package com.hubspot.immutables.model;

import com.hubspot.immutables.style.HubSpotStyle;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
public interface WrappedIF {
  String getSomeString();
  int getAnInteger();
}
