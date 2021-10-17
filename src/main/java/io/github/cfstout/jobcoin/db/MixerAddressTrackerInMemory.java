package io.github.cfstout.jobcoin.db;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;

@Singleton
public class MixerAddressTrackerInMemory implements MixerAddressTracker, AutoCloseable {
  private final ConcurrentHashMap<String, MixerAddressTrackerEntry> database = new ConcurrentHashMap<>();
  private final ExecutorService initializationPool = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder()
          .setNameFormat("MixerAddressTrackerInMemory-init")
          .setDaemon(true)
          .build()
  );


  @Inject
  public MixerAddressTrackerInMemory(JobCoinClient jobCoinClient) {
    initializationPool.submit(() -> initialize(jobCoinClient));
  }

  /**
   * We should be able to initialize this from the transaction log of the house account.
   * Obviously this won't keep the state for addresses that are set up, but never run, but
   * it's just a bit of an optimization to help with testing. Eventualy we should replace
   * this implementation with a database to have persistent data across startups.
   */
  private void initialize(JobCoinClient jobCoinClient) {
    // todo
  }

  @Override
  public void initializeMixerMapping(String depositAddress, List<String> returnAddresses) {
    database.put(depositAddress, new MixerAddressTrackerEntry(depositAddress, returnAddresses, Instant.ofEpochSecond(0)));
  }

  @Override
  public List<MixerAddressTrackerEntry> getAllAddressesToWatch() {
    return ImmutableList.copyOf(database.values());
  }

  @Override
  public Optional<MixerAddressTrackerEntry> getEntryForAddress(String depositAddress) {
    return Optional.ofNullable(database.get(depositAddress));
  }

  @Override
  public void setLatestTransaction(String depositAddress, Instant lastestTransaction) {
    if (!database.containsKey(depositAddress)) {
      throw new IllegalArgumentException("Trying to update latest transaction for an un-initialized entry!!");
    }
    database.computeIfPresent(depositAddress,
        (address, existing) ->
            new MixerAddressTrackerEntry(address, existing.getReturnAddresses(), lastestTransaction)
    );
  }

  @Override
  public void close() throws Exception {
    initializationPool.shutdown();
  }
}
