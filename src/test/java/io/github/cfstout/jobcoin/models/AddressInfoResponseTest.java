package io.github.cfstout.jobcoin.models;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cfstout.jobcoin.helpers.ObjectMapperProvider;

public class AddressInfoResponseTest {
  private static final Logger LOG = LoggerFactory.getLogger(AddressInfoResponseTest.class);

  @Test
  public void testAddressInfoDeserialization() throws JsonProcessingException {
    String asString = "{\"balance\":\"37.5\",\"transactions\":[{\"timestamp\":\"2021-10-12T21:53:33.569Z\",\"toAddress\":\"Alice\",\"amount\":\"50\"},{\"timestamp\":\"2021-10-12T21:53:33.577Z\",\"fromAddress\":\"Alice\",\"toAddress\":\"Bob\",\"amount\":\"12.5\"}]}\n" +
        "! }";
    ObjectMapper mapper = ObjectMapperProvider.getMapper();
    AddressInfoResponse addressInfoResponse = mapper.readValue(asString, AddressInfoResponse.class);
    LOG.info("Address info response: {}", addressInfoResponse);
    LOG.info("Re-serialized: {}", mapper.writeValueAsString(addressInfoResponse));
  }

}
