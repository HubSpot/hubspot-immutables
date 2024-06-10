package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestListWithParamAndDefaultIF {
  @Parameter
  String getName();

  @Default
  default ImmutableList<String> getStrings() {
    return ImmutableList.of("default");
  }
}
