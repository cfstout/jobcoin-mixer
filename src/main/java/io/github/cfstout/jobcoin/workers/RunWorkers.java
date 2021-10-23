package io.github.cfstout.jobcoin.workers;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RunWorkers implements AutoCloseable{
  private final JobCoinDepositedWorker jobCoinDepositedWorker;
  private final MixerPayoutWorker mixerPayoutWorker;

  private static final ScheduledExecutorService runWorkers = Executors.newScheduledThreadPool(
      2,
      new ThreadFactoryBuilder()
          .setNameFormat("run-workers")
          .build()
  );

  @Inject
  public RunWorkers(JobCoinDepositedWorker jobCoinDepositedWorker,
                    MixerPayoutWorker mixerPayoutWorker) {
    this.jobCoinDepositedWorker = jobCoinDepositedWorker;
    this.mixerPayoutWorker = mixerPayoutWorker;
    init();
  }

  private void init() {
    runWorkers.scheduleAtFixedRate(jobCoinDepositedWorker, 0, 30, TimeUnit.SECONDS);
    runWorkers.scheduleAtFixedRate(mixerPayoutWorker, 40, 60, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    runWorkers.shutdown();
  }
}
