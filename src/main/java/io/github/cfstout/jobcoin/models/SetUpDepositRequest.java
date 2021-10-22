package io.github.cfstout.jobcoin.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SetUpDepositRequest {
  @JsonProperty
  List<String> returnAddresses;

  @JsonCreator
  public SetUpDepositRequest(@JsonProperty("returnAddresses") List<String> returnAddresses) {
    this.returnAddresses = returnAddresses;
  }
}
