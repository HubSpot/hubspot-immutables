package com.hubspot.immutables.model;

import java.util.List;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotModifiableStyle;

@Value.Immutable
@Value.Modifiable
@HubSpotModifiableStyle
public interface ImmutableWithModifiableIF {
  int getId();
  String getDescription();
  List<String> getNames();
}
