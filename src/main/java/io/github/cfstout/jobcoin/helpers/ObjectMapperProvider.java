package io.github.cfstout.jobcoin.helpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperProvider {
  private static ObjectMapper objectMapper;

  private ObjectMapperProvider() {
  }

  public static ObjectMapper getMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule());

    }
    return objectMapper;
  }
}
