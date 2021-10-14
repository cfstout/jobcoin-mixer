package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.List;

import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;

public interface MixerAddressTracker {
  List<MixerAddressTrackerEntry> getAllAddressesToWatch();

  void setLatestTransaction(String depositAddress, Instant lastestTransaction);
}
