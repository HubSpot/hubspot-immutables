package com.hubspot.immutables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableSet;
import com.hubspot.immutables.model.WidgetGuava;
import com.hubspot.immutables.validation.InvalidImmutableStateException;
import com.hubspot.immutables.model.Foo;
import com.hubspot.immutables.model.FooEgg;
import com.hubspot.immutables.model.ImmutableWithModifiable;
import com.hubspot.immutables.model.ModifiableImmutableWithModifiable;
import com.hubspot.immutables.model.NormalizedWidget;
import com.hubspot.immutables.model.RosettaSprocket;
import com.hubspot.immutables.model.RosettaSprocketIF;
import com.hubspot.immutables.model.Sprocket;
import com.hubspot.immutables.model.Widget;
import com.hubspot.immutables.model.Wrapper;
import com.hubspot.rosetta.Rosetta;

public class ImmutablesTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void itUsesImmutableEncodings() {
    WidgetGuava widgetGuava = WidgetGuava.builder()
        .setAnInt(1)
        .addSomeOtherVals("test", "test2")
        .build();

    assertThat(widgetGuava.getSomeVals()).isInstanceOf(ImmutableSet.class);
  }

  @Test
  public void itGeneratesFromAbstract() {
    Sprocket sprocket = Sprocket.builder()
        .setAnInt(1)
        .setAnOptionalString("isHere")
        .build();

    assertThat(sprocket.getAnInt()).isEqualTo(1);
    assertThat(sprocket.getAnOptionalString()).contains("isHere");
  }

  @Test
  public void itGeneratesFromInterface() {
    Widget widget = Widget.builder()
        .setAnInt(1)
        .setAnOptionalString("isHere")
        .build();

    assertThat(widget.getAnInt()).isEqualTo(1);
    assertThat(widget.getAnOptionalString()).contains("isHere");
  }

  @Test
  public void itThrowsInvalidStateOnValidation() {
    assertThatThrownBy(() -> Widget.builder().setAnInt(-1).build()).hasMessage("int -1 must be greater than 0").isInstanceOf(InvalidImmutableStateException.class);
    assertThatThrownBy(() -> Widget.builder().setAnInt(11).build()).hasMessage("int must be less than 10").isInstanceOf(InvalidImmutableStateException.class);
  }

  @Test
  public void itDeserializesJson() throws IOException {
    String inputJson = "{\"id\":1,\"hubSpotId\":12,\"name\":\"test\",\"somethingWithLongName\":\"a long thing\",\"blueSprocket\":true}";
    RosettaSprocket rosettaSprocket = objectMapper.readValue(inputJson, RosettaSprocket.class);
    assertThat(rosettaSprocket.getId()).isEqualTo(1);
    assertThat(rosettaSprocket.getHubSpotId()).isEqualTo(12);
    assertThat(rosettaSprocket.getName()).isEqualTo("test");
    assertThat(rosettaSprocket.getSomethingWithLongName()).isEqualTo("a long thing");
    assertThat(rosettaSprocket.isBlueSprocket()).isTrue();

    JsonNode output = objectMapper.valueToTree(rosettaSprocket);
    JsonNode original = objectMapper.readTree(inputJson);
    assertThat(output).isEqualTo(original);
  }

  @Test
  public void itDeserializesRosettaJson() throws IOException {
    String inputJson = "{\"id\":1,\"hubspot_id\":12,\"name\":\"test\",\"something_with_long_name\":\"a long thing\",\"blue_sprocket\":true}";
    RosettaSprocket rosettaSprocket = Rosetta.getMapper().readValue(inputJson, RosettaSprocket.class);
    assertThat(rosettaSprocket.getId()).isEqualTo(1);
    assertThat(rosettaSprocket.getHubSpotId()).isEqualTo(12);
    assertThat(rosettaSprocket.getName()).isEqualTo("test");
    assertThat(rosettaSprocket.getSomethingWithLongName()).isEqualTo("a long thing");
    assertThat(rosettaSprocket.isBlueSprocket()).isTrue();

    JsonNode output = Rosetta.getMapper().valueToTree(rosettaSprocket);
    JsonNode original = Rosetta.getMapper().readTree(inputJson);
    assertThat(output).isEqualTo(original);
  }

  @Test
  public void itDeserializesAnInterface() throws IOException {
    String inputJson = "{\"id\":1,\"hubSpotId\":12,\"name\":\"test\",\"somethingWithLongName\":\"a long thing\",\"blueSprocket\":true}";
    RosettaSprocketIF rosettaSprocketIF = objectMapper.readValue(inputJson, RosettaSprocketIF.class);
    assertThat(rosettaSprocketIF.getId()).isEqualTo(1);
    assertThat(rosettaSprocketIF.getHubSpotId()).isEqualTo(12);
    assertThat(rosettaSprocketIF.getName()).isEqualTo("test");
    assertThat(rosettaSprocketIF.getSomethingWithLongName()).isEqualTo("a long thing");
    assertThat(rosettaSprocketIF.isBlueSprocket()).isTrue();

    JsonNode output = objectMapper.valueToTree(rosettaSprocketIF);
    JsonNode original = objectMapper.readTree(inputJson);
    assertThat(output).isEqualTo(original);
  }

  @Test
  public void itDeserializesAnInterfaceFromRosetta() throws IOException {
    String inputJson = "{\"id\":1,\"hubspot_id\":12,\"name\":\"test\",\"something_with_long_name\":\"a long thing\",\"blue_sprocket\":true}";
    RosettaSprocketIF rosettaSprocketIF = Rosetta.getMapper().readValue(inputJson, RosettaSprocketIF.class);
    assertThat(rosettaSprocketIF.getId()).isEqualTo(1);
    assertThat(rosettaSprocketIF.getHubSpotId()).isEqualTo(12);
    assertThat(rosettaSprocketIF.getName()).isEqualTo("test");
    assertThat(rosettaSprocketIF.getSomethingWithLongName()).isEqualTo("a long thing");
    assertThat(rosettaSprocketIF.isBlueSprocket()).isTrue();

    JsonNode output = Rosetta.getMapper().valueToTree(rosettaSprocketIF);
    JsonNode original = Rosetta.getMapper().readTree(inputJson);
    assertThat(output).isEqualTo(original);
  }

  @Test
  public void itSupportsWrappedJson() throws IOException {
    String inputJson = "{\"id\": 12,\"someString\": \"something\",\"anInteger\": 8}";
    Wrapper wrapper = objectMapper.readValue(inputJson, Wrapper.class);
    assertThat(wrapper.getId()).isEqualTo(12);
    assertThat(wrapper.getWrapped().getSomeString()).isEqualTo("something");
    assertThat(wrapper.getWrapped().getAnInteger()).isEqualTo(8);

    JsonNode output = objectMapper.valueToTree(wrapper);
    JsonNode original = objectMapper.readTree(inputJson);
    assertThat(output).isEqualTo(original);
  }

  @Test
  public void itSupportsNormalization() {
    NormalizedWidget widget = NormalizedWidget.builder()
        .setValue(BigDecimal.TEN)
        .setOptionalValue(BigDecimal.TEN)
        .build();

    BigDecimal scaledValue = BigDecimal.TEN.setScale(6, BigDecimal.ROUND_HALF_UP);

    assertThat(widget.getValue()).isEqualTo(scaledValue);
    assertThat(widget.getOptionalValue()).contains(scaledValue);
  }

  @Test
  public void itParsesAModifiable() throws IOException {
    String inputJson = "{\"names\": [\"Bill\", \"Bob\"], \"description\": \"Foo\"}";
    assertThatThrownBy(() -> objectMapper.readValue(inputJson, ImmutableWithModifiable.class))
        .hasMessageStartingWith("Instantiation of [simple type, class com.hubspot.immutables.model.ImmutableWithModifiable] value failed")
        .hasMessageContaining("Cannot build ImmutableWithModifiable, some of required attributes are not set [id]")
        .isInstanceOf(JsonMappingException.class);
    ModifiableImmutableWithModifiable modifiable = objectMapper.readValue(inputJson, ModifiableImmutableWithModifiable.class);
    assertThatThrownBy(modifiable::toImmutable)
        .hasMessage("ImmutableWithModifiable in not initialized, some of the required attributes are not set [id]")
        .isInstanceOf(IllegalStateException.class);
    ImmutableWithModifiable exampleImmutable = modifiable.setId(1).toImmutable();
    assertThat(exampleImmutable.getId()).isEqualTo(1);
    assertThat(exampleImmutable.getDescription()).isEqualTo("Foo");
    assertThat(exampleImmutable.getNames()).contains("Bill", "Bob");
  }

  @Test
  public void itParsesEggPattern() throws IOException {
    String inputJson = "{\"name\": \"EggTest\"}";
    assertThatThrownBy(() -> objectMapper.readValue(inputJson, Foo.class))
        .hasMessageStartingWith("Instantiation of [simple type, class com.hubspot.immutables.model.Foo] value failed")
        .hasMessageContaining("Cannot build Foo, some of required attributes are not set [id]")
        .isInstanceOf(JsonMappingException.class);

    FooEgg fooEgg = objectMapper.readValue(inputJson, FooEgg.class);

    Foo foo = Foo.builder()
        .from(fooEgg)
        .setId(1)
        .build();

    assertThat(foo.getId()).isEqualTo(1);
    assertThat(foo.getName()).isEqualTo("EggTest");
  }
}
