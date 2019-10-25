package com.hubspot.immutables.utils;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
  private final Class<T> enumType;
  private final String stringValue;
  private final Optional<T> enumValue;

  private WireSafeEnum(Class<T> enumType, String stringValue) {
    this.enumType = enumType;
    this.stringValue = stringValue;
    this.enumValue = tryParse(enumType, stringValue);
  }

  @SuppressWarnings("unchecked")
  private WireSafeEnum(T value) {
    this.enumType = (Class<T>) value.getClass();
    this.stringValue = value.name();
    this.enumValue = Optional.of(value);
  }

  public static <T extends Enum<T>> WireSafeEnum<T> of(T value) {
    return new WireSafeEnum<>(value);
  }

  public static <T extends Enum<T>> WireSafeEnum<T> of(String value, Class<T> enumType) {
    return new WireSafeEnum<>(enumType, value);
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

  private static <T extends Enum<T>> Optional<T> tryParse(Class<T> enumType, String stringValue) {
    // TODO use Guava's Enums#getIfPresent or a similar approach
    try {
      return Optional.of(Enum.valueOf(enumType, stringValue));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static class Deserializer extends JsonDeserializer<WireSafeEnum<?>> implements ContextualDeserializer {

    @Override
    public WireSafeEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
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
    private static <T extends Enum<T>> JsonDeserializer<WireSafeEnum<T>> deserializerFor(Class<?> rawType) {
      Class<T> enumType = (Class<T>) rawType;
      // TODO cache these in a map?
      return new JsonDeserializer<WireSafeEnum<T>>() {

        @Override
        public WireSafeEnum<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
          if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            return WireSafeEnum.of(p.getText(), enumType);
          } else {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, null);
          }
        }
      };
    }
  }
}
