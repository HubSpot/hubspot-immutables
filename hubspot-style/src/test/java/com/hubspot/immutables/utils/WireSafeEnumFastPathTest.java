package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hubspot.immutables.utils.WireSafeEnum.CacheEntries;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.junit.Test;

public class WireSafeEnumFastPathTest {

  // ---- shapes Jackson serializes as name() ----

  public enum Plain {
    A,
    B,
    C,
  }

  public enum Empty {}

  public enum WithFieldsAndToString {
    A("x"),
    B("y");

    private final String label;

    WithFieldsAndToString(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    @Override
    public String toString() {
      return "label-" + label;
    }
  }

  public enum WithBodies {
    A {
      @Override
      int value() {
        return 1;
      }
    },
    B {
      @Override
      int value() {
        return 2;
      }

      @Override
      public String toString() {
        return "bee";
      }
    };

    abstract int value();
  }

  public interface PlainInterface {
    String describe();
  }

  public enum ImplementsPlainInterface implements PlainInterface {
    A,
    B;

    @Override
    public String describe() {
      return name().toLowerCase();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(
    { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER }
  )
  public @interface Unrelated {
  }

  @Unrelated
  public enum NonJacksonAnnotations {
    @Deprecated
    @Unrelated
    A,
    B;

    @Nonnull
    @Unrelated
    public String thing(@Unrelated String s) {
      return s;
    }
  }

  // ---- shapes Jackson treats specially ----

  public enum WithJsonValue {
    A,
    B;

    @JsonValue
    public String json() {
      return "v-" + name();
    }
  }

  public enum WithJsonPropertyOnConstants {
    @JsonProperty("alpha")
    A,
    B,
  }

  public enum WithJsonCreator {
    A,
    B;

    @JsonCreator
    public static WithJsonCreator fromJson(String value) {
      return "A".equals(value) ? B : A;
    }
  }

  public enum WithJsonAlias {
    @JsonAlias("a")
    A,
    B,
  }

  public enum WithJsonEnumDefaultValue {
    A,
    @JsonEnumDefaultValue
    B,
  }

  public enum WithJsonIgnoreOnMethod {
    A;

    @JsonIgnore
    public String x() {
      return "";
    }
  }

  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  public enum ShapeObject {
    A,
    B;

    public String getLabel() {
      return name();
    }
  }

  @JsonFormat(shape = JsonFormat.Shape.NUMBER)
  public enum ShapeNumber {
    A,
    B,
  }

  public static class LowercaseSerializer extends JsonSerializer<Enum<?>> {

    @Override
    public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider sp)
      throws IOException {
      gen.writeString(value.name().toLowerCase());
    }
  }

  @JsonSerialize(using = LowercaseSerializer.class)
  public enum WithJsonSerialize {
    A,
    B,
  }

  public interface JsonValueInterface {
    @JsonValue
    String json();
  }

  public enum ImplementsJsonValueInterface implements JsonValueInterface {
    A,
    B;

    @Override
    public String json() {
      return "i-" + name();
    }
  }

  public interface InheritsJsonValueInterface extends JsonValueInterface {}

  public enum ImplementsInterfaceTransitively implements InheritsJsonValueInterface {
    A,
    B;

    @Override
    public String json() {
      return "t-" + name();
    }
  }

  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  public interface ObjectShapeInterface {}

  public enum ImplementsObjectShapeInterface implements ObjectShapeInterface {
    A,
  }

  public enum JsonValueInConstantBody {
    A {
      @JsonValue
      public String json() {
        return "body";
      }
    },
    B,
  }

  public enum JsonPropertyOnConstructorParam {
    A("a");

    JsonPropertyOnConstructorParam(@JsonProperty("x") String x) {}
  }

  @Retention(RetentionPolicy.RUNTIME)
  @JacksonAnnotationsInside
  @JsonFormat(shape = JsonFormat.Shape.NUMBER)
  public @interface NumberBundle {
  }

  @NumberBundle
  public enum WithAnnotationBundle {
    A,
    B,
  }

  @Retention(RetentionPolicy.RUNTIME)
  @NumberBundle
  public @interface NestedBundle {
  }

  @NestedBundle
  public enum WithNestedBundle {
    A,
  }

  private static final List<Case> ELIGIBLE = Arrays.asList(
    Case.of(Plain.class),
    Case.of(Empty.class),
    Case.of(WithFieldsAndToString.class),
    Case.of(WithBodies.class),
    Case.of(ImplementsPlainInterface.class),
    Case.of(NonJacksonAnnotations.class),
    Case.of(RetentionPolicy.class),
    Case.of(TimeUnit.class),
    Case.of(DayOfWeek.class),
    Case.of(ChronoUnit.class)
  );

  private static final List<Case> INELIGIBLE = Arrays.asList(
    Case.of(WithJsonValue.class),
    Case.of(WithJsonPropertyOnConstants.class),
    Case.of(WithJsonCreator.class),
    Case.of(WithJsonAlias.class),
    Case.of(WithJsonEnumDefaultValue.class),
    Case.of(WithJsonIgnoreOnMethod.class),
    Case.of(ShapeObject.class),
    Case.of(ShapeNumber.class),
    Case.of(WithJsonSerialize.class),
    Case.of(ImplementsJsonValueInterface.class),
    Case.of(ImplementsInterfaceTransitively.class),
    Case.of(ImplementsObjectShapeInterface.class),
    Case.of(JsonValueInConstantBody.class),
    Case.of(JsonPropertyOnConstructorParam.class),
    Case.of(WithAnnotationBundle.class),
    Case.of(WithNestedBundle.class)
  );

  // Shapes whose mapper-built cache really differs from name()/valueOf, so the
  // differential check below is not vacuous.
  private static final List<Case> MAPPER_DIFFERS = Arrays.asList(
    Case.of(WithJsonValue.class),
    Case.of(WithJsonPropertyOnConstants.class),
    Case.of(WithJsonCreator.class),
    Case.of(ShapeObject.class),
    Case.of(ShapeNumber.class),
    Case.of(WithJsonSerialize.class),
    Case.of(ImplementsJsonValueInterface.class),
    Case.of(ImplementsInterfaceTransitively.class),
    Case.of(ImplementsObjectShapeInterface.class),
    Case.of(WithAnnotationBundle.class)
  );

  private static final class Case {

    final Class<?> type;
    final Supplier<CacheEntries<?>> fromNames;
    final Supplier<CacheEntries<?>> withMapper;
    final BooleanSupplier eligible;

    private Case(
      Class<?> type,
      Supplier<CacheEntries<?>> fromNames,
      Supplier<CacheEntries<?>> withMapper,
      BooleanSupplier eligible
    ) {
      this.type = type;
      this.fromNames = fromNames;
      this.withMapper = withMapper;
      this.eligible = eligible;
    }

    static <E extends Enum<E>> Case of(Class<E> type) {
      return new Case(
        type,
        () -> WireSafeEnum.buildCacheFromNames(type),
        () -> WireSafeEnum.buildCacheWithMapper(type),
        () -> WireSafeEnum.usesDefaultJacksonNames(type)
      );
    }

    boolean eligible() {
      return eligible.getAsBoolean();
    }
  }

  @Test
  public void itMatchesTheMapperForEligibleEnums() {
    for (Case c : ELIGIBLE) {
      assertThat(c.eligible()).as(c.type.getName()).isTrue();
      CacheEntries<?> fast = c.fromNames.get();
      CacheEntries<?> mapper = c.withMapper.get();
      assertThat(fast.enumMap).as(c.type.getName()).isEqualTo(mapper.enumMap);
      assertThat(fast.jsonMap).as(c.type.getName()).isEqualTo(mapper.jsonMap);
      assertThat(new ArrayList<Object>(fast.enumMap.keySet()))
        .as(c.type.getName())
        .isEqualTo(new ArrayList<Object>(mapper.enumMap.keySet()));
    }
  }

  @Test
  public void itRejectsEnumsWithJacksonAnnotations() {
    for (Case c : INELIGIBLE) {
      assertThat(c.eligible()).as(c.type.getName()).isFalse();
    }
  }

  @Test
  public void itRejectsEveryShapeWhereTheMapperDiffers() {
    for (Case c : MAPPER_DIFFERS) {
      assertThat(mapperDiffersFromNames(c)).as(c.type.getName()).isTrue();
      assertThat(c.eligible()).as(c.type.getName()).isFalse();
    }
  }

  @Test
  public void itUsesTheFastPathForPlainEnumsViaPublicApi() {
    assertThat(WireSafeEnum.of(Plain.B).asString()).isEqualTo("B");
    assertThat(WireSafeEnum.fromJson(Plain.class, "C").asEnum()).contains(Plain.C);
    assertThat(WireSafeEnum.fromJson(Plain.class, "c").asEnum()).isEmpty();
    assertThat(WireSafeEnum.of(WithBodies.B).asString()).isEqualTo("B");
    assertThat(WireSafeEnum.fromJson(WithBodies.class, "B").asEnum())
      .contains(WithBodies.B);
  }

  private static boolean mapperDiffersFromNames(Case c) {
    CacheEntries<?> fast = c.fromNames.get();
    CacheEntries<?> mapper;
    try {
      mapper = c.withMapper.get();
    } catch (RuntimeException e) {
      return true;
    }
    return !fast.enumMap.equals(mapper.enumMap) || !fast.jsonMap.equals(mapper.jsonMap);
  }

  @Test
  public void itStillFailsForNonTextualShapes() {
    Throwable t = catchThrowable(() -> WireSafeEnum.of(ShapeNumber.A));
    assertThat(t).isInstanceOf(IllegalStateException.class);
  }
}
