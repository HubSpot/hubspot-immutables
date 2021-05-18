package com.hubspot.immutable.collection.encoding.test;

import java.util.Set;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestSetIF {
  @Parameter
  Set<String> getStrings();
}
