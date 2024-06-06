package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestMapWithDefaultIF {
  @Default
  default ImmutableMap<String, Integer> getInts() {
    return ImmutableMap.of("one", 1);
  }
}
