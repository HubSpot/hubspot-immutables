package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestListWithParamIF {
  @Parameter
  String getName();

  ImmutableList<String> getStrings();
}
