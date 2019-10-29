package com.hubspot.immutables.utils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.hubspot.immutables.utils.WireSafeEnum.Deserializer;

@JsonDeserialize(using = Deserializer.class)
public final class WireSafeEnum<T extends Enum<T>> {
  private static final Map<Class<?>, Map<?, WireSafeEnum<?>>> ENUM_LOOKUP_CACHE =
      new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<String, WireSafeEnum<?>>> STRING_LOOKUP_CACHE =
      new ConcurrentHashMap<>();

  private final Class<T> enumType;
  private final String stringValue;
  private final Optional<T> enumValue;

  private WireSafeEnum(Class<T> enumType, T value) {
    this.enumType = enumType;
    this.stringValue = value.name();
    this.enumValue = Optional.of(value);
  }

  private WireSafeEnum(Class<T> enumType, String stringValue) {
    this.enumType = enumType;
    this.stringValue = stringValue;
    this.enumValue = Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> of(T value) {
    Class<T> enumType = (Class<T>) value.getClass();
    ensureEnumCacheInitialized(enumType);
    return (WireSafeEnum<T>) ENUM_LOOKUP_CACHE.get(enumType).get(value);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> of(Class<T> enumType, String value) {
    ensureStringCacheInitialized(enumType);
    WireSafeEnum<?> cached = STRING_LOOKUP_CACHE.get(enumType).get(value);
    if (cached == null) {
      return new WireSafeEnum<>(enumType, value);
    } else {
      return (WireSafeEnum<T>) cached;
    }
  }

  public Class<T> enumType() {
    return enumType;
  }

  @JsonValue
  public String asString() {
    return stringValue;
  }

  public Optional<T> asEnum() {
    return enumValue;
  }

  private static <T extends Enum<T>> void ensureEnumCacheInitialized(Class<T> enumType) {
    if (!ENUM_LOOKUP_CACHE.containsKey(enumType)) {
      initializeCache(enumType);
    }
  }

  private static <T extends Enum<T>> void ensureStringCacheInitialized(Class<T> enumType) {
    if (!STRING_LOOKUP_CACHE.containsKey(enumType)) {
      initializeCache(enumType);
    }
  }

  private static <T extends Enum<T>> void initializeCache(Class<T> enumType) {
    Map<T, WireSafeEnum<?>> enumMap = new EnumMap<>(enumType);
    Map<String, WireSafeEnum<?>> stringMap = new HashMap<>();

    for (T value : enumType.getEnumConstants()) {
      WireSafeEnum<T> wireSafeEnum = new WireSafeEnum<>(enumType, value);
      enumMap.put(value, wireSafeEnum);
      stringMap.put(value.name(), wireSafeEnum);
    }

    ENUM_LOOKUP_CACHE.put(enumType, enumMap);
    STRING_LOOKUP_CACHE.put(enumType, stringMap);
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
          if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            return WireSafeEnum.of(enumType, p.getText());
          } else {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, null);
          }
        }
      };
    }
  }
}
