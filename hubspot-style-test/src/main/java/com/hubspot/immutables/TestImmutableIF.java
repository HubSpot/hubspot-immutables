package com.hubspot.immutables;

import com.hubspot.immutables.encoding.WireSafeEnumEncodingEnabled;
import com.hubspot.immutables.style.HubSpotStyle;
import com.hubspot.immutables.utils.WireSafeEnum;
import org.immutables.value.Value.Immutable;

@Immutable
@HubSpotStyle
@WireSafeEnumEncodingEnabled
public interface TestImmutableIF extends InheritedEnum {
  String getString();
  WireSafeEnum<TestEnum> getSecondEnum();
}
