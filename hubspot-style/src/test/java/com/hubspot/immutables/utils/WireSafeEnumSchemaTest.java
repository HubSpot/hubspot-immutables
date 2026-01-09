package com.hubspot.immutables.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class WireSafeEnumSchemaTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public enum TestEnum {
    ONE,
    TWO,
    THREE;

    @JsonValue
    public String customValue() {
      return "CUSTOM_" + name();
    }
  }

  public class TestClass {

    public TestEnum enumField;
    public WireSafeEnum<TestEnum> wireSafeEnumField;

    @JsonCreator
    public TestClass(
      @JsonProperty("enumField") TestEnum enumField,
      @JsonProperty("wireSafeEnumField") WireSafeEnum<TestEnum> wireSafeEnumField
    ) {
      this.enumField = enumField;
      this.wireSafeEnumField = wireSafeEnumField;
    }
  }

  @Test
  public void itAddsEnumValuesToJsonSchema() throws JsonMappingException {
    Map<String, Set<String>> capturedEnumValues = new HashMap<>();

    SerializerProvider provider = MAPPER.getSerializerProviderInstance();
    JsonSerializer<?> serializer = MAPPER
      .getSerializerFactory()
      .createSerializer(provider, MAPPER.constructType(TestClass.class));

    serializer.acceptJsonFormatVisitor(
      new ObjectVisitor(provider, capturedEnumValues),
      MAPPER.constructType(TestClass.class)
    );

    assertThat(capturedEnumValues.get("enumField"))
      .isEqualTo(ImmutableSet.of("CUSTOM_ONE", "CUSTOM_TWO", "CUSTOM_THREE"));
    // make sure that WireSafeEnum adds enum values to schema, and that it respects custom @JsonValue method
    assertThat(capturedEnumValues.get("wireSafeEnumField"))
      .isEqualTo(ImmutableSet.of("CUSTOM_ONE", "CUSTOM_TWO", "CUSTOM_THREE"));
  }

  private static class ObjectVisitor extends JsonFormatVisitorWrapper.Base {

    private final Map<String, Set<String>> capturedEnumValues;

    ObjectVisitor(
      SerializerProvider provider,
      Map<String, Set<String>> capturedEnumValues
    ) {
      super(provider);
      this.capturedEnumValues = capturedEnumValues;
    }

    @Override
    public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
      return new JsonObjectFormatVisitor.Base(getProvider()) {
        @Override
        public void property(BeanProperty prop) throws JsonMappingException {
          visitProperty(prop);
        }

        @Override
        public void optionalProperty(BeanProperty prop) throws JsonMappingException {
          visitProperty(prop);
        }

        private void visitProperty(BeanProperty prop) throws JsonMappingException {
          JsonSerializer<?> serializer = getProvider()
            .findValueSerializer(prop.getType(), prop);

          serializer.acceptJsonFormatVisitor(
            new PropertyVisitor(getProvider(), prop.getName(), capturedEnumValues),
            prop.getType()
          );
        }
      };
    }
  }

  private static class PropertyVisitor extends JsonFormatVisitorWrapper.Base {

    private final String propertyName;
    private final Map<String, Set<String>> capturedEnumValues;

    PropertyVisitor(
      SerializerProvider provider,
      String propertyName,
      Map<String, Set<String>> capturedEnumValues
    ) {
      super(provider);
      this.propertyName = propertyName;
      this.capturedEnumValues = capturedEnumValues;
    }

    @Override
    public JsonStringFormatVisitor expectStringFormat(JavaType type) {
      return new JsonStringFormatVisitor.Base() {
        @Override
        public void enumTypes(Set<String> enums) {
          capturedEnumValues.put(propertyName, enums);
        }
      };
    }
  }
}
