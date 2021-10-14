package io.github.cfstout.jobcoin.workers;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.db.MixerAddressTracker;
import io.github.cfstout.jobcoin.models.AddressInfoResponse;
import io.github.cfstout.jobcoin.models.MixerAddressTrackerEntry;
import io.github.cfstout.jobcoin.models.Transaction;

class JobCoinDepositedWorkerTest {
  private static final ZoneId UTC = ZoneId.of("UTC");

  private final MixerAddressTracker mixerAddressTracker = Mockito.mock(MixerAddressTracker.class);
  private final JobCoinClient jobCoinClient = Mockito.mock(JobCoinClient.class);
  private final JobCoinDepositedWorker unit = new JobCoinDepositedWorker(mixerAddressTracker, jobCoinClient, "house");


  private static Stream<Arguments> provideArgumentsForShouldProcess() {
    Instant now = Instant.now();
    return Stream.of(
        // New deposit
        Arguments.of(
            new MixerAddressTrackerEntry("deposit", List.of(), now.minus(5, MINUTES)),
            new AddressInfoResponse("100", List.of(new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.empty(), "deposit", "50"))),
            true
        ),
        // No new deposit
        Arguments.of(
            new MixerAddressTrackerEntry("deposit", List.of(), now.minus(5, MINUTES)),
            new AddressInfoResponse("100", List.of(new Transaction(ZonedDateTime.ofInstant(now.minus(10, MINUTES), UTC), Optional.empty(), "deposit", "50"))),
            false
        ),
        // New withdrawl
        Arguments.of(
            new MixerAddressTrackerEntry("deposit", List.of(), now.minus(5, MINUTES)),
            new AddressInfoResponse("100", List.of(new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("deposit"), "withdrawal", "50"))),
            false
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsForShouldProcess")
  public void testShouldProcess(MixerAddressTrackerEntry entry, AddressInfoResponse response, boolean expectedResult) {
    Assertions.assertThat(unit.shouldProcess(entry, response)).isEqualTo(expectedResult);
  }

  private static Stream<Arguments> provideArgumentsForMostRecentDeposits() {
    Instant now = Instant.now();
    return Stream.of(
        // In order deposits
        Arguments.of(
            List.of(
                new Transaction(ZonedDateTime.ofInstant(now.minus(2, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now.minus(1, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("other"), "deposit", "50")
            ),
            "deposit",
            now
        ),
        // Out of order deposits
        Arguments.of(
            List.of(
                new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now.minus(1, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now.minus(2, MINUTES), UTC), Optional.of("other"), "deposit", "50")
            ),
            "deposit",
            now
        ),
        // Most recent transaction is a withdrawal
        Arguments.of(
            List.of(
                new Transaction(ZonedDateTime.ofInstant(now.minus(2, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now.minus(1, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
                new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("deposit"), "withdrawal", "50")
            ),
            "deposit",
            now.minus(1, MINUTES)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsForMostRecentDeposits")
  public void testGetMostRecentDeposits(List<Transaction> transactions, String address, Instant result) {
    Assertions.assertThat(unit.getMostRecentDeposit(transactions, address)).isEqualTo(result);
  }

  @Test
  public void testHappyPath() {
    Instant now = Instant.now();

    MixerAddressTrackerEntry entry = new MixerAddressTrackerEntry("deposit", List.of(), now.minus(5, MINUTES));
    when(mixerAddressTracker.getAllAddressesToWatch()).thenReturn(List.of(entry));
    AddressInfoResponse addressInfoResponse = new AddressInfoResponse("25", List.of(
        new Transaction(ZonedDateTime.ofInstant(now.minus(5, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
        new Transaction(ZonedDateTime.ofInstant(now.minus(4, MINUTES), UTC), Optional.of("deposit"), "house", "50"),
        new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("other"), "deposit", "25")
    ));
    when(jobCoinClient.getAddressInfo(eq(entry.getDepositAddress())))
        .thenReturn(CompletableFuture.completedFuture(addressInfoResponse));
    when(jobCoinClient.sendTransaction(anyString(), anyString(), anyDouble())).thenReturn(CompletableFuture.completedFuture(true));

    unit.run();

    verify(jobCoinClient, times(1)).sendTransaction("deposit", "house", 25);
    verify(mixerAddressTracker, times(1)).setLatestTransaction("deposit", now);
  }

  @Test
  public void testDontProcessInProcessAccounts() throws InterruptedException {
    Instant now = Instant.now();

    MixerAddressTrackerEntry entry = new MixerAddressTrackerEntry("deposit", List.of(), now.minus(5, MINUTES));
    when(mixerAddressTracker.getAllAddressesToWatch()).thenReturn(List.of(entry));
    AddressInfoResponse firstResponse = new AddressInfoResponse("25", List.of(
        new Transaction(ZonedDateTime.ofInstant(now.minus(5, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
        new Transaction(ZonedDateTime.ofInstant(now.minus(4, MINUTES), UTC), Optional.of("deposit"), "house", "50"),
        new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("other"), "deposit", "25")
    ));
    AddressInfoResponse secondResponse = new AddressInfoResponse("35", List.of(
        new Transaction(ZonedDateTime.ofInstant(now.minus(5, MINUTES), UTC), Optional.of("other"), "deposit", "50"),
        new Transaction(ZonedDateTime.ofInstant(now.minus(4, MINUTES), UTC), Optional.of("deposit"), "house", "50"),
        new Transaction(ZonedDateTime.ofInstant(now, UTC), Optional.of("other"), "deposit", "25"),
        new Transaction(ZonedDateTime.ofInstant(now.plus(1, MINUTES), UTC), Optional.of("other"), "deposit", "10")
    ));
    when(jobCoinClient.getAddressInfo(eq(entry.getDepositAddress())))
        .thenReturn(CompletableFuture.completedFuture(firstResponse))
        .thenReturn(CompletableFuture.completedFuture(secondResponse));
    // Sending the first transaction takes 500ms, so when running the method twice in a row we should _only_ deposit 25
    when(jobCoinClient.sendTransaction(anyString(), anyString(), anyDouble())).thenReturn(CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return true;
    }));

    unit.run();
    unit.run();
    verify(jobCoinClient, times(2)).getAddressInfo("deposit");
    verify(jobCoinClient, times(1)).sendTransaction("deposit", "house", 25);
    verify(mixerAddressTracker, times(1)).setLatestTransaction("deposit", now);
  }

}
