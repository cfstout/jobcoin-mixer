package io.github.cfstout.jobcoin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
