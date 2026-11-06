package com.hubspot.immutables.utils;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.google.common.collect.ImmutableSet;
import com.hubspot.immutables.utils.WireSafeEnum.Deserializer;
import com.hubspot.immutables.utils.WireSafeEnum.KeyDeserializer;
import com.hubspot.immutables.utils.WireSafeEnum.Serializer;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
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
@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class, keyUsing = KeyDeserializer.class)
public final class WireSafeEnum<T extends Enum<T>> {

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
  public static <T extends Enum<T>> WireSafeEnum<T> fromJson(
    @Nonnull Class<T> enumType,
    @Nonnull String jsonValue
  ) {
    return fromJson(enumType, jsonValue, WireSafeEnum::new);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> WireSafeEnum<T> fromJson(
    @Nonnull Class<T> enumType,
    @Nonnull String jsonValue,
    @Nonnull BiFunction<Class<T>, String, WireSafeEnum<T>> fallback
  ) {
    checkNotNull(enumType, "enumType");
    checkNotNull(jsonValue, "jsonValue");
    checkNotNull(fallback, "fallback");

    enumType = getRealEnumType(enumType);
    ensureJsonCacheInitialized(enumType);
    WireSafeEnum<?> cached = JSON_LOOKUP_CACHE.get(enumType).get(jsonValue);
    if (cached == null) {
      return fallback.apply(enumType, jsonValue);
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
    Collection<WireSafeEnum<?>> wiresafeEnumTypes = JSON_LOOKUP_CACHE
      .get(enumType)
      .values();
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

  @SafeVarargs
  public final boolean containsAnyOf(@Nonnull T... values) {
    checkNotNull(values, "values");
    // copy-of doesn't alloc on empty input
    return containsAnyOf(ImmutableSet.copyOf(values));
  }

  public boolean containsAnyOf(@Nonnull Collection<T> values) {
    checkNotNull(values, "values");
    return enumValue.isPresent() && values.contains(enumValue.get());
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
    CacheEntries<T> entries = usesDefaultJacksonNames(enumType)
      ? buildCacheFromNames(enumType)
      : buildCacheWithMapper(enumType);
    ENUM_LOOKUP_CACHE.put(enumType, entries.enumMap);
    JSON_LOOKUP_CACHE.put(enumType, entries.jsonMap);
  }

  static final class CacheEntries<T extends Enum<T>> {

    final Map<T, WireSafeEnum<?>> enumMap;
    final Map<String, WireSafeEnum<?>> jsonMap;

    private CacheEntries(Class<T> enumType, int size) {
      this.enumMap = new EnumMap<>(enumType);
      this.jsonMap = new HashMap<>(mapCapacity(size));
    }
  }

  /*
  True only when nothing Jackson could read touches the enum, so the private
  MAPPER (no modules, mixins or enum features) would serialize each constant
  as name() and deserialize it back to itself.
   */
  static boolean usesDefaultJacksonNames(Class<? extends Enum<?>> enumType) {
    try {
      Set<Class<?>> seen = new HashSet<>();
      if (!isJacksonFree(enumType, seen)) {
        return false;
      }
      for (Enum<?> constant : enumType.getEnumConstants()) {
        if (!isJacksonFree(constant.getClass(), seen)) {
          return false;
        }
      }
      return true;
    } catch (LinkageError | SecurityException e) {
      // leave the outcome to the mapper path, as before
      return false;
    }
  }

  private static boolean isJacksonFree(Class<?> type, Set<Class<?>> seen) {
    if (type == null || type == Enum.class || type == Object.class || !seen.add(type)) {
      return true;
    }
    if (hasJacksonAnnotation(type.getDeclaredAnnotations())) {
      return false;
    }
    for (Field field : type.getDeclaredFields()) {
      if (hasJacksonAnnotation(field.getDeclaredAnnotations())) {
        return false;
      }
    }
    for (Method method : type.getDeclaredMethods()) {
      if (
        hasJacksonAnnotation(method.getDeclaredAnnotations()) ||
        hasJacksonAnnotation(method.getParameterAnnotations())
      ) {
        return false;
      }
    }
    for (Constructor<?> constructor : type.getDeclaredConstructors()) {
      if (
        hasJacksonAnnotation(constructor.getDeclaredAnnotations()) ||
        hasJacksonAnnotation(constructor.getParameterAnnotations())
      ) {
        return false;
      }
    }
    for (Class<?> iface : type.getInterfaces()) {
      if (!isJacksonFree(iface, seen)) {
        return false;
      }
    }
    return isJacksonFree(type.getSuperclass(), seen);
  }

  private static boolean hasJacksonAnnotation(Annotation[][] annotations) {
    for (Annotation[] group : annotations) {
      if (hasJacksonAnnotation(group)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasJacksonAnnotation(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (isJacksonAnnotationType(annotation.annotationType(), new HashSet<>())) {
        return true;
      }
    }
    return false;
  }

  // Recurses into meta-annotations to catch @JacksonAnnotationsInside bundles.
  private static boolean isJacksonAnnotationType(
    Class<? extends Annotation> type,
    Set<Class<?>> seen
  ) {
    if (type.getName().startsWith("com.fasterxml.jackson.")) {
      return true;
    }
    if (!seen.add(type)) {
      return false;
    }
    for (Annotation meta : type.getDeclaredAnnotations()) {
      if (isJacksonAnnotationType(meta.annotationType(), seen)) {
        return true;
      }
    }
    return false;
  }

  static <T extends Enum<T>> CacheEntries<T> buildCacheFromNames(Class<T> enumType) {
    T[] enumConstants = enumType.getEnumConstants();
    CacheEntries<T> entries = new CacheEntries<>(enumType, enumConstants.length);
    for (T enumValue : enumConstants) {
      WireSafeEnum<T> wireSafeEnum = new WireSafeEnum<>(
        enumType,
        enumValue.name(),
        enumValue
      );
      entries.enumMap.put(enumValue, wireSafeEnum);
      entries.jsonMap.put(enumValue.name(), wireSafeEnum);
    }
    return entries;
  }

  static <T extends Enum<T>> CacheEntries<T> buildCacheWithMapper(Class<T> enumType) {
    ObjectMapper mapper = MapperHolder.MAPPER;
    T[] enumConstants = enumType.getEnumConstants();
    ArrayNode stringArray = mapper.valueToTree(enumConstants);
    /*
    Convert the enum constants to JSON and then back, in case this
    mapping is not bijective. For example, two enum constants might
    both be aliased to the same JSON value. When we encounter such
    a JSON value, we'll defer to the enum's deserialization behavior.
    This should match the behavior as if the enum wasn't wrapped in
    WireSafeEnum.
     */
    T[] deserializedConstants = mapper.convertValue(
      stringArray,
      mapper.getTypeFactory().constructArrayType(enumType)
    );

    CacheEntries<T> entries = new CacheEntries<>(enumType, enumConstants.length);

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
      entries.enumMap.put(enumValue, wireSafeEnum);
      /*
      If the deserialized value doesn't match, then this enum
      is probably some sort of alias
       */
      if (enumValue == deserializedValue) {
        entries.jsonMap.put(jsonValue, wireSafeEnum);
      }
    }

    return entries;
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

  private static final class MapperHolder {

    static final ObjectMapper MAPPER = new ObjectMapper();
  }

  // adapted from Guava
  private static int mapCapacity(int elements) {
    if (elements < 3) {
      return elements + 1;
    } else {
      return (int) ((float) elements / 0.75F + 1.0F);
    }
  }

  // Use a custom serializer so that we can override the schema introspection and
  // propagate the allowed enum values from the underlying enum type. Otherwise,
  // WireSafeEnum just shows up as a string in the schema and the enum values are lost.
  @SuppressWarnings("unchecked")
  public static class Serializer
    extends StdScalarSerializer<WireSafeEnum<?>>
    implements ContextualSerializer {

    private final Optional<Class<? extends Enum<?>>> enumType;

    public Serializer() {
      this(Optional.empty());
    }

    private Serializer(Optional<Class<? extends Enum<?>>> enumType) {
      super((Class<WireSafeEnum<?>>) (Class<?>) WireSafeEnum.class);
      this.enumType = enumType;
    }

    @Override
    public void serialize(
      WireSafeEnum<?> value,
      JsonGenerator gen,
      SerializerProvider provider
    ) throws IOException {
      gen.writeString(value.asString());
    }

    @Override
    public JsonSerializer<?> createContextual(
      SerializerProvider prov,
      BeanProperty property
    ) {
      if (property == null) {
        return this;
      }

      Optional<Class<? extends Enum<?>>> enumType = Optional
        .ofNullable(property.getType())
        .filter(wrapperType -> wrapperType.containedTypeCount() == 1)
        .map(wrapperType -> wrapperType.containedType(0))
        .map(wrapperType -> (Class<? extends Enum<?>>) wrapperType.getRawClass());

      return new Serializer(enumType);
    }

    @Override
    public void acceptJsonFormatVisitor(
      JsonFormatVisitorWrapper visitor,
      JavaType typeHint
    ) throws JsonMappingException {
      JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
      if (stringVisitor == null) {
        return;
      }

      Optional<Class<? extends Enum<?>>> enumType = this.enumType;

      // fallback: try pulling enumType from typeHint
      if (
        !enumType.isPresent() && typeHint != null && typeHint.containedTypeCount() == 1
      ) {
        enumType =
          Optional.of((Class<? extends Enum<?>>) typeHint.containedType(0).getRawClass());
      }

      if (!enumType.isPresent() || !enumType.get().isEnum()) {
        return;
      }

      ImmutableSet.Builder<String> enumValues = ImmutableSet.builder();
      for (Enum<?> e : enumType.get().getEnumConstants()) {
        enumValues.add(enumValue(e));
      }

      stringVisitor.enumTypes(enumValues.build());
    }

    private static <T extends Enum<T>> String enumValue(Enum<?> e) {
      return WireSafeEnum.of((T) e).asString();
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
      throw JsonMappingException.from(p, "Expected createContextual to be called");
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
            throw ctxt.wrongTokenException(p, enumType, JsonToken.VALUE_STRING, null);
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
      throw JsonMappingException.from(
        ctxt.getParser(),
        "Expected createContextual to be called"
      );
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
              (klass, value) -> createKey(enumType, value, ctxt)
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
      throw JsonMappingException.from(
        ctxt.getParser(),
        "Can not handle contextualType: " + contextualType
      );
    } else {
      JavaType[] typeParameters = contextualType.findTypeParameters(WireSafeEnum.class);
      if (typeParameters.length != 1) {
        throw JsonMappingException.from(
          ctxt.getParser(),
          "Can not discover enum type for: " + contextualType
        );
      } else if (!typeParameters[0].isEnumType()) {
        throw JsonMappingException.from(
          ctxt.getParser(),
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

  private static <T extends Enum<T>> WireSafeEnum<T> createKey(
    JavaType enumType,
    String jsonValue,
    DeserializationContext ctxt
  ) {
    @SuppressWarnings("unchecked")
    Class<T> rawClass = (Class<T>) enumType.getRawClass();

    Optional<T> maybeEnumValue = deserializeKey(enumType, jsonValue, ctxt);

    return maybeEnumValue
      .map(enumValue -> new WireSafeEnum<>(rawClass, jsonValue, enumValue))
      .orElseGet(() -> new WireSafeEnum<>(rawClass, jsonValue));
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> Optional<T> deserializeKey(
    JavaType enumType,
    String key,
    DeserializationContext ctxt
  ) {
    try {
      com.fasterxml.jackson.databind.KeyDeserializer keyDeserializer = ctxt
        .getFactory()
        .createKeyDeserializer(ctxt, enumType);

      if (keyDeserializer == null) {
        return Optional.empty();
      }

      return Optional.ofNullable((T) keyDeserializer.deserializeKey(key, ctxt));
    } catch (Exception e) {
      return Optional.empty();
    }
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
