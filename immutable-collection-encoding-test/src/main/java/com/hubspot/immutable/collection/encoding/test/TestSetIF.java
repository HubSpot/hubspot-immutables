package com.hubspot.immutable.collection.encoding.test;

import java.util.Set;

import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestSetIF {
  Set<String> getStrings();
}
