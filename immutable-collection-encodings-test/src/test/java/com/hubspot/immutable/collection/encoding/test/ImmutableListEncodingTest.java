package com.hubspot.immutable.collection.encoding.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  public void itCanAcceptIterable() {
    Iterable<String> strings = Lists.newArrayList("testing", "this is a test");

    TestList test = TestList.builder()
        .addAllStrings(strings)
        .build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");
  }

  @Test
  public void itCanAcceptVarargs() {
    TestList test = TestList.builder()
        .addStrings("testing", "this is a test")
        .build();

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");

    TestList test2 = TestList.builder()
        .addStrings("testing")
        .build();

    assertThat(test2.getStrings()).containsExactly("testing");
  }

  @Test
  public void itCanAcceptVarargsWith() {
    TestList test = TestList.builder()
        .build()
        .withStrings("testing", "this is a test");

    assertThat(test.getStrings()).containsExactly("testing", "this is a test");

    TestList test2 = TestList.builder()
        .build()
        .withStrings("testing");

    assertThat(test2.getStrings()).containsExactly("testing");
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

  @Test
  public void itCanConstructWithParameters() {
    TestList test = TestList.of(Collections.singleton("testing"));

    assertThat(test.getStrings()).containsExactly("testing");
  }

  @Test
  @Ignore
  public void itCanSerializeAndDeserializeToJson() throws IOException {
    String expectedJson = "{\"strings\":[\"testing\"]}";
    ObjectMapper mapper = new ObjectMapper();

    TestList test = TestList.of(Collections.singleton("testing"));
    assertThat(mapper.writeValueAsString(test)).isEqualToIgnoringWhitespace(expectedJson);

    TestList deser = mapper.readValue(expectedJson, TestList.class);

    assertThat(deser).isEqualTo(test);
  }

}
