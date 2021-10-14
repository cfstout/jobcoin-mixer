package io.github.cfstout.jobcoin.models;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Transaction {
  @JsonCreator
  public Transaction(@JsonProperty("timestamp") ZonedDateTime timestamp,
                     @JsonProperty("fromAddress") Optional<String> fromAddress,
                     @JsonProperty("toAddress") String toAddress,
                     @JsonProperty("amount") String amount) {
    this.timestamp = timestamp;
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
    this.amount = amount;
  }

  private final ZonedDateTime timestamp;

  private final Optional<String> fromAddress;

  private final String toAddress;

  private final String amount;

  public double getAmountAsDouble() {
    return Double.parseDouble(amount);
  }
}
