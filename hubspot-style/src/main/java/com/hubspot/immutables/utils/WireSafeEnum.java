package com.hubspot.immutables.utils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hubspot.immutables.utils.WireSafeEnum.Deserializer;

@JsonDeserialize(using = Deserializer.class)
public final class WireSafeEnum<T extends Enum<T>> {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Map<Class<?>, Map<?, WireSafeEnum<?>>> ENUM_LOOKUP_CACHE =
      new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<String, WireSafeEnum<?>>> JSON_LOOKUP_CACHE =
      new ConcurrentHashMap<>();

  private final Class<T> enumType;
  private final String jsonValue;
  private final Optional<T> enumValue;

  private WireSafeEnum(Class<T> enumType, String jsonValue, T enumValue) {
    this.enumType = enumType;
    this.jsonValue = jsonValue;
    this.enumValue = Optional.of(enumValue);
  }

  private WireSafeEnum(Class<T> enumType, String jsonValue) {
    this.enumType = enumType;
    this.jsonValue = jsonValue;
    this.enumValue = Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> fromEnum(T value) {
    Class<T> enumType = (Class<T>) value.getClass();
    ensureEnumCacheInitialized(enumType);
    return (WireSafeEnum<T>) ENUM_LOOKUP_CACHE.get(enumType).get(value);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> fromJson(Class<T> enumType, String jsonValue) {
    ensureJsonCacheInitialized(enumType);
    WireSafeEnum<?> cached = JSON_LOOKUP_CACHE.get(enumType).get(jsonValue);
    if (cached == null) {
      return new WireSafeEnum<>(enumType, jsonValue);
    } else {
      return (WireSafeEnum<T>) cached;
    }
  }

  public Class<T> enumType() {
    return enumType;
  }

  @JsonValue
  public String asString() {
    return jsonValue;
  }

  public Optional<T> asEnum() {
    return enumValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WireSafeEnum<?> that = (WireSafeEnum<?>) o;
    return Objects.equals(enumType, that.enumType) &&
        Objects.equals(jsonValue, that.jsonValue) &&
        Objects.equals(enumValue, that.enumValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enumType, jsonValue, enumValue);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", "WireSafeEnum[", "]")
        .add("enumType=" + enumType)
        .add("jsonValue='" + jsonValue + "'")
        .add("enumValue=" + enumValue)
        .toString();
  }

  private static <T extends Enum<T>> void ensureEnumCacheInitialized(Class<T> enumType) {
    if (!ENUM_LOOKUP_CACHE.containsKey(enumType)) {
      initializeCache(enumType);
    }
  }

  private static <T extends Enum<T>> void ensureJsonCacheInitialized(Class<T> enumType) {
    if (!JSON_LOOKUP_CACHE.containsKey(enumType)) {
      initializeCache(enumType);
    }
  }

  private static <T extends Enum<T>> void initializeCache(Class<T> enumType) {
    T[] enumConstants = enumType.getEnumConstants();
    ArrayNode stringArray = MAPPER.valueToTree(enumConstants);
    /*
    Convert the enum constants to JSON and then back, in case this
    mapping is not bijective. For example, two enum constants might
    both be aliased to the same JSON value. When we encounter such
    a JSON value, we'll defer to the enum's deserialization behavior.
    This should match the behavior as if the enum wasn't wrapped in
    WireSafeEnum.
     */
    T[] deserializedConstants = MAPPER.convertValue(
        stringArray,
        MAPPER.getTypeFactory().constructArrayType(enumType)
    );

    Map<T, WireSafeEnum<?>> enumMap = new EnumMap<>(enumType);
    Map<String, WireSafeEnum<?>> jsonMap = new HashMap<>(mapCapacity(enumConstants.length));

    for (int i = 0; i < enumConstants.length; i++) {
      T enumValue = enumConstants[i];
      JsonNode jsonNode = stringArray.get(i);
      T deserializedValue = deserializedConstants[i];

      final String jsonValue;
      if (jsonNode.isTextual()) {
        jsonValue = jsonNode.textValue();
      } else {
        String message = new StringBuilder()
            .append("Invalid JSON value in enum type: " + enumType.getTypeName() + "\n")
            .append("Constant " + enumValue.name() + " serialized as: " + jsonNode + "\n")
            .append("Enums wrapped in WireSafeEnum must serialize to JSON as a non-null string")
            .toString();
        throw new IllegalStateException(message);
      }

      WireSafeEnum<T> wireSafeEnum = new WireSafeEnum<>(enumType, jsonValue, enumValue);
      enumMap.put(enumValue, wireSafeEnum);
      /*
      If the deserialized value doesn't match, then this enum
      is probably some sort of alias
       */
      if (enumValue == deserializedValue) {
        jsonMap.put(jsonValue, wireSafeEnum);
      }
    }

    ENUM_LOOKUP_CACHE.put(enumType, enumMap);
    JSON_LOOKUP_CACHE.put(enumType, jsonMap);
  }

  // adapted from Guava
  private static int mapCapacity(int elements) {
    if (elements < 3) {
      return elements + 1;
    } else {
      return (int) ((float) elements / 0.75F + 1.0F);
    }
  }

  public static class Deserializer extends JsonDeserializer<WireSafeEnum<?>> implements ContextualDeserializer {
    private static final Map<Class<?>, JsonDeserializer<WireSafeEnum<?>>> DESERIALIZER_CACHE =
        new ConcurrentHashMap<>();

    @Override
    public WireSafeEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      throw ctxt.mappingException("Expected createContextual to be called");
    }


    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
      JavaType contextualType = ctxt.getContextualType();
      if (contextualType == null || !contextualType.hasRawClass(WireSafeEnum.class)) {
        throw ctxt.mappingException("Can not handle contextualType: " + contextualType);
      } else {
        JavaType[] typeParameters = contextualType.findTypeParameters(WireSafeEnum.class);
        if (typeParameters.length != 1) {
          throw ctxt.mappingException("Can not discover enum type for: " + contextualType);
        } else if (!typeParameters[0].isEnumType()) {
          throw ctxt.mappingException("Can not handle non-enum type: " + typeParameters[0].getRawClass());
        } else {
          return deserializerFor(typeParameters[0].getRawClass());
        }
      }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> JsonDeserializer<?> deserializerFor(Class<?> rawType) {
      return DESERIALIZER_CACHE.computeIfAbsent(rawType, ignored -> {
        Class<T> enumType = (Class<T>) rawType;
        return newDeserializer(enumType);
      });
    }

    private static <T extends Enum<T>> JsonDeserializer<WireSafeEnum<?>> newDeserializer(Class<T> enumType) {
      return new JsonDeserializer<WireSafeEnum<?>>() {

        @Override
        public WireSafeEnum<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
          if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
            return null;
          } else if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            return WireSafeEnum.fromJson(enumType, p.getText());
          } else {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, null);
          }
        }
      };
    }
  }
}
