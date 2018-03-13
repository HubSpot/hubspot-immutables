package com.hubspot.immutables.model;

import java.math.BigDecimal;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.immutables.styles.HubSpotStyle;

@HubSpotStyle
@Value.Immutable
public interface NormalizedWidgetIF {
  BigDecimal getValue();
  Optional<BigDecimal> getOptionalValue();

  @Value.Check
  default NormalizedWidgetIF normalize() {
    return isNormalized()
        ? this
        : NormalizedWidget.builder()
          .from(this)
          .setValue(getValue().setScale(6, BigDecimal.ROUND_HALF_UP))
          .setOptionalValue(getOptionalValue().map(bigDecimal -> bigDecimal.setScale(6, BigDecimal.ROUND_HALF_UP)))
          .build();
  }

  @JsonIgnore
  @Value.Auxiliary
  default boolean isNormalized() {
    return getValue().scale() == 6
        && getOptionalValue().map(bigDecimal -> bigDecimal.scale() == 6).orElse(true);
  }
}
