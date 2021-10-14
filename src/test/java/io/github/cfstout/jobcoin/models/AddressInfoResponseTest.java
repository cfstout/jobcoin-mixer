package io.github.cfstout.jobcoin.models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.github.cfstout.jobcoin.helpers.ObjectMapperProvider;

public class AddressInfoResponseTest {
  private static final Logger LOG = LoggerFactory.getLogger(AddressInfoResponseTest.class);

  @Test
  public void testAddressInfoDeserialization() throws JsonProcessingException {
    String asString = "{\"balance\":\"37.5\",\"transactions\":[{\"timestamp\":\"2021-10-12T21:53:33.569Z\",\"toAddress\":\"Alice\",\"amount\":\"50\"},{\"timestamp\":\"2021-10-12T21:53:33.577Z\",\"fromAddress\":\"Alice\",\"toAddress\":\"Bob\",\"amount\":\"12.5\"}]}";
    ObjectMapper mapper = ObjectMapperProvider.getMapper();
    AddressInfoResponse addressInfoResponse = mapper.readValue(asString, AddressInfoResponse.class);
    LOG.debug("Address info response: {}", addressInfoResponse);
    Assertions.assertThat(addressInfoResponse).isEqualTo(new AddressInfoResponse("37.5", ImmutableList.of(
        new Transaction(
            ZonedDateTime.of(LocalDateTime.of(2021, 10, 12, 21, 53, 33, 569000000), ZoneId.of("UTC")),
            Optional.empty(),
            "Alice",
            "50"
        ),
        new Transaction(
            ZonedDateTime.of(LocalDateTime.of(2021, 10, 12, 21, 53, 33, 577000000), ZoneId.of("UTC")),
            Optional.of("Alice"),
            "Bob",
            "12.5"
        )
    )));
    LOG.debug("Re-serialized: {}", mapper.writeValueAsString(addressInfoResponse));
  }

}
