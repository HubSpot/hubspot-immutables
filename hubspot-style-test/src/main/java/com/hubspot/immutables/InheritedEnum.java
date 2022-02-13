package com.hubspot.immutables;

import org.immutables.value.Value;

import com.hubspot.immutables.utils.WireSafeEnum;

public interface InheritedEnum {
  @Value.Default
  default WireSafeEnum<TestEnum> getFirstEnum() {
    return WireSafeEnum.of(TestEnum.ONE);
  }
}
