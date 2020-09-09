package com.hubspot.immutables.utils;

import java.io.Serializable;

import com.hubspot.immutables.utils.WireSafeEnumTest.CustomJsonEnum;

class TestClass implements Serializable {
  private WireSafeEnum<CustomJsonEnum> customJsonEnum;

  public TestClass(CustomJsonEnum customJsonEnum) {
    this.customJsonEnum = WireSafeEnum.of(customJsonEnum);
  }

  public WireSafeEnum<CustomJsonEnum> getCustomJsonEnum() {
    return customJsonEnum;
  }
}
