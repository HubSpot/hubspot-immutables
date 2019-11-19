package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
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

  public enum NumericJsonEnum {
    ABC, DEF;

    @JsonValue
    public int jsonValue() {
      return ordinal();
    }
  }

  public enum CollidingJsonEnum {
    ABC, DEF;

    @JsonValue
    public String jsonName() {
      return "123";
    }
  }

  public enum CollidingJsonEnumWithCreator {
    ABC, DEF;

    @JsonValue
    public String jsonName() {
      return "123";
    }

    @JsonCreator
    public static CollidingJsonEnumWithCreator fromString(String s) {
      if (DEF.jsonName().equals(s)) {
        return DEF;
      } else {
        throw new IllegalArgumentException("Unknown value: " + s);
      }
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
  public void itDoesntAllowNumericJsonValues() {
    Throwable t = catchThrowable(() -> WireSafeEnum.of(NumericJsonEnum.ABC));
    assertThat(t)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("NumericJsonEnum");
  }

  @Test
  public void itBuildsFromEnum() {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.of(RetentionPolicy.SOURCE);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromEnumWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper =
        WireSafeEnum.of(CustomJsonEnum.ABC);
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromKnownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper =
        WireSafeEnum.fromJson(CustomJsonEnum.class, "CBA");
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownStringWithCollidingJson() {
    WireSafeEnum<CollidingJsonEnum> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnum.class, "123");
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownStringWithCollidingJsonAndCreator() {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnumWithCreator.class, "123");
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnumWithCreator.DEF));
  }

  @Test
  public void itBuildsFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper =
        WireSafeEnum.fromJson(CustomJsonEnum.class, "ABC");
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCollidingJson() {
    WireSafeEnum<CollidingJsonEnum> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnum.class, "ABC");
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCollidingJsonAndCreator() {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnumWithCreator.class, "ABC");
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itSerializesKnownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of(RetentionPolicy.SOURCE);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");

    wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"SOURCE\"");
  }

  @Test
  public void itSerializesKnownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.of(CustomJsonEnum.ABC);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"CBA\"");

    wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "CBA");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"CBA\"");
  }

  @Test
  public void itSerializesKnownValueAsStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper = WireSafeEnum.fromJson(CollidingJsonEnum.class, "123");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"123\"");

    wrapper = WireSafeEnum.of(CollidingJsonEnum.ABC);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"123\"");
  }

  @Test
  public void itSerializesKnownValueAsStringWithCollidingJsonAndCreator() throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnumWithCreator.class, "123");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"123\"");

    wrapper = WireSafeEnum.of(CollidingJsonEnumWithCreator.ABC);
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"123\"");
  }

  @Test
  public void itSerializesUnknownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"INVALID\"");
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper =
        WireSafeEnum.fromJson(CustomJsonEnum.class, "ABC");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"ABC\"");
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnum.class, "ABC");
    assertThat(MAPPER.writeValueAsString(wrapper)).isEqualTo("\"ABC\"");
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCollidingJsonAndCreator() throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper =
        WireSafeEnum.fromJson(CollidingJsonEnumWithCreator.class, "ABC");
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
  public void itDeserializesFromKnownStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper = MAPPER.readValue(
        "\"123\"",
        new TypeReference<WireSafeEnum<CollidingJsonEnum>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnum.ABC));
  }

  @Test
  public void itDeserializesFromKnownStringWithCollidingJsonAndCreator() throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = MAPPER.readValue(
        "\"123\"",
        new TypeReference<WireSafeEnum<CollidingJsonEnumWithCreator>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnumWithCreator.DEF));
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

  @Test
  public void itDeserializesFromUnknownStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper = MAPPER.readValue(
        "\"ABC\"",
        new TypeReference<WireSafeEnum<CollidingJsonEnum>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itDeserializesFromUnknownStringWithCollidingJsonAndCreator() throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = MAPPER.readValue(
        "\"ABC\"",
        new TypeReference<WireSafeEnum<CollidingJsonEnumWithCreator>>() {}
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }
}
