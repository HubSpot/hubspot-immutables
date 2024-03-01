package com.hubspot.immutables;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.encoding.WireSafeEnumEncodingEnabled;
import com.hubspot.immutables.style.HubSpotStyle;
import com.hubspot.immutables.utils.WireSafeEnum;

@Immutable
@HubSpotStyle
@WireSafeEnumEncodingEnabled
public interface TestImmutableIF extends InheritedEnum {
  String getString();
  WireSafeEnum<TestEnum> getSecondEnum();
}
