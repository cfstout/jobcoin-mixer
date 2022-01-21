package io.github.cfstout.jobcoin.workers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.cfstout.jobcoin.annotations.HouseAccount;
import io.github.cfstout.jobcoin.annotations.MixerPayoutIncrement;
import io.github.cfstout.jobcoin.annotations.TransactionFeePercent;
import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.db.MixerAddressTracker;
import io.github.cfstout.jobcoin.db.MixerPayoutTracker;
import io.github.cfstout.jobcoin.models.AddressInfoResponse;
import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;
import io.github.cfstout.jobcoin.models.Transaction;
import io.github.cfstout.jobcoin.models.TransactionRequest;

@Singleton
public class MixerPayoutWorker implements Runnable, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(MixerPayoutWorker.class);

  private final ExecutorService workPool = Executors.newFixedThreadPool(4,
      new ThreadFactoryBuilder()
          .setNameFormat("mixer-payout")
          .setDaemon(true)
          .build()
  );

  private final MixerPayoutTracker mixerPayoutTracker;
  private final MixerAddressTracker mixerAddressTracker;
  private final JobCoinClient jobCoinClient;
  private final String houseAccount;
  private final double mixerTransactionFeePercent;
  private final int mixerPayoutIncrement;


  @Inject
  public MixerPayoutWorker(MixerPayoutTracker mixerPayoutTracker,
                           MixerAddressTracker mixerAddressTracker,
                           JobCoinClient jobCoinClient,
                           @HouseAccount String houseAccount,
                           @TransactionFeePercent double mixerTransactionFeePercent,
                           @MixerPayoutIncrement int mixerPayoutIncrement) {
    this.mixerPayoutTracker = mixerPayoutTracker;
    this.mixerAddressTracker = mixerAddressTracker;
    this.jobCoinClient = jobCoinClient;
    this.houseAccount = houseAccount;
    this.mixerTransactionFeePercent = mixerTransactionFeePercent;
    this.mixerPayoutIncrement = mixerPayoutIncrement;
  }

  // todo I think we could add a concurrent deque that we add to in this method and then have another background thread pull work off that queue to do the actual transactions
  // Only allow one thread to run this method at a time
  @Override
  public synchronized void run() {
    LOG.info("Setting up payouts for new deposits to the house account");
    CompletableFuture<AddressInfoResponse> addressInfoFuture = jobCoinClient.getAddressInfo(houseAccount);
    Instant latestTransactionProcessed = mixerPayoutTracker.getLatestTransactionProcessed();
    CompletableFuture<Void> job = addressInfoFuture.thenCompose(addressInfoResponse -> {
      List<CompletableFuture<Void>> collect = addressInfoResponse.getTransactions().stream()
          .filter(transaction -> transaction.getFromAddress().isPresent())
          .filter(transaction -> transaction.getToAddress().equals(houseAccount))
          .filter(transaction -> transaction.getTimestamp().toInstant().isAfter(latestTransactionProcessed))
          .map(this::payoutViaMixer)
          .collect(Collectors.toList());
      return CompletableFuture.allOf(collect.toArray(new CompletableFuture[]{}));
    });
    try {
      job.get(30, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOG.error("Error processing payouts", e);
    }
  }

  @VisibleForTesting
  CompletableFuture<Void> payoutViaMixer(Transaction transactionToProcess) {
    // Checked in our filter in the run statement above
    String mixerDepositAddress = transactionToProcess.getFromAddress().get();
    Optional<MixerAddressTrackerEntry> maybeEntry = mixerAddressTracker.getEntryForAddress(mixerDepositAddress);
    Instant sourceTimestamp = transactionToProcess.getTimestamp().toInstant();
    if (maybeEntry.isEmpty() || maybeEntry.get().getReturnAddresses().isEmpty()) {
      mixerPayoutTracker.logError(
          mixerDepositAddress,
          transactionToProcess.getAmountAsDouble(),
          sourceTimestamp,
          new IllegalStateException("No mixer addresses configured"));
      return CompletableFuture.completedFuture(null);
    }
    MixerAddressTrackerEntry mixerAddressTrackerEntry = maybeEntry.get();
    LOG.info("Paying out to configured payout addresses '{}' via the mixer", mixerAddressTrackerEntry.getReturnAddresses());
    double amountToDeposit = transactionToProcess.getAmountAsDouble() * (1 - mixerTransactionFeePercent / 100);
    int intermediateTransactionNumber = 0;
    int numberOfAddresses = mixerAddressTrackerEntry.getReturnAddresses().size();
    Builder<CompletableFuture<?>> transactionFutures = ImmutableList.builder();
    while (amountToDeposit > 0) {
      double amountToDepositNow = Math.min(mixerPayoutIncrement, amountToDeposit);
      TransactionRequest request = new TransactionRequest(
          houseAccount,
          mixerAddressTrackerEntry
              .getReturnAddresses()
              .get(intermediateTransactionNumber % numberOfAddresses),
          amountToDepositNow
      );
      transactionFutures.add(
          jobCoinClient.sendTransaction(request).thenApplyAsync(success -> {
            if (success) {
              // Need to double-check, but this should reference the correct value for each loop iteration
              mixerPayoutTracker.logTransaction(
                  request.getFromAddress(),
                  request.getToAddress(),
                  request.getAmountAsDouble(),
                  sourceTimestamp
              );
            } else {
              mixerPayoutTracker.logError(
                  request.getFromAddress(),
                  Optional.of(request.getToAddress()),
                  request.getAmountAsDouble(),
                  sourceTimestamp,
                  new RuntimeException("Transaction failed")
              );
            }
            return success;
          }, workPool)
      );
      intermediateTransactionNumber++;
      amountToDeposit -= amountToDepositNow;
    }
    return CompletableFuture.allOf(transactionFutures.build().toArray(new CompletableFuture[]{}));
  }

  @VisibleForTesting
  List<TransactionRequest> distributeFunds(List<String> returnAddresses, double amount) {
    Builder<TransactionRequest> requests = ImmutableList.builder();
    // 95 to deposit
    // 3 address
    // 3 payments of 30 each + extra 5
    double amtLeft = amount % mixerPayoutIncrement;
    for (String address: returnAddresses) {
      int numAvgPayouts = (int) (amount / mixerPayoutIncrement / returnAddresses.size());
      TransactionRequest request = new TransactionRequest(
          houseAccount,
          address,
          numAvgPayouts * mixerPayoutIncrement + (address.equals(returnAddresses.get(returnAddresses.size() - 1)) ? amtLeft : 0)
      );
      requests.add(request);
    }

    return requests.build();
  }

  @Override
  public void close() {
    workPool.shutdown();
  }
}
