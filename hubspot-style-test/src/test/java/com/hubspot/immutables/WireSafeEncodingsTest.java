package com.hubspot.immutables;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.hubspot.immutables.utils.WireSafeEnum;

public class WireSafeEncodingsTest {

  @Test
  public void itImplementsBuilderSetProperly() {
    TestImmutable one = TestImmutable.builder()
        .setString("value")
        .setEnum(TestEnum.ONE)
        .setOtherEnum(TestEnum.ONE)
        .build();

    TestImmutable two = TestImmutable.builder()
        .setString("value")
        .setEnum(WireSafeEnum.of(TestEnum.TWO))
        .setOtherEnum(WireSafeEnum.of(TestEnum.TWO))
        .build();

    assertThat(one.getEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(one.getOtherEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(two.getEnum()).isEqualTo(WireSafeEnum.of(TestEnum.TWO));
    assertThat(two.getOtherEnum()).isEqualTo(WireSafeEnum.of(TestEnum.TWO));

    assertThat(one).isNotEqualTo(two);
  }

  @Test
  public void itImplementsWithProperly() {
    TestImmutable one = TestImmutable.builder()
        .setString("value")
        .setEnum(TestEnum.ONE)
        .setOtherEnum(TestEnum.ONE)
        .build();

    assertThat(one.withEnum(TestEnum.TWO).getEnum())
        .isEqualTo(WireSafeEnum.of(TestEnum.TWO));

    assertThat(one.withOtherEnum(TestEnum.TWO).getOtherEnum())
        .isEqualTo(WireSafeEnum.of(TestEnum.TWO));
  }

  @Test
  public void itImplementsFromProperly() {
    TestImmutable one = TestImmutable.builder()
        .setString("value")
        .setEnum(TestEnum.ONE)
        .setOtherEnum(TestEnum.TWO)
        .build();

    TestImmutable two = TestImmutable.builder()
        .from(one)
        .setOtherEnum(TestEnum.THREE)
        .build();

    assertThat(two.getEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(two.getOtherEnum()).isEqualTo(WireSafeEnum.of(TestEnum.THREE));

    assertThat(one).isNotEqualTo(two);
  }
}
