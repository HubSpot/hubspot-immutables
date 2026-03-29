package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;

public class WireSafeEnumTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public enum CustomJsonEnum {
    ABC,
    DEF;

    @JsonValue
    public String reversedName() {
      return new StringBuilder(name()).reverse().toString();
    }
  }

  public enum AliasJsonEnum {
    @JsonAlias("abc")
    ABC,
    @JsonAlias("def")
    DEF,
  }

  public enum NullJsonEnum {
    ABC,
    DEF;

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
    ABC,
    DEF;

    @JsonValue
    public int jsonValue() {
      return ordinal();
    }
  }

  public enum CollidingJsonEnum {
    ABC,
    DEF;

    @JsonValue
    public String jsonName() {
      return "123";
    }
  }

  public enum CollidingJsonEnumWithCreator {
    ABC,
    DEF;

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
    };

    public String getSomething() {
      return "a";
    }
  }

  public enum EnumWithMultipleSerializedForms {
    ABC,
    DEF;

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
    ABC;

    @JsonCreator
    public static EnumWithNullableJsonCreator fromString(String s) {
      return s.equals(ABC.name()) ? ABC : null;
    }
  }

  @JsonDeserialize(converter = StringToEnumConverter.class)
  @JsonSerialize(converter = EnumToStringConverter.class)
  public enum EnumWithConverter {
    ABC("1"),
    DEF("2");

    private static final Map<String, EnumWithConverter> LOOKUP = Maps.uniqueIndex(
      Arrays.asList(values()),
      EnumWithConverter::getValue
    );

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
  public static class StringToEnumConverter
    implements Converter<String, EnumWithConverter> {

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

  public static class EnumToStringConverter
    implements Converter<EnumWithConverter, String> {

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
  public void itCanCheckContains() {
    WireSafeEnum<EnumWithOverride> abc = WireSafeEnum.of(EnumWithOverride.ABC);
    WireSafeEnum<EnumWithOverride> def = WireSafeEnum.of(EnumWithOverride.DEF);

    assertThat(abc.contains(EnumWithOverride.ABC)).isTrue();
    assertThat(abc.contains(EnumWithOverride.DEF)).isFalse();
    assertThat(abc.containsAnyOf(EnumWithOverride.ABC, EnumWithOverride.DEF)).isTrue();
    assertThat(def.containsAnyOf(EnumWithOverride.ABC, EnumWithOverride.DEF)).isTrue();
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
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of(RetentionPolicy.SOURCE);
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromEnumWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.of(CustomJsonEnum.ABC);
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(
      RetentionPolicy.class,
      "SOURCE"
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
  }

  @Test
  public void itBuildsFromKnownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(
      CustomJsonEnum.class,
      "CBA"
    );
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("CBA");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownStringWithCollidingJson() {
    WireSafeEnum<CollidingJsonEnum> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnum.class,
      "123"
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromKnownStringWithCollidingJsonAndCreator() {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnumWithCreator.class,
      "123"
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("123");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnumWithCreator.DEF));
  }

  @Test
  public void itBuildsFromJsonAlias() {
    WireSafeEnum<AliasJsonEnum> wrapper = WireSafeEnum.fromJson(
      AliasJsonEnum.class,
      "abc"
    );

    assertThat(wrapper.enumType()).isEqualTo(AliasJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("abc");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(AliasJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromNameWhenJsonAliasIsAvalable() {
    WireSafeEnum<AliasJsonEnum> wrapper = WireSafeEnum.fromJson(
      AliasJsonEnum.class,
      "ABC"
    );

    assertThat(wrapper.enumType()).isEqualTo(AliasJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.of(AliasJsonEnum.ABC));
  }

  @Test
  public void itBuildsFromUnknownStringWhenJsonAliasIsAvalable() {
    WireSafeEnum<AliasJsonEnum> wrapper = WireSafeEnum.fromJson(
      AliasJsonEnum.class,
      "tuv"
    );

    assertThat(wrapper.enumType()).isEqualTo(AliasJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("tuv");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(
      RetentionPolicy.class,
      "INVALID"
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCustomJson() {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(
      CustomJsonEnum.class,
      "ABC"
    );
    assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCollidingJson() {
    WireSafeEnum<CollidingJsonEnum> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnum.class,
      "ABC"
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itBuildsFromUnknownStringWithCollidingJsonAndCreator() {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnumWithCreator.class,
      "ABC"
    );
    assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
    assertThat(wrapper.asString()).isEqualTo("ABC");
    assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
  }

  @Test
  public void itSerializesKnownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.of(RetentionPolicy.SOURCE);
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"SOURCE\""));

    wrapper = WireSafeEnum.fromJson(RetentionPolicy.class, "SOURCE");
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"SOURCE\""));
  }

  @Test
  public void itSerializesKnownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.of(CustomJsonEnum.ABC);
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"CBA\""));

    wrapper = WireSafeEnum.fromJson(CustomJsonEnum.class, "CBA");
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"CBA\""));
  }

  @Test
  public void itSerializesKnownValueAsStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnum.class,
      "123"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"123\""));

    wrapper = WireSafeEnum.of(CollidingJsonEnum.ABC);
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"123\""));
  }

  @Test
  public void itSerializesKnownValueAsStringWithCollidingJsonAndCreator()
    throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnumWithCreator.class,
      "123"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"123\""));

    wrapper = WireSafeEnum.of(CollidingJsonEnumWithCreator.ABC);
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"123\""));
  }

  @Test
  public void itSerializesUnknownValueAsString() throws IOException {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(
      RetentionPolicy.class,
      "INVALID"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"INVALID\""));
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCustomJson() throws IOException {
    WireSafeEnum<CustomJsonEnum> wrapper = WireSafeEnum.fromJson(
      CustomJsonEnum.class,
      "ABC"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"ABC\""));
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCollidingJson() throws IOException {
    WireSafeEnum<CollidingJsonEnum> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnum.class,
      "ABC"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"ABC\""));
  }

  @Test
  public void itSerializesUnknownValueAsStringWithCollidingJsonAndCreator()
    throws IOException {
    WireSafeEnum<CollidingJsonEnumWithCreator> wrapper = WireSafeEnum.fromJson(
      CollidingJsonEnumWithCreator.class,
      "ABC"
    );
    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"ABC\""));
  }

  @Test
  public void itSerializesAliasAsConstantName() throws IOException {
    WireSafeEnum<AliasJsonEnum> wrapper = WireSafeEnum.fromJson(
      AliasJsonEnum.class,
      "abc"
    );

    writeToJson(wrapper).forEach(s -> assertThat(s).isEqualTo("\"abc\""));
  }

  @Test
  public void itDeserializesFromKnownAliasString() throws IOException {
    readFromJson("\"abc\"", new TypeReference<WireSafeEnum<AliasJsonEnum>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(AliasJsonEnum.class);
        assertThat(wrapper.asString()).isEqualTo("abc");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.of(AliasJsonEnum.ABC));
      });
  }

  @Test
  public void itDeserializesFromKnownString() throws IOException {
    readFromJson("\"SOURCE\"", new TypeReference<WireSafeEnum<RetentionPolicy>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
        assertThat(wrapper.asString()).isEqualTo("SOURCE");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.of(RetentionPolicy.SOURCE));
      });
  }

  @Test
  public void itDeserializesFromKnownStringWithCustomJson() throws IOException {
    readFromJson("\"CBA\"", new TypeReference<WireSafeEnum<CustomJsonEnum>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
        assertThat(wrapper.asString()).isEqualTo("CBA");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CustomJsonEnum.ABC));
      });
  }

  @Test
  public void itDeserializesFromKnownStringWithCollidingJson() throws IOException {
    readFromJson("\"123\"", new TypeReference<WireSafeEnum<CollidingJsonEnum>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
        assertThat(wrapper.asString()).isEqualTo("123");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.of(CollidingJsonEnum.ABC));
      });
  }

  @Test
  public void itDeserializesFromKnownStringWithCollidingJsonAndCreator()
    throws IOException {
    readFromJson(
      "\"123\"",
      new TypeReference<WireSafeEnum<CollidingJsonEnumWithCreator>>() {}
    )
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
        assertThat(wrapper.asString()).isEqualTo("123");
        assertThat(wrapper.asEnum())
          .isEqualTo(Optional.of(CollidingJsonEnumWithCreator.DEF));
      });
  }

  @Test
  public void itDeserializesFromUnknownString() throws IOException {
    readFromJson("\"INVALID\"", new TypeReference<WireSafeEnum<RetentionPolicy>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
        assertThat(wrapper.asString()).isEqualTo("INVALID");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
      });
  }

  @Test
  public void itDeserializesFromUnknownStringWithCustomJson() throws IOException {
    readFromJson("\"ABC\"", new TypeReference<WireSafeEnum<CustomJsonEnum>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CustomJsonEnum.class);
        assertThat(wrapper.asString()).isEqualTo("ABC");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
      });
  }

  @Test
  public void itDeserializesFromUnknownStringWithCollidingJson() throws IOException {
    readFromJson("\"ABC\"", new TypeReference<WireSafeEnum<CollidingJsonEnum>>() {})
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnum.class);
        assertThat(wrapper.asString()).isEqualTo("ABC");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
      });
  }

  @Test
  public void itDeserializesFromUnknownStringWithCollidingJsonAndCreator()
    throws IOException {
    readFromJson(
      "\"ABC\"",
      new TypeReference<WireSafeEnum<CollidingJsonEnumWithCreator>>() {}
    )
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(CollidingJsonEnumWithCreator.class);
        assertThat(wrapper.asString()).isEqualTo("ABC");
        assertThat(wrapper.asEnum()).isEqualTo(Optional.empty());
      });
  }

  @Test
  public void itThrowsForValueFromUnknownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(
      RetentionPolicy.class,
      "INVALID"
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("INVALID");

    assertThatThrownBy(wrapper::asEnumOrThrow)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(
        "Value 'INVALID' is not valid for enum of type 'RetentionPolicy'. Valid values are: [CLASS, RUNTIME, SOURCE]"
      );
  }

  @Test
  public void itDoesNotThrowForValueFromKnownString() {
    WireSafeEnum<RetentionPolicy> wrapper = WireSafeEnum.fromJson(
      RetentionPolicy.class,
      "SOURCE"
    );
    assertThat(wrapper.enumType()).isEqualTo(RetentionPolicy.class);
    assertThat(wrapper.asString()).isEqualTo("SOURCE");

    assertThat(wrapper.asEnumOrThrow()).isEqualTo(RetentionPolicy.SOURCE);
  }

  @Test
  public void itDelegatesToJsonCreatorIfNotCached() throws Exception {
    TypeReference<WireSafeEnum<EnumWithMultipleSerializedForms>> type =
      new TypeReference<WireSafeEnum<EnumWithMultipleSerializedForms>>() {};

    readFromJson("\"ABC\"", type)
      .forEach(wrapper ->
        assertCorrectEnum(wrapper, "ABC", EnumWithMultipleSerializedForms.ABC)
      );
    readFromJson("\"abc\"", type)
      .forEach(wrapper ->
        assertCorrectEnum(wrapper, "abc", EnumWithMultipleSerializedForms.ABC)
      );
    readFromJson("\"def\"", type)
      .forEach(wrapper ->
        assertCorrectEnum(wrapper, "def", EnumWithMultipleSerializedForms.DEF)
      );
    readFromJson("\"xyz\"", type)
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(EnumWithMultipleSerializedForms.class);
        assertThat(wrapper.asString()).isEqualTo("xyz");
        assertThat(wrapper.asEnum()).isEmpty();
      });
  }

  @Test
  public void itHandlesNullFromJsonCreator() throws Exception {
    TypeReference<WireSafeEnum<EnumWithNullableJsonCreator>> type =
      new TypeReference<WireSafeEnum<EnumWithNullableJsonCreator>>() {};

    readFromJson("\"ABC\"", type)
      .forEach(wrapper ->
        assertCorrectEnum(wrapper, "ABC", EnumWithNullableJsonCreator.ABC)
      );
    readFromJson("\"xyz\"", type)
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(EnumWithNullableJsonCreator.class);
        assertThat(wrapper.asString()).isEqualTo("xyz");
        assertThat(wrapper.asEnum()).isEmpty();
      });
  }

  @Test
  public void itHandlesEnumsWithConverters() throws Exception {
    TypeReference<WireSafeEnum<EnumWithConverter>> type =
      new TypeReference<WireSafeEnum<EnumWithConverter>>() {};

    readFromJson("\"1\"", type)
      .forEach(wrapper -> assertCorrectEnum(wrapper, "1", EnumWithConverter.ABC));
    readFromJson("\"2\"", type)
      .forEach(wrapper -> assertCorrectEnum(wrapper, "2", EnumWithConverter.DEF));
    readFromJson("\"3\"", type)
      .forEach(wrapper -> {
        assertThat(wrapper.enumType()).isEqualTo(EnumWithConverter.class);
        assertThat(wrapper.asString()).isEqualTo("3");
        assertThat(wrapper.asEnum()).isEmpty();
      });
  }

  @Test
  public void itRoundsTripsAMap() throws Exception {
    Map<WireSafeEnum<RetentionPolicy>, Integer> original = new HashMap<>();
    original.put(WireSafeEnum.of(RetentionPolicy.SOURCE), 123);
    original.put(WireSafeEnum.fromJson(RetentionPolicy.class, "INVALID"), 456);

    Map<WireSafeEnum<RetentionPolicy>, Integer> parsed = MAPPER.readValue(
      MAPPER.writeValueAsString(original),
      new TypeReference<Map<WireSafeEnum<RetentionPolicy>, Integer>>() {}
    );

    assertThat(parsed).isEqualTo(original);
  }

  private Stream<String> writeToJson(WireSafeEnum<?> wireSafeEnum) throws IOException {
    Map<WireSafeEnum<?>, Integer> map = Collections.singletonMap(wireSafeEnum, 123);
    String mapKey = Iterators.getOnlyElement(MAPPER.valueToTree(map).fieldNames());

    return Stream.of(
      MAPPER.writeValueAsString(wireSafeEnum),
      MAPPER.writeValueAsString(mapKey)
    );
  }

  private <T extends Enum<T>> Stream<WireSafeEnum<T>> readFromJson(
    String json,
    TypeReference<WireSafeEnum<T>> typeReference
  ) throws IOException {
    Map<WireSafeEnum<T>, Integer> map = MAPPER.readValue(
      "{" + json + ": 123}",
      MAPPER
        .getTypeFactory()
        .constructMapType(
          HashMap.class,
          MAPPER.constructType(typeReference.getType()),
          MAPPER.constructType(Integer.class)
        )
    );

    return Stream.of(
      MAPPER.readValue(json, typeReference),
      Iterables.getOnlyElement(map.keySet())
    );
  }

  private <T extends Enum<T>> void assertCorrectEnum(
    WireSafeEnum<?> wrapper,
    String stringValue,
    T enumValue
  ) {
    assertThat(wrapper.enumType()).isEqualTo(enumValue.getClass());
    assertThat(wrapper.asString()).isEqualTo(stringValue);
    assertThat(wrapper.asEnum().isPresent()).isTrue();
    assertThat(wrapper.asEnum().get()).isEqualTo(enumValue);
  }
}
