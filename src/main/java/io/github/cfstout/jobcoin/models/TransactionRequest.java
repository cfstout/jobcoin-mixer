package io.github.cfstout.jobcoin.models;

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

  public TransactionRequest(String fromAddress, String toAddress, double amount) {
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
    this.amount = String.valueOf(amount);
  }
}
