package com.hubspot.immutable.collection.encoding.test;

import java.util.List;

import org.immutables.value.Value.Immutable;

@Immutable
@TestStyle
public interface TestListIF {
  List<String> getStrings();
}
