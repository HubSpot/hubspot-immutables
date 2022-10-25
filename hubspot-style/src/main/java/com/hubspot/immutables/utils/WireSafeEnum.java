package com.hubspot.immutables.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

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

/**
 * This utility is meant to help with the fragility introduced by
 * using an enum across API boundaries. The most common example is
 * adding a new enum constant causing clients to explode on
 * deserialization if they don't have the updated enum definition.
 *
 * Instead of using the enum in your models directly, you could instead
 * wrap the field in a WireSafeEnum. This should be transparent from
 * a serialization perspective, but will allow you to more gracefully
 * handle the case of an unknown enum constant. It also stores the JSON
 * value when deserializing, and uses that for serialization. This means
 * that intermedaries preserve, rather than mangle, unknown enum values.
 *
 * For the most part WireSafeEnum should be a drop-in replacement, but
 * there are some things to be aware of:
 * 1. every enum constant must serialize to JSON as a non-null string
 *    (serializing as a number or null is not supported)
 * 2. enums with custom @JsonCreator logic are poorly/not supported
 * 3. T and WireSafeEnum<T> are different types so migrating is a
 *    breaking change from a code perspective and Java code usages
 *    of the field will need to get updated
 */
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
    this.enumType = checkNotNull(enumType, "enumType");
    this.jsonValue = checkNotNull(jsonValue, "jsonValue");
    this.enumValue = Optional.of(checkNotNull(enumValue, "enumValue"));
  }

  private WireSafeEnum(Class<T> enumType, String jsonValue) {
    this.enumType = checkNotNull(enumType, "enumType");
    this.jsonValue = checkNotNull(jsonValue, "jsonValue");
    this.enumValue = Optional.empty();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> of(@Nonnull T value) {
    checkNotNull(value, "value");

    Class<T> enumType = getRealEnumType((Class<T>) value.getClass());
    ensureEnumCacheInitialized(enumType);
    return (WireSafeEnum<T>) ENUM_LOOKUP_CACHE.get(enumType).get(value);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> WireSafeEnum<T> fromJson(
      @Nonnull Class<T> enumType,
      @Nonnull String jsonValue
  ) {
    checkNotNull(enumType, "enumType");
    checkNotNull(jsonValue, "jsonValue");

    enumType = getRealEnumType(enumType);
    ensureJsonCacheInitialized(enumType);
    WireSafeEnum<?> cached = JSON_LOOKUP_CACHE.get(enumType).get(jsonValue);
    if (cached == null) {
      return new WireSafeEnum<>(enumType, jsonValue);
    } else {
      return (WireSafeEnum<T>) cached;
    }
  }

  @Nonnull
  public Class<T> enumType() {
    return enumType;
  }

  @Nonnull
  @JsonValue
  public String asString() {
    return jsonValue;
  }

  @Nonnull
  public Optional<T> asEnum() {
    return enumValue;
  }

  @Nonnull
  public <X extends Throwable> T asEnumOrThrow(Supplier<? extends X> exceptionSupplier) throws X {
    return asEnum()
        .orElseThrow(exceptionSupplier);
  }

  @Nonnull
  public T asEnumOrThrow() {
    return asEnumOrThrow(this::getInvalidValueException);
  }

  private IllegalStateException getInvalidValueException() {
    Collection<WireSafeEnum<?>> wiresafeEnumTypes = JSON_LOOKUP_CACHE.get(enumType).values();
    String validMembers = Arrays.toString(wiresafeEnumTypes.stream()
        .map(WireSafeEnum::asString)
        .distinct()
        .sorted()
        .toArray());

    String message = String.format("Value '%s' is not valid for enum of type '%s'. Valid values are: %s",
        jsonValue, enumType.getSimpleName(), validMembers);

    return new IllegalStateException(message);
  }

  public boolean contains(@Nonnull T value) {
    checkNotNull(value, "value");

    return enumValue.isPresent() && enumValue.get() == value;
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

  private static <T> T checkNotNull(T o, String name) {
    return Objects.requireNonNull(o, name + " must not be null");
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

      if (enumValue instanceof WireSafeEnumAlternativeJsonInputValue) {
        String alternativeJsonInputValue = ((WireSafeEnumAlternativeJsonInputValue) enumValue).getExtraJsonInputValue();
        if (alternativeJsonInputValue != null && !alternativeJsonInputValue.isEmpty() && !alternativeJsonInputValue.equals(jsonValue)) {
          WireSafeEnum<T> wireSafeEnumForAlternativeInput = new WireSafeEnum<>(enumType, alternativeJsonInputValue, enumValue);
          enumMap.put(enumValue, wireSafeEnumForAlternativeInput);
          jsonMap.put(alternativeJsonInputValue, wireSafeEnumForAlternativeInput);
        }
      }
    }

    ENUM_LOOKUP_CACHE.put(enumType, enumMap);
    JSON_LOOKUP_CACHE.put(enumType, jsonMap);
  }

  private static <T extends Enum<T>> Class<T> getRealEnumType(Class<T> enumType) {
    Class<?> superType = enumType.getSuperclass();
    if (Enum.class.equals(superType)) {
      return enumType;
    }
    // In cases where the enum constant passed in overrides a method on the enum,
    // the class received here may be a subclass of the enum's actual type, which
    // does not have access to the enum's declared constants, so we coerce that type
    // into the supertype that was actually declared
    if (superType == null || !Enum.class.equals(superType.getSuperclass()) || !superType.equals(enumType.getEnclosingClass())) {
      throw new IllegalArgumentException("Provided type is not an enum or a direct subclass of an enum");
    }

    @SuppressWarnings("unchecked")
    Class<T> widenedType = (Class<T>) superType;
    return widenedType;
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
