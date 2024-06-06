package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestSetWithDefaultIF {
  @Default
  default ImmutableSet<Integer> getInts() {
    return ImmutableSet.of(1);
  }
}
