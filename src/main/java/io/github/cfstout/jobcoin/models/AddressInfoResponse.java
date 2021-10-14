package io.github.cfstout.jobcoin.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AddressInfoResponse {

  @JsonCreator
  public AddressInfoResponse(@JsonProperty("balance") String balance, @JsonProperty("transactions") List<Transaction> transactions) {
    this.balance = balance;
    this.transactions = transactions;
  }

  @JsonProperty
  private final String balance;

  @JsonProperty
  private final List<Transaction> transactions;

  @JsonIgnore
  public double getBalanceAsDouble() {
    return Double.parseDouble(balance);
  }
}
