package com.hubspot.immutables.utils;

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
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.hubspot.immutables.utils.WireSafeEnum.Deserializer;
import com.hubspot.immutables.utils.WireSafeEnum.KeyDeserializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

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
 * 2. T and WireSafeEnum<T> are different types so migrating is a
 *    breaking change from a code perspective and Java code usages
 *    of the field will need to get updated
 */
@JsonDeserialize(using = Deserializer.class, keyUsing = KeyDeserializer.class)
public final class WireSafeEnum<T extends Enum<T>> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final AtomicReference<ImmutableMap<Class<?>, Map<?, WireSafeEnum<?>>>> ENUM_LOOKUP_CACHE =
    new AtomicReference<>(ImmutableMap.of());
  private static final AtomicReference<ImmutableMap<Class<?>, Map<String, WireSafeEnum<?>>>> JSON_LOOKUP_CACHE =
    new AtomicReference<>(ImmutableMap.of());

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
    return enumCacheFor(enumType).get(value);
  }

  @Nonnull
  public static <T extends Enum<T>> WireSafeEnum<T> fromJson(
    @Nonnull Class<T> enumType,
    @Nonnull String jsonValue
  ) {
    return fromJson(enumType, jsonValue, WireSafeEnum::new);
  }

  @Nonnull
  private static <T extends Enum<T>> WireSafeEnum<T> fromJson(
    @Nonnull Class<T> enumType,
    @Nonnull String jsonValue,
    @Nonnull BiFunction<Class<T>, String, WireSafeEnum<T>> fallback
  ) {
    checkNotNull(enumType, "enumType");
    checkNotNull(jsonValue, "jsonValue");
    checkNotNull(fallback, "fallback");

    enumType = getRealEnumType(enumType);
    WireSafeEnum<T> cached = jsonCacheFor(enumType).get(jsonValue);
    if (cached == null) {
      return fallback.apply(enumType, jsonValue);
    } else {
      return cached;
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

  /**
   * @deprecated this method doesn't handle unknown enum values, and eliminates the
   * benefits of WireSafeEnum. If you want to compare to a specific value, you can
   * call {@link #contains(Enum)}, which handles unknown values gracefully. If you do
   * really need to coerce to an enum, you can replace this method with
   * .asEnum().orElseThrow(exceptionSupplier)
   */
  @Nonnull
  @Deprecated
  public <X extends Throwable> T asEnumOrThrow(Supplier<? extends X> exceptionSupplier)
    throws X {
    return asEnum().orElseThrow(exceptionSupplier);
  }

  /**
   * @deprecated this method doesn't handle unknown enum values, and eliminates the
   * benefits of WireSafeEnum. If you want to compare to a specific value, you can
   * call {@link #contains(Enum)}, which handles unknown values gracefully. If you do
   * really need to coerce to an enum, you can replace this method with .asEnum().get()
   */
  @Nonnull
  @Deprecated
  public T asEnumOrThrow() {
    return asEnumOrThrow(this::getInvalidValueException);
  }

  private IllegalStateException getInvalidValueException() {
    Collection<WireSafeEnum<T>> wiresafeEnumTypes = jsonCacheFor(enumType).values();
    String validMembers = Arrays.toString(
      wiresafeEnumTypes.stream().map(WireSafeEnum::asString).distinct().sorted().toArray()
    );

    String message = String.format(
      "Value '%s' is not valid for enum of type '%s'. Valid values are: %s",
      jsonValue,
      enumType.getSimpleName(),
      validMembers
    );

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
    return (
      Objects.equals(enumType, that.enumType) &&
      Objects.equals(jsonValue, that.jsonValue) &&
      Objects.equals(enumValue, that.enumValue)
    );
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

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> Map<T, WireSafeEnum<T>> enumCacheFor(
    Class<T> enumType
  ) {
    Map<T, WireSafeEnum<T>> enumCache =
      (Map<T, WireSafeEnum<T>>) (Map<?, ?>) ENUM_LOOKUP_CACHE.get().get(enumType);

    if (enumCache == null) {
      initializeCache(enumType);
      return (Map<T, WireSafeEnum<T>>) (Map<?, ?>) ENUM_LOOKUP_CACHE.get().get(enumType);
    } else {
      return enumCache;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> Map<String, WireSafeEnum<T>> jsonCacheFor(
    Class<T> enumType
  ) {
    Map<String, WireSafeEnum<T>> jsonCache =
      (Map<String, WireSafeEnum<T>>) (Map<?, ?>) JSON_LOOKUP_CACHE.get().get(enumType);

    if (jsonCache == null) {
      initializeCache(enumType);
      return (Map<String, WireSafeEnum<T>>) (Map<?, ?>) JSON_LOOKUP_CACHE
        .get()
        .get(enumType);
    } else {
      return jsonCache;
    }
  }

  @SuppressWarnings("unchecked")
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
    ImmutableMap.Builder<String, WireSafeEnum<?>> jsonMap = ImmutableMap.builder();

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
          .append(
            "Enums wrapped in WireSafeEnum must serialize to JSON as a non-null string"
          )
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

    updateCache(ENUM_LOOKUP_CACHE, enumType, enumMap);
    // <3 generics
    updateCache(
      (AtomicReference<ImmutableMap<Class<?>, Map<?, WireSafeEnum<?>>>>) (AtomicReference<?>) JSON_LOOKUP_CACHE,
      enumType,
      jsonMap.build()
    );
  }

  private static void updateCache(
    AtomicReference<ImmutableMap<Class<?>, Map<?, WireSafeEnum<?>>>> cache,
    Class<?> key,
    Map<?, WireSafeEnum<?>> value
  ) {
    while (true) {
      ImmutableMap<Class<?>, Map<?, WireSafeEnum<?>>> previous = cache.get();

      if (previous.containsKey(key)) {
        return;
      } else {
        ImmutableMap<Class<?>, Map<?, WireSafeEnum<?>>> updated = ImmutableMap
          .<Class<?>, Map<?, WireSafeEnum<?>>>builder()
          .putAll(previous)
          .put(key, value)
          .build();

        if (cache.compareAndSet(previous, updated)) {
          return;
        }
      }
    }
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
    if (
      superType == null ||
      !Enum.class.equals(superType.getSuperclass()) ||
      !superType.equals(enumType.getEnclosingClass())
    ) {
      throw new IllegalArgumentException(
        "Provided type is not an enum or a direct subclass of an enum"
      );
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

  public static class Deserializer
    extends JsonDeserializer<WireSafeEnum<?>>
    implements ContextualDeserializer {

    private static final Map<JavaType, JsonDeserializer<WireSafeEnum<?>>> DESERIALIZER_CACHE =
      new ConcurrentHashMap<>();

    @Override
    public WireSafeEnum<?> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
      throw ctxt.mappingException("Expected createContextual to be called");
    }

    @Override
    public JsonDeserializer<?> createContextual(
      DeserializationContext ctxt,
      BeanProperty property
    ) throws JsonMappingException {
      return deserializerFor(findWireSafeEnumType(ctxt.getContextualType(), ctxt));
    }

    private static JsonDeserializer<?> deserializerFor(JavaType javaType) {
      return DESERIALIZER_CACHE.computeIfAbsent(javaType, Deserializer::newDeserializer);
    }

    private static <T extends Enum<T>> JsonDeserializer<WireSafeEnum<?>> newDeserializer(
      JavaType enumType
    ) {
      return new JsonDeserializer<WireSafeEnum<?>>() {
        @Override
        public WireSafeEnum<T> deserialize(JsonParser p, DeserializationContext ctxt)
          throws IOException {
          if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
            return null;
          } else if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            @SuppressWarnings("unchecked")
            Class<T> rawType = (Class<T>) enumType.getRawClass();
            return WireSafeEnum.fromJson(
              rawType,
              p.getText(),
              (klass, value) -> create(enumType, value, p, ctxt)
            );
          } else {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, null);
          }
        }
      };
    }
  }

  public static class KeyDeserializer
    extends com.fasterxml.jackson.databind.KeyDeserializer
    implements ContextualKeyDeserializer {

    private static final Map<JavaType, com.fasterxml.jackson.databind.KeyDeserializer> KEY_DESERIALIZER_CACHE =
      new ConcurrentHashMap<>();

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt)
      throws IOException {
      throw ctxt.mappingException("Expected createContextual to be called");
    }

    @Override
    public com.fasterxml.jackson.databind.KeyDeserializer createContextual(
      DeserializationContext ctxt,
      BeanProperty property
    ) throws JsonMappingException {
      return keyDeserializerFor(
        findWireSafeEnumType(ctxt.getContextualType().getKeyType(), ctxt)
      );
    }

    private static com.fasterxml.jackson.databind.KeyDeserializer keyDeserializerFor(
      JavaType javaType
    ) {
      return KEY_DESERIALIZER_CACHE.computeIfAbsent(
        javaType,
        KeyDeserializer::newKeyDeserializer
      );
    }

    private static <T extends Enum<T>> com.fasterxml.jackson.databind.KeyDeserializer newKeyDeserializer(
      JavaType enumType
    ) {
      return new com.fasterxml.jackson.databind.KeyDeserializer() {
        @Override
        @SuppressWarnings("unchecked")
        public Object deserializeKey(String key, DeserializationContext ctxt)
          throws IOException {
          if (key == null) {
            return null;
          } else {
            Class<T> rawType = (Class<T>) enumType.getRawClass();
            return WireSafeEnum.fromJson(
              rawType,
              key,
              (klass, value) -> create(enumType, value, ctxt.getParser(), ctxt)
            );
          }
        }
      };
    }
  }

  private static JavaType findWireSafeEnumType(
    JavaType contextualType,
    DeserializationContext ctxt
  ) throws JsonMappingException {
    if (contextualType == null || !contextualType.hasRawClass(WireSafeEnum.class)) {
      throw ctxt.mappingException("Can not handle contextualType: " + contextualType);
    } else {
      JavaType[] typeParameters = contextualType.findTypeParameters(WireSafeEnum.class);
      if (typeParameters.length != 1) {
        throw ctxt.mappingException("Can not discover enum type for: " + contextualType);
      } else if (!typeParameters[0].isEnumType()) {
        throw ctxt.mappingException(
          "Can not handle non-enum type: " + typeParameters[0].getRawClass()
        );
      } else {
        return typeParameters[0];
      }
    }
  }

  private static <T extends Enum<T>> WireSafeEnum<T> create(
    JavaType enumType,
    String jsonValue,
    JsonParser parser,
    DeserializationContext ctxt
  ) {
    @SuppressWarnings("unchecked")
    Class<T> rawClass = (Class<T>) enumType.getRawClass();

    Optional<T> maybeEnumValue = deserializeValue(enumType, parser, ctxt);

    return maybeEnumValue
      .map(enumValue -> new WireSafeEnum<>(rawClass, jsonValue, enumValue))
      .orElseGet(() -> new WireSafeEnum<>(rawClass, jsonValue));
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> Optional<T> deserializeValue(
    JavaType enumType,
    JsonParser p,
    DeserializationContext ctxt
  ) {
    try {
      JsonDeserializer<?> deserializer = ctxt.findNonContextualValueDeserializer(
        enumType
      );
      if (deserializer == null) {
        return Optional.empty();
      }

      return Optional.ofNullable((T) deserializer.deserialize(p, ctxt));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
