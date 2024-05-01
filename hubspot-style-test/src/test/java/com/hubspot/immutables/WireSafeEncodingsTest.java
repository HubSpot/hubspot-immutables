package com.hubspot.immutables;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.immutables.utils.WireSafeEnum;
import org.junit.Test;

public class WireSafeEncodingsTest {

  @Test
  public void itImplementsBuilderSetProperly() {
    TestImmutable one = TestImmutable
      .builder()
      .setString("value")
      .setFirstEnum(TestEnum.ONE)
      .setSecondEnum(TestEnum.ONE)
      .build();

    TestImmutable two = TestImmutable
      .builder()
      .setString("value")
      .setFirstEnum(WireSafeEnum.of(TestEnum.TWO))
      .setSecondEnum(WireSafeEnum.of(TestEnum.TWO))
      .build();

    assertThat(one.getFirstEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(one.getSecondEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(two.getFirstEnum()).isEqualTo(WireSafeEnum.of(TestEnum.TWO));
    assertThat(two.getSecondEnum()).isEqualTo(WireSafeEnum.of(TestEnum.TWO));

    assertThat(one).isNotEqualTo(two);
  }

  @Test
  public void itImplementsWithProperly() {
    TestImmutable one = TestImmutable
      .builder()
      .setString("value")
      .setFirstEnum(TestEnum.ONE)
      .setSecondEnum(TestEnum.ONE)
      .build();

    assertThat(one.withFirstEnum(TestEnum.TWO).getFirstEnum())
      .isEqualTo(WireSafeEnum.of(TestEnum.TWO));

    assertThat(one.withSecondEnum(TestEnum.TWO).getSecondEnum())
      .isEqualTo(WireSafeEnum.of(TestEnum.TWO));
  }

  @Test
  public void itImplementsFromProperly() {
    TestImmutable one = TestImmutable
      .builder()
      .setString("value")
      .setFirstEnum(TestEnum.ONE)
      .setSecondEnum(TestEnum.TWO)
      .build();

    TestImmutable two = TestImmutable
      .builder()
      .from(one)
      .setSecondEnum(TestEnum.THREE)
      .build();

    assertThat(two.getFirstEnum()).isEqualTo(WireSafeEnum.of(TestEnum.ONE));
    assertThat(two.getSecondEnum()).isEqualTo(WireSafeEnum.of(TestEnum.THREE));

    assertThat(one).isNotEqualTo(two);
  }

  @Test
  public void itHandlesDefault() {
    TestImmutable one = TestImmutable
      .builder()
      .setString("value")
      .setSecondEnum(TestEnum.TWO)
      .build();

    assertThat(one.getFirstEnum()).isEqualTo(WireSafeEnum.of(TestEnum.THREE));
  }
}
