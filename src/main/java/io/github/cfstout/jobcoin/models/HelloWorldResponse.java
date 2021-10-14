package io.github.cfstout.jobcoin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.cfstout.jobcoin.clients.JobCoinClient;

public class HelloWorldResponse {
  private final String greeting;

  public HelloWorldResponse(String greeting) {
    this.greeting = greeting;
  }

  @JsonProperty
  public String getGreeting() {
    return greeting;
  }
}
