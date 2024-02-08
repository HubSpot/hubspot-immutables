package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestListWithDefaultIF {
  @Default
  default ImmutableList<Integer> getInts() {
    return ImmutableList.of(1);
  }
}
