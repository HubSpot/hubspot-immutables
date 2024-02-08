package com.hubspot.immutables.model;

import com.hubspot.immutables.style.HubSpotModifiableStyle;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@HubSpotModifiableStyle
public interface ImmutableWithModifiableIF {
  int getId();
  String getDescription();
  List<String> getNames();
}
