package com.hubspot.immutable.collection.encoding.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.junit.Test;

public class HubspotImmutableStyleTest {

  public static final String INPUT_JSON = "{}";
  public static final ObjectMapper MAPPER = new ObjectMapper()
    .registerModule(new GuavaModule());

  @Test
  public void itCanReadDefaultFromJsonMissingValue() throws JsonProcessingException {
    TestHubspotStyleWithDefault test = MAPPER.readValue(
      INPUT_JSON,
      TestHubspotStyleWithDefault.class
    );
    assertThat(test.getInts()).containsExactly(1);
  }
}
