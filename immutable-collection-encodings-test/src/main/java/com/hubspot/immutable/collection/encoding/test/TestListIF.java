package com.hubspot.immutable.collection.encoding.test;

import java.util.List;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@TestStyle
public interface TestListIF {
  @Parameter
  List<String> getStrings();
}
