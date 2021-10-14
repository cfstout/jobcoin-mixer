package io.github.cfstout.jobcoin.models;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class MixerAddressTrackerEntry {
  private final String depositAddress;
  private final List<String> returnAddresses;
  private final Instant lastTransactionProcessed;
}
