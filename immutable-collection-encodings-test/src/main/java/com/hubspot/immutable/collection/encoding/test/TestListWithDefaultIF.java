package com.hubspot.immutable.collection.encoding.test;

import com.google.common.collect.ImmutableList;
import com.hubspot.immutables.style.HubSpotImmutableStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@HubSpotImmutableStyle
public interface TestListWithDefaultIF {
  @Default
  default ImmutableList<Integer> getInts() {
    return ImmutableList.of(1);
  }
}
