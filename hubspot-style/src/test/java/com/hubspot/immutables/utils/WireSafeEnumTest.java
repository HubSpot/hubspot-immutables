package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

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

  public enum EnumWithOverride {
    ABC,
    DEF {
      @Override
      public String getSomething() {
        return "b";
      }
    }
    ;

    public String getSomething() {
      return "a";
    }
  }

  public enum EnumWithMultipleSerializedForms {
    ABC,
    DEF,
    ;

    @JsonCreator
    public static EnumWithMultipleSerializedForms fromString(String s) {
      if (s.equalsIgnoreCase(ABC.name())) {
        return ABC;
      } else if (s.equalsIgnoreCase(DEF.name())) {
        return DEF;
      } else {
        throw new IllegalArgumentException("Unknown value: " + s);
      }
    }
  }

  public enum EnumWithNullableJsonCreator {
    ABC,
    ;

    @JsonCreator
    public static EnumWithNullableJsonCreator fromString(String s) {
      return s.equals(ABC.name()) ? ABC : null;
    }
  }

  @JsonDeserialize(converter = StringToEnumConverter.class)
  @JsonSerialize(converter = EnumToStringConverter.class)
  public enum EnumWithConverter {
    ABC("1"),
    DEF("2"),
    ;

    private static final Map<String, EnumWithConverter> LOOKUP = Maps.uniqueIndex(Arrays.asList(values()), EnumWithConverter::getValue);

    private final String value;

    EnumWithConverter(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static Optional<EnumWithConverter> fromString(String s) {
      return Optional.ofNullable(LOOKUP.get(s));
    }
  }

  // Exceptions from converts are not wrapped in JsonMappingExceptions and therefore we need to test
  // that we also handle these failures
  public static class StringToEnumConverter implements Converter<String, EnumWithConverter> {

    @Override
    public EnumWithConverter convert(String value) {
      return EnumWithConverter
          .fromString(value)
          .orElseThrow(() ->
              new IllegalArgumentException(
                  "Could not find variant by steroids name with provided value: " + value
              )
          );
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
      return typeFactory.constructType(String.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
      return typeFactory.constructType(EnumWithConverter.class);
    }
  }

  public static class EnumToStringConverter implements Converter<EnumWithConverter, String> {

    @Override
    public String convert(EnumWithConverter value) {
      return value.getValue();
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
      return typeFactory.constructType(EnumWithConverter.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
      return typeFactory.constructType(String.class);
    }
  }

  @Test
  public void itHandlesEnumsWithOverrides() {
    WireSafeEnum<EnumWithOverride> abc = WireSafeEnum.of(EnumWithOverride.ABC);
    WireSafeEnum<EnumWithOverride> def = WireSafeEnum.of(EnumWithOverride.DEF);

    assertThat(abc.asEnum()).contains(EnumWithOverride.ABC);
    assertThat(def.asEnum()).contains(EnumWithOverride.DEF);
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

  @Test
  public void itThrowsForValueFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");

    assertThatThrownBy(wrapper::asEnumOrThrow)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Value 'INVALID' is not valid for enum of type 'RetentionPolicy'. Valid values are: [CLASS, RUNTIME, SOURCE]");
  }

  @Test
  public void itDoesNotThrowForValueFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper =
        WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");

    assertThat(wrapper.asEnumOrThrow())
        .isEqualTo(RetentionPolicy.SOURCE);
  }

  @Test
  public void itDelegatesToJsonCreatorIfNotCached() throws Exception {
    TypeReference<WireSafeEnum<EnumWithMultipleSerializedForms>> type = new TypeReference<WireSafeEnum<EnumWithMultipleSerializedForms>>() {};

    WireSafeEnum<EnumWithMultipleSerializedForms> wrapper = MAPPER.readValue("\"ABC\"", type);
    assertCorrectEnum(wrapper, "ABC", EnumWithMultipleSerializedForms.ABC);

    wrapper = MAPPER.readValue("\"abc\"", type);
    assertCorrectEnum(wrapper, "abc", EnumWithMultipleSerializedForms.ABC);

    wrapper = MAPPER.readValue("\"def\"", type);
    assertCorrectEnum(wrapper, "def", EnumWithMultipleSerializedForms.DEF);

    wrapper = MAPPER.readValue("\"xyz\"", type);
    assertThat(wrapper.enumType()).isEqualTo(EnumWithMultipleSerializedForms.class);
    assertThat(wrapper.asString()).isEqualTo("xyz");
    assertThat(wrapper.asEnum()).isEmpty();
  }

  @Test
  public void itHandlesNullFromJsonCreator() throws Exception {
    TypeReference<WireSafeEnum<EnumWithNullableJsonCreator>> type = new TypeReference<WireSafeEnum<EnumWithNullableJsonCreator>>() {};
    WireSafeEnum<EnumWithNullableJsonCreator> wrapper = MAPPER.readValue("\"ABC\"", type);
    assertCorrectEnum(wrapper, "ABC", EnumWithNullableJsonCreator.ABC);

    wrapper = MAPPER.readValue("\"xyz\"", type);
    assertThat(wrapper.enumType()).isEqualTo(EnumWithNullableJsonCreator.class);
    assertThat(wrapper.asString()).isEqualTo("xyz");
    assertThat(wrapper.asEnum()).isEmpty();
  }

  @Test
  public void itHandlesEnumsWithConverters() throws Exception {
    TypeReference<WireSafeEnum<EnumWithConverter>> type = new TypeReference<WireSafeEnum<EnumWithConverter>>() {};

    WireSafeEnum<EnumWithConverter> wrapper = MAPPER.readValue("\"1\"", type);
    assertCorrectEnum(wrapper, "1", EnumWithConverter.ABC);

    wrapper = MAPPER.readValue("\"2\"", type);
    assertCorrectEnum(wrapper, "2", EnumWithConverter.DEF);

    wrapper = MAPPER.readValue("\"3\"", type);
    assertThat(wrapper.enumType()).isEqualTo(EnumWithConverter.class);
    assertThat(wrapper.asString()).isEqualTo("3");
    assertThat(wrapper.asEnum()).isEmpty();
  }

  private <T extends Enum<T>> void assertCorrectEnum(WireSafeEnum<?> wrapper, String stringValue, T enumValue) {
    assertThat(wrapper.enumType()).isEqualTo(enumValue.getClass());
    assertThat(wrapper.asString()).isEqualTo(stringValue);
    assertThat(wrapper.asEnum().isPresent()).isTrue();
    assertThat(wrapper.asEnum().get()).isEqualTo(enumValue);
  }
}
