package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WireSafeEnumTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void itBuildsFromEnum() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of(RetentionPolicy.SOURCE);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of("SOURCE", RetentionPolicy.class);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of("INVALID", RetentionPolicy.class);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itSerializesKnownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of(RetentionPolicy.SOURCE);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");

    wrapper = WireSafeEnum.of("SOURCE", RetentionPolicy.class);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");
  }

  @Test
  public void itSerializesUnknownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of("INVALID", RetentionPolicy.class);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"INVALID\"");
  }

  @Test
  public void itDeserializesFromKnownString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = MAPPER.readValue(
        "\"SOURCE\"",
        new TypeReference<WireSafeEnum<RetentionPolicy>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itDeserializesFromUnknownString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = MAPPER.readValue(
        "\"INVALID\"",
        new TypeReference<WireSafeEnum<RetentionPolicy>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }
}
