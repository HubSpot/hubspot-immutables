package com.hubspot.immutables.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.HubSpotStyle;
import com.hubspot.rosetta.annotations.RosettaNaming;
import com.hubspot.rosetta.annotations.RosettaProperty;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
@RosettaNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonDeserialize(as = RosettaSprocket.class)
public interface RosettaSprocketIF {
  int getId();

  @RosettaProperty("hubspot_id")
  int getHubSpotId();

  @JsonProperty("hubspotCustomName")
  @RosettaProperty("hubspot_custom_name")
  int getCustomName();

  String getName();
  String getSomethingWithLongName();
  boolean isBlueSprocket();
}
