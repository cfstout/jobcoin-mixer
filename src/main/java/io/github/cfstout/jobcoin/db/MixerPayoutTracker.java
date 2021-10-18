package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.cfstout.jobcoin.models.Transaction;
import io.github.cfstout.jobcoin.models.TransactionLogEntry;

public interface MixerPayoutTracker {
  Instant getLatestTransactionProcessed();

  void logTransaction(String fromAddress, String toAddress, double amount, Instant sourceTimestamp);

  default void logError(String fromAddress, double amount, Instant sourceTimestamp, Exception error) {
    logError(fromAddress, Optional.empty(), amount, sourceTimestamp, error);
  }
  void logError(String fromAddress, Optional<String> toAddress, double amount, Instant sourceTimestamp, Exception error);

  List<TransactionLogEntry> getAllTransactions();

  default List<TransactionLogEntry> getAllErrors() {
    return getAllTransactions().stream()
        .filter(transactionLogEntry -> transactionLogEntry.getError().isPresent())
        .collect(Collectors.toList());
  }

  default List<TransactionLogEntry> getAllSuccesses() {
    return getAllTransactions().stream()
        .filter(transactionLogEntry -> transactionLogEntry.getError().isEmpty())
        .collect(Collectors.toList());
  }
}
