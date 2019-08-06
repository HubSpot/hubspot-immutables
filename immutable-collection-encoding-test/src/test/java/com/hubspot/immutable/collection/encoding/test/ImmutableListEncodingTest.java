package com.hubspot.immutable.collection.encoding.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ImmutableListEncodingTest {

  @Test
  public void itDoesNotCopyInputImmutableList() {
    List<String> strings = ImmutableList.of("testing", "this is a test");

    TestList test = TestList.builder()
        .setStrings(strings)
        .build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesNotCopyInputImmutableSet() {
    ImmutableSet<String> strings = ImmutableSet.of("testing", "this is a test");

    TestList test = TestList.builder()
        .setStrings(strings)
        .build();

    assertThat(test.getStrings()).isSameAs(strings.asList());
  }

  @Test
  public void itDoesNotCopyAddedImmutableList() {
    List<String> strings = ImmutableList.of("testing", "this is a test");

    TestList test = TestList.builder()
        .addAllStrings(strings)
        .build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesCopyList() {
    List<String> strings = Lists.newArrayList("testing", "this is a test");

    TestList test = TestList.builder()
        .setStrings(strings)
        .build();

    assertThat(test.getStrings()).isInstanceOf(ImmutableList.class);
    assertThat(test.getStrings()).isNotSameAs(strings);
  }

  @Test
  public void itCanExpandInputImmutableList() {
    List<String> strings = ImmutableList.of("testing", "this is a test");

    TestList test = TestList.builder()
        .setStrings(strings)
        .addStrings("another")
        .build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test", "another");
  }

  @Test
  public void itCanExpandFromEmpty() {
    List<String> strings = Lists.newArrayList("testing", "this is a test");

    TestList test = TestList.builder()
        .addAllStrings(strings)
        .build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");
  }

  @Test
  public void itCanCombineInputImmutableLists() {
    List<String> strings = ImmutableList.of("testing", "this is a test");
    List<String> moreStrings = ImmutableList.of("more1", "more2");

    TestList test = TestList.builder()
        .setStrings(strings)
        .addAllStrings(moreStrings)
        .build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test", "more1", "more2");
  }


  @Test
  public void itDoesNotCopyInputImmutableListUsingWith() {
    List<String> strings = ImmutableList.of("testing", "this is a test");

    TestList test = TestList.builder()
        .build()
        .withStrings(strings);

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesNotCopyInputListUsingWith() {
    List<String> strings = Lists.newArrayList("testing", "this is a test");

    TestList test = TestList.builder()
        .build()
        .withStrings(strings);

    assertThat(test.getStrings()).isInstanceOf(ImmutableList.class);
    assertThat(test.getStrings()).containsExactlyElementsOf(strings);
  }

}
