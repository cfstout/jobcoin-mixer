package io.github.cfstout.jobcoin.clients;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import io.github.cfstout.jobcoin.annotations.JobCoinUrl;
import io.github.cfstout.jobcoin.models.AddressInfoResponse;
import io.github.cfstout.jobcoin.models.Transaction;
import io.github.cfstout.jobcoin.models.TransactionRequest;

public class JobCoinClient {
  private final AsyncHttpClient asyncHttpClient;
  private final String baseUrl;
  private final ObjectMapper objectMapper;

  @Inject
  public JobCoinClient(AsyncHttpClient asyncHttpClient, @JobCoinUrl String baseUrl, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.baseUrl = baseUrl;
    this.objectMapper = objectMapper;
  }

  public CompletableFuture<AddressInfoResponse> getAddressInfo(String address) {
    return get("addresses/" + address, Optional.empty(), new TypeReference<AddressInfoResponse>() {
    });
  }

  public CompletableFuture<List<Transaction>> getAllTransactions() {
    return get("transactions", Optional.empty(), new TypeReference<List<Transaction>>() {
    });
  }

  public CompletableFuture<Boolean> sendTransaction(String fromAddress, String toAddress, double amount) {
    return sendTransaction(new TransactionRequest(fromAddress, toAddress, amount));
  }

  public CompletableFuture<Boolean> sendTransaction(TransactionRequest transactionRequest) {
    try {
      return asyncHttpClient.preparePost(baseUrl + "transactions")
          .addHeader("Content-Type", "application/json")
          .setBody(objectMapper.writeValueAsBytes(transactionRequest))
          .execute()
          .toCompletableFuture()
          .thenApply(response -> response.getStatusCode() >= 200 && response.getStatusCode() < 300);
    } catch (JsonProcessingException e) {
      return CompletableFuture.failedFuture(new RuntimeException(e));
    }
  }

  private <T> CompletableFuture<T> get(String endpoint, Optional<Map<String, String>> queryParameters, TypeReference<T> returnType) {
    BoundRequestBuilder builder = asyncHttpClient.prepareGet(baseUrl + endpoint);
    queryParameters.ifPresent(map -> {
      map.forEach(builder::addQueryParam);
    });
    return builder.execute().toCompletableFuture()
        .thenApply(response -> {
          if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            try {
              return objectMapper.readValue(response.getResponseBody(), returnType);
            } catch (JsonProcessingException e) {
              throw new BadResponseException(response);
            }
          } else {
            throw new BadResponseException(response);
          }
        });
  }
}
