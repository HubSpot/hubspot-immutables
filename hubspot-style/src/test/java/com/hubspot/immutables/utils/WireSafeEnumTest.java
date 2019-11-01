package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WireSafeEnumTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public enum CustomJsonEnum {
    ABC, DEF;

    @JsonValue
    public String reversedName() {
      return new StringBuilder(name()).reverse().toString();
    }
  }

  public enum NullJsonEnum {
    ABC, DEF;

    @JsonValue
    public String jsonName() {
      if (this == DEF) {
        return null;
      } else {
        return name();
      }
    }
  }

  public enum CollidingJsonEnum {
    ABC, DEF;

    @JsonValue
    public String jsonName() {
      return "123";
    }
  }

  @Test
  public void itParsesNullAsNull() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = MAPPER.readValue(
        "null",
        new TypeReference<WireSafeEnum<RetentionPolicy>>() {}
    );
    assertThat(wrapper).isNull();
  }

  @Test
  public void itDoesntAllowNullJsonValues() {
    Throwable t = catchThrowable(() -> WireSafeEnum.fromJson(NullJsonEnum.class, "ABC"));
    assertThat(t)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("NullJsonEnum")
        .hasMessageContaining("DEF")
        .hasMessageContaining("null");
  }

  @Test
  public void itDoesntAllowCollidingJsonValues() {
    Throwable t = catchThrowable(() -> WireSafeEnum.fromJson(CollidingJsonEnum.class, "123"));
    assertThat(t)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CollidingJsonEnum")
        .hasMessageContaining("ABC")
        .hasMessageContaining("DEF")
        .hasMessageContaining("123");
  }

  @Test
  public void itBuildsFromEnum() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromEnum(RetentionPolicy.SOURCE);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromEnumWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromEnum(CustomJsonEnum.ABC);
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromKnownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "CBA");
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "ABC");
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itSerializesKnownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromEnum(RetentionPolicy.SOURCE);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");

    wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");
  }

  @Test
  public void itSerializesKnownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromEnum(CustomJsonEnum.ABC);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"CBA\"");

    wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "CBA");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"CBA\"");
  }

  @Test
  public void itSerializesUnknownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"INVALID\"");
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "ABC");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"ABC\"");
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
  public void itDeserializesFromKnownStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = MAPPER.readValue(
        "\"CBA\"",
        new TypeReference<WireSafeEnum<CustomJsonEnum>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
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

  @Test
  public void itDeserializesFromUnknownStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = MAPPER.readValue(
        "\"ABC\"",
        new TypeReference<WireSafeEnum<CustomJsonEnum>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }
}
