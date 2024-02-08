package com.hubspot.immutables.model;

import com.hubspot.immutables.style.HubSpotStyle;
import java.util.Optional;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
public abstract class AbstractSprocket {

  public abstract Optional<String> getAnOptionalString();

  public abstract int getAnInt();
}
