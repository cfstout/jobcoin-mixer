package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;

import io.github.cfstout.jobcoin.models.TransactionLogEntry;

public class MixerPayoutTrackerInMemory implements MixerPayoutTracker {
  private final AtomicReference<Instant> latestTransactionProcessed = new AtomicReference<>(Instant.EPOCH);
  private final ConcurrentLinkedQueue<TransactionLogEntry> transactionLog = new ConcurrentLinkedQueue<>();
  @Override
  public Instant getLatestTransactionProcessed() {
    return latestTransactionProcessed.get();
  }

  @Override
  public void logTransaction(String fromAddress, String toAddress, double amount, Instant sourceTimestamp) {
    Instant now = Instant.now();
    maybeUpdateLatestTransaction(sourceTimestamp);
    transactionLog.add(new TransactionLogEntry(
        fromAddress,
        Optional.of(toAddress),
        amount,
        sourceTimestamp,
        now,
        Optional.empty()
    ));
  }

  @Override
  public void logError(String fromAddress, Optional<String> toAddress, double amount, Instant sourceTimestamp, Exception error) {
    Instant now = Instant.now();
    maybeUpdateLatestTransaction(sourceTimestamp);
    transactionLog.add(new TransactionLogEntry(
        fromAddress, toAddress, amount, sourceTimestamp, now, Optional.of(error)
    ));
  }

  @Override
  public List<TransactionLogEntry> getAllTransactions() {
    return ImmutableList.copyOf(transactionLog);
  }

  private void maybeUpdateLatestTransaction(Instant sourceTimestamp) {
    latestTransactionProcessed.getAndUpdate(cur -> sourceTimestamp.isAfter(cur) ? sourceTimestamp : cur);
  }
}
