package io.github.cfstout.jobcoin.workers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.db.MixerAddressTracker;
import io.github.cfstout.jobcoin.db.MixerPayoutTracker;
import io.github.cfstout.jobcoin.db.MixerPayoutTrackerInMemory;
import io.github.cfstout.jobcoin.models.AddressInfoResponse;
import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;
import io.github.cfstout.jobcoin.models.Transaction;
import io.github.cfstout.jobcoin.models.TransactionRequest;

public class MixerPayoutWorkerTest {
  private static final String HOUSE_ACCOUNT = "HOUSE";
  private static final double MIXER_FEE_PERCENT = 5.0;
  private static final int MIXER_PAYOUT_INCREMENT = 10;

  private final MixerPayoutTracker mixerPayoutTracker = new MixerPayoutTrackerInMemory();
  private final MixerAddressTracker mixerAddressTracker = mock(MixerAddressTracker.class);
  private final JobCoinClient jobCoinClient = mock(JobCoinClient.class);
  private final MixerPayoutWorker unit = new MixerPayoutWorker(
      mixerPayoutTracker,
      mixerAddressTracker,
      jobCoinClient,
      HOUSE_ACCOUNT,
      MIXER_FEE_PERCENT,
      MIXER_PAYOUT_INCREMENT
  );

  private static Stream<Arguments> provideArgumentsForPayoutViaMixer() {
    ZonedDateTime now = ZonedDateTime.now();
    return Stream.of(
        Arguments.of(
            new Transaction(now, Optional.of("deposit"), HOUSE_ACCOUNT, "20"),
            new MixerAddressTrackerEntry("deposit", List.of("return"), now.minusMinutes(15L).toInstant()),
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return", 9)
            )
        ),
        Arguments.of(
            new Transaction(now, Optional.of("deposit"), HOUSE_ACCOUNT, "20"),
            new MixerAddressTrackerEntry("deposit", List.of("return1", "return2"), now.minusMinutes(15L).toInstant()),
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 9)
            )
        ),
        Arguments.of(
            new Transaction(now, Optional.of("deposit"), HOUSE_ACCOUNT, "100"),
            new MixerAddressTrackerEntry("deposit", List.of("return1", "return2"), now.minusMinutes(15L).toInstant()),
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 5)
            )
        ),
        Arguments.of(
            new Transaction(now, Optional.of("deposit"), HOUSE_ACCOUNT, "10"),
            new MixerAddressTrackerEntry("deposit", List.of("return"), now.minusMinutes(15L).toInstant()),
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return", 9.5)
            )
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsForPayoutViaMixer")
  public void testPayoutViaMixer(Transaction transaction, MixerAddressTrackerEntry entryForTransactionAddress, List<TransactionRequest> expectedRequests) {
    when(mixerAddressTracker.getEntryForAddress(eq(transaction.getFromAddress().get())))
        .thenReturn(Optional.of(entryForTransactionAddress));

    ArgumentCaptor<TransactionRequest> transactionRequestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
    when(jobCoinClient.sendTransaction(transactionRequestCaptor.capture())).thenReturn(CompletableFuture.completedFuture(true));

    unit.payoutViaMixer(transaction);

    Assertions.assertThat(transactionRequestCaptor.getAllValues()).hasSameElementsAs(expectedRequests);
  }

  @Test
  public void testMixerPayout() throws ExecutionException, InterruptedException, TimeoutException {
    ZonedDateTime now = ZonedDateTime.now();
    Transaction transaction1 = new Transaction(now.minusMinutes(10), Optional.of("deposit"), HOUSE_ACCOUNT, "10");
    Transaction transaction2 = new Transaction(now.minusMinutes(5), Optional.of("deposit"), HOUSE_ACCOUNT, "20");
    List<Transaction> originalTransactions = List.of(
        transaction1
    );
    AddressInfoResponse firstResponse = new AddressInfoResponse("10", originalTransactions);
    List<Transaction> newTransactions = List.of(transaction1, transaction2);
    AddressInfoResponse secondResponse = new AddressInfoResponse("30", newTransactions);
    when(jobCoinClient.getAddressInfo(eq(HOUSE_ACCOUNT)))
        .thenReturn(CompletableFuture.completedFuture(firstResponse))
        .thenReturn(CompletableFuture.completedFuture(secondResponse));


    when(mixerAddressTracker.getEntryForAddress(eq("deposit")))
        .thenReturn(Optional.of(new MixerAddressTrackerEntry(
            "deposit",
            List.of("return1", "return2"),
            now.minusMinutes(15L).toInstant()
        )));

    ArgumentCaptor<TransactionRequest> transactionRequestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
    when(jobCoinClient.sendTransaction(transactionRequestCaptor.capture())).thenReturn(CompletableFuture.completedFuture(true));

    // We're going to run the worker 2 times, the first should happen at basically the same time, but the first should
    // need to complete before the second one or else we'll get multiple transactions sent
    ExecutorService workPool = Executors.newFixedThreadPool(2);
    Future<?> future1 = workPool.submit(unit);
    Future<?> future2 = workPool.submit(unit);

    future1.get();
    future2.get();
//    future1.get(5, TimeUnit.SECONDS);
//    future2.get(5, TimeUnit.SECONDS);

    List<TransactionRequest> expectedSentTransactions = List.of(
        new TransactionRequest(HOUSE_ACCOUNT, "return1", 9.5),
        new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
        new TransactionRequest(HOUSE_ACCOUNT, "return2", 9)
    );
    Assertions.assertThat(transactionRequestCaptor.getAllValues()).hasSameElementsAs(expectedSentTransactions);
  }

  private static Stream<Arguments> provideTestNewMixerPayoutArgs() {
    return Stream.of(
        Arguments.of(
            List.of("return1", "return2"),
            25,
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 15)
            )
        ),
        Arguments.of(
            List.of("return1", "return2"),
            20,
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10)
            )
        ),
        Arguments.of(
            List.of("return1", "return2"),
            20.5,
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10.5)
            )
        ),
        Arguments.of(
            List.of("return1", "return2"),
            20.5000000000000000002,
            List.of(
                new TransactionRequest(HOUSE_ACCOUNT, "return1", 10),
                new TransactionRequest(HOUSE_ACCOUNT, "return2", 10.5000000000000000002)
            )
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideTestNewMixerPayoutArgs")
  public void testNewMixerPayout(List<String> returnAddresses, double amount, List<TransactionRequest> expected) {
    List<TransactionRequest> methodResult = unit.distributeFunds(returnAddresses, amount);

    Assertions.assertThat(methodResult).hasSameElementsAs(expected);
  }
}
