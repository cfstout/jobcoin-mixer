package io.github.cfstout.jobcoin.clients;

import org.asynchttpclient.Response;

public class BadResponseException extends RuntimeException {
  private final Response response;

  public BadResponseException(Response response) {
    this.response = response;
  }

  @Override
  public String getMessage() {
    return "Got bad response" + response.toString();
  }
}
