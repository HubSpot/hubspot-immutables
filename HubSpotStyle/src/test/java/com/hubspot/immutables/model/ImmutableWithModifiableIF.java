package com.hubspot.immutables.model;

import java.util.List;

import org.immutables.value.Value;

import com.hubspot.immutables.styles.HubSpotStyle;

@HubSpotStyle
@Value.Immutable
@Value.Modifiable
public interface ImmutableWithModifiableIF {
  int getId();
  String getDescription();
  List<String> getNames();
}
