package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestSetWithParamIF {
  @Parameter
  String getName();

  ImmutableSet<String> getStrings();
}
