package com.hubspot.immutables.model;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotStyle;

@HubSpotStyle
@Value.Immutable
public interface FooIF extends FooCore {
  int getId();
}
