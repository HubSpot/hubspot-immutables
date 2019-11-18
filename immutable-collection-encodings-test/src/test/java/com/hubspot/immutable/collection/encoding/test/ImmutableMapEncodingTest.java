package com.hubspot.immutable.collection.encoding.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class ImmutableMapEncodingTest {

  @Test
  public void itDoesNotCopyInputImmutableMap() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test", "test2", "other test");

    TestMap test = TestMap.builder()
        .setStrings(strings)
        .build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesNotCopyAddedImmutableMap() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test");

    TestMap test = TestMap.builder()
        .putAllStrings(strings)
        .build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesCopyMap() {
    Map<String, String> strings = Maps.newHashMap();
    strings.put("Test", "test");
    strings.put("test2", "test2");

    TestMap test = TestMap.builder()
        .setStrings(strings)
        .build();

    assertThat(test.getStrings()).isInstanceOf(ImmutableMap.class);
    assertThat(test.getStrings()).isNotSameAs(strings);
  }

  @Test
  public void itCanExpandInputImmutableMap() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test");

    TestMap test = TestMap.builder()
        .setStrings(strings)
        .putStrings("key", "another")
        .build();

    assertThat(test.getStrings()).containsKeys("testing", "key");
    assertThat(test.getStrings()).containsValues("this is a test", "another");
  }

  @Test
  public void itCanExpandFromEmpty() {
    Map<String, String> strings = Maps.newHashMap();
    strings.put("testing", "this is a test");

    TestMap test = TestMap.builder()
        .putAllStrings(strings)
        .build();

    assertThat(test.getStrings().size()).isEqualTo(1);
  }

  @Test
  public void itCanCombineInputImmutableMaps() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test");
    Map<String, String> moreStrings = ImmutableMap.of("more1", "more2");

    TestMap test = TestMap.builder()
        .setStrings(strings)
        .putAllStrings(moreStrings)
        .build();

    assertThat(test.getStrings()).containsKeys("testing", "more1");
    assertThat(test.getStrings()).containsValues("this is a test", "more2");
  }


  @Test
  public void itDoesNotCopyInputImmutableMapUsingWith() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test");

    TestMap test = TestMap.builder()
        .build()
        .withStrings(strings);

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itCanConstructWithParameters() {
    Map<String, String> strings = ImmutableMap.of("testing", "this is a test");

    TestMap test = TestMap.of(strings);

    assertThat(test.getStrings()).isSameAs(strings);
  }
}
