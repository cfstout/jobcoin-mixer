package io.github.cfstout.jobcoin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SetUpDepositResponse {
  @JsonProperty
  String depositAddress;

  @JsonCreator
  public SetUpDepositResponse(@JsonProperty("depositAddress") String depositAddress) {
    this.depositAddress = depositAddress;
  }
}
