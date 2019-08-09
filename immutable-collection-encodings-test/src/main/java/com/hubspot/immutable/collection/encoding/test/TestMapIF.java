package com.hubspot.immutable.collection.encoding.test;

import java.util.Map;

import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestMapIF {
  Map<String, String> getStrings();
}
