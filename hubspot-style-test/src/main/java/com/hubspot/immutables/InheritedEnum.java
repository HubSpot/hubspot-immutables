package com.hubspot.immutables;

import com.hubspot.immutables.utils.WireSafeEnum;
import org.immutables.value.Value;

public interface InheritedEnum {
  @Value.Default
  default WireSafeEnum<TestEnum> getFirstEnum() {
    return WireSafeEnum.of(TestEnum.THREE);
  }
}
