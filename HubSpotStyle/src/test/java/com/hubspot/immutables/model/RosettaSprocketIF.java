package com.hubspot.immutables.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.HubSpotStyle;
import com.hubspot.rosetta.annotations.RosettaNaming;
import com.hubspot.rosetta.annotations.RosettaProperty;

@HubSpotStyle
@Value.Immutable
@RosettaNaming(value = LowerCaseWithUnderscoresStrategy.class)
@JsonDeserialize(as = RosettaSprocket.class)
public interface RosettaSprocketIF {
  int getId();
  @RosettaProperty("hubspot_id")
  int getHubSpotId();
  String getName();
  String getSomethingWithLongName();
  boolean isBlueSprocket();
}
