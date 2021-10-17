package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;

public interface MixerAddressTracker {
  void initializeMixerMapping(String depositAddress, List<String> returnAddresses);

  List<MixerAddressTrackerEntry> getAllAddressesToWatch();

  Optional<MixerAddressTrackerEntry> getEntryForAddress(String depositAddress);

  void setLatestTransaction(String depositAddress, Instant lastestTransaction);
}
