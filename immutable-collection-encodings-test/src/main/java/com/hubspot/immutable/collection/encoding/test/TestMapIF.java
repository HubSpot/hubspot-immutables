package com.hubspot.immutable.collection.encoding.test;

import java.util.Map;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestMapIF {
  @Parameter
  Map<String, String> getStrings();
}
