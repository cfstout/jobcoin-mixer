package io.github.cfstout.jobcoin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TransactionRequest {
  @JsonProperty
  private final String fromAddress;

  @JsonProperty
  private final String toAddress;

  @JsonProperty
  private final String amount;

  @JsonIgnore
  public double getAmountAsDouble() {
    return Double.parseDouble(amount);
  }

  public TransactionRequest(String fromAddress, String toAddress, double amount) {
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
    this.amount = String.valueOf(amount);
  }

  @JsonCreator
  public TransactionRequest(@JsonProperty("fromAddress") String fromAddress,
                            @JsonProperty("toAddress") String toAddress,
                            @JsonProperty("amount") String amount) {
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
    this.amount = amount;
  }
}
