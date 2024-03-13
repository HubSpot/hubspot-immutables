package com.hubspot.immutable.collection.encoding.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class ImmutableSetEncodingTest {

  @Test
  public void itDoesNotCopyInputImmutableSet() {
    Set<String> strings = ImmutableSet.of("testing", "this is a test");

    TestSet test = TestSet.builder().setStrings(strings).build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesNotCopyAddedImmutableSet() {
    Set<String> strings = ImmutableSet.of("testing", "this is a test");

    TestSet test = TestSet.builder().addAllStrings(strings).build();

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesCopySet() {
    Set<String> strings = Sets.newHashSet("testing", "this is a test");

    TestSet test = TestSet.builder().setStrings(strings).build();

    assertThat(test.getStrings()).isInstanceOf(ImmutableSet.class);
    assertThat(test.getStrings()).isNotSameAs(strings);
  }

  @Test
  public void itCanExpandInputImmutableSet() {
    Set<String> strings = ImmutableSet.of("testing", "this is a test");

    TestSet test = TestSet.builder().setStrings(strings).addStrings("another").build();

    assertThat(test.getStrings())
      .containsExactlyInAnyOrder("testing", "this is a test", "another");
  }

  @Test
  public void itCanExpandFromEmpty() {
    Set<String> strings = Sets.newHashSet("testing", "this is a test");

    TestSet test = TestSet.builder().addAllStrings(strings).build();

    assertThat(test.getStrings()).containsExactlyInAnyOrder("testing", "this is a test");
  }

  @Test
  public void itCanAcceptIterable() {
    Iterable<String> strings = Lists.newArrayList("testing", "this is a test");

    TestSet test = TestSet.builder().addAllStrings(strings).build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");
  }

  @Test
  public void itCanAcceptVarargs() {
    TestSet test = TestSet.builder().addStrings("testing", "this is a test").build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");
  }

  @Test
  public void itCanAcceptVarargsWith() {
    TestSet test = TestSet.builder().build().withStrings("testing", "this is a test");

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");
  }

  @Test
  public void itCanCombineInputImmutableSets() {
    Set<String> strings = ImmutableSet.of("testing", "this is a test");
    Set<String> moreStrings = ImmutableSet.of("more1", "more2");

    TestSet test = TestSet
      .builder()
      .setStrings(strings)
      .addAllStrings(moreStrings)
      .build();

    assertThat(test.getStrings())
      .containsExactlyInAnyOrder("testing", "this is a test", "more1", "more2");
  }

  @Test
  public void itDoesNotCopyInputImmutableSetUsingWith() {
    Set<String> strings = ImmutableSet.of("testing", "this is a test");

    TestSet test = TestSet.builder().build().withStrings(strings);

    assertThat(test.getStrings()).isSameAs(strings);
  }

  @Test
  public void itDoesNotCopyInputSetUsingWith() {
    Set<String> strings = Sets.newHashSet("testing", "this is a test");

    TestSet test = TestSet.builder().build().withStrings(strings);

    assertThat(test.getStrings()).isInstanceOf(ImmutableSet.class);
    assertThat(test.getStrings()).containsAll(strings);
  }

  @Test
  public void itCanConstructWithParameters() {
    TestSet test = TestSet.of(Collections.singleton("testing"));

    assertThat(test.getStrings()).containsExactly("testing");
  }
}
