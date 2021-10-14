package io.github.cfstout.jobcoin.workers;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.cfstout.jobcoin.annotations.HouseAccount;
import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.db.MixerAddressTracker;
import io.github.cfstout.jobcoin.models.AddressInfoResponse;
import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;
import io.github.cfstout.jobcoin.models.Transaction;

@Singleton
public class JobCoinDepositedWorker implements Runnable, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JobCoinDepositedWorker.class);
  /**
   * TODO the intent here is to avoid over-withdrawing the deposit addresses for the mixer.
   * This assumes a single worker running, so we can use this set as a locking mechanism to avoid that case.
   * In practice, to scale we'll likely either need to pipeline the transactions to process so that all transactions
   * for the same address take place in the same worker. Alternatively we could avoid the lock altogether and just
   * rely on the consistent backing block chain which would prevent us from overdrawing the source account.
   */
  private final Set<String> inProgressDepositAddresses = Sets.newConcurrentHashSet();
  // todo could add a configuration for the number of threads for this worker, but this should be fine for now
  private final ExecutorService workPool = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("job-coin-deposited").setDaemon(true).build());

  private final MixerAddressTracker mixerAddressTracker;
  private final JobCoinClient jobCoinClient;
  private final String houseAccount;

  @Inject
  public JobCoinDepositedWorker(MixerAddressTracker mixerAddressTracker,
                                JobCoinClient jobCoinClient,
                                @HouseAccount String houseAccount) {
    this.mixerAddressTracker = mixerAddressTracker;
    this.jobCoinClient = jobCoinClient;
    this.houseAccount = houseAccount;
  }

  /**
   * Called periodically to check all addresses we have registered for deposits to the mixer.
   * <p>
   * We should poll to find the transaction logs for these addresses.
   * <p>
   * If there are no newer transactions than the last transaction processed OR an entry for the address in the
   * inProgress map, do nothing.
   * <p>
   * We are assuming that confirmed transactions will only be sent by the API in order. IOW we will never poll the
   * API and get an early transaction than one already seen.
   * <p>
   * Otherwise, there are deposits to process.
   * - Mark the key as in progress
   * - Move all jobcoin from the deposit address to our house account
   * - When the move is confirmed, update the last transaction processed in our tracker
   * - Remove the key from our in-progress tracker
   */
  @Override
  public void run() {
    CompletableFuture<Void> job = CompletableFuture.supplyAsync(mixerAddressTracker::getAllAddressesToWatch, workPool)
        .thenCompose(mixerAddressTrackerEntries -> {
          List<CompletableFuture<?>> updates = mixerAddressTrackerEntries.stream()
              .map(mixerAddressTrackerEntry -> jobCoinClient.getAddressInfo(mixerAddressTrackerEntry.getDepositAddress())
                  .thenCompose(addressInfoResponse -> {
                    if (shouldProcess(mixerAddressTrackerEntry, addressInfoResponse)) {
                      return processLatestTransactions(mixerAddressTrackerEntry, addressInfoResponse);
                    } else {
                      return CompletableFuture.completedFuture(true);
                    }
                  })).collect(Collectors.toList());
          return CompletableFuture.allOf(updates.toArray(new CompletableFuture[]{}));
        });
    try {
      job.get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOG.error("There was an issue checking all deposit addresses!", e);
    }

  }

  private boolean shouldProcess(MixerAddressTrackerEntry mixerAddressTrackerEntry,
                                AddressInfoResponse addressInfoResponse) {
    Instant mostRecentDeposit = getMostRecentDeposit(addressInfoResponse.getTransactions(), mixerAddressTrackerEntry.getDepositAddress());
    return mostRecentDeposit.isAfter(mixerAddressTrackerEntry.getLastTransactionProcessed())
        && !inProgressDepositAddresses.contains(mixerAddressTrackerEntry.getDepositAddress());
  }

  private CompletableFuture<Boolean> processLatestTransactions(MixerAddressTrackerEntry mixerAddressTrackerEntry,
                                                         AddressInfoResponse addressInfoResponse) {
    inProgressDepositAddresses.add(mixerAddressTrackerEntry.getDepositAddress());
    double totalNewDeposits = addressInfoResponse.getTransactions().stream()
        .filter(transaction -> transaction.getToAddress().equals(mixerAddressTrackerEntry.getDepositAddress()))
        .filter(transaction -> transaction.getTimestamp().toInstant().isAfter(mixerAddressTrackerEntry.getLastTransactionProcessed()))
        .mapToDouble(Transaction::getAmountAsDouble)
        .sum();
    if (totalNewDeposits > 0) {
      return jobCoinClient.sendTransaction(mixerAddressTrackerEntry.getDepositAddress(), houseAccount, totalNewDeposits)
          .thenApplyAsync(b -> {
            if (b) {
              Instant mostRecentDeposit = getMostRecentDeposit(addressInfoResponse.getTransactions(), mixerAddressTrackerEntry.getDepositAddress());
              mixerAddressTracker.setLatestTransaction(mixerAddressTrackerEntry.getDepositAddress(), mostRecentDeposit);
              inProgressDepositAddresses.remove(mixerAddressTrackerEntry.getDepositAddress());
            }
            return true;
          }, workPool);
    }
    return CompletableFuture.completedFuture(true);
  }

  @VisibleForTesting
  Instant getMostRecentDeposit(List<Transaction> transactions, String address) {
    // since the API could return a _lot_ of transactions ideally we'd only calculate this once
    return transactions.stream()
        // We only care about deposits to this address
        .filter(transaction -> transaction.getToAddress().equals(address))
        .map(Transaction::getTimestamp)
        .max(ZonedDateTime::compareTo)
        .map(ZonedDateTime::toInstant)
        .orElseGet(() -> Instant.ofEpochSecond(0L));
  }

  @Override
  public void close() throws Exception {
    workPool.shutdown();
  }
}
