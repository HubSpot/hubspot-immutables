package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestMapWithParamIF {
  @Parameter
  String getName();

  ImmutableMap<String, String> getStrings();
}
