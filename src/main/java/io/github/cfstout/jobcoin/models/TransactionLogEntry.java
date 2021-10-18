package io.github.cfstout.jobcoin.models;

import java.time.Instant;
import java.util.Optional;

import lombok.Data;

@Data
public class TransactionLogEntry {
  private final String fromAddress;
  private final Optional<String> toAddress;
  private final double amount;
  private final Instant sourceTimestamp;
  private final Instant timestamp;
  private final Optional<Exception> error;
}
