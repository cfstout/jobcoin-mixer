package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.Optional;

public interface MixerPayoutTracker {
  Instant getLatestTransactionProcessed();

  void logTransaction(String fromAddress, String toAddress, double amount);

  default void logError(String fromAddress, double amount) {
    logError(fromAddress, Optional.empty(), amount);
  }
  void logError(String fromAddress, Optional<String> toAddress, double amount);
}
