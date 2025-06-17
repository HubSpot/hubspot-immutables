package com.hubspot.immutables;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TypeUseInheritanceTest {

  @Test
  public void itCopiesNullableFieldsWithFromers() {
    ImmutableFirstChild firstChild = ImmutableFirstChild
      .builder()
      .string("test")
      .myField(ImmutableMyField.builder().build())
      .build();

    ImmutableSecondChild secondChild = ImmutableSecondChild
      .builder()
      .from(firstChild)
      .build();

    assertThat(secondChild.getMyField()).isNotNull();
    assertThat(secondChild.getString()).isEqualTo("test");
  }
}
