package io.github.cfstout.jobcoin.config;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.github.cfstout.jobcoin.annotations.HouseAccount;
import io.github.cfstout.jobcoin.annotations.JobCoinUrl;
import io.github.cfstout.jobcoin.annotations.MixerPayoutIncrement;
import io.github.cfstout.jobcoin.annotations.TransactionFeePercent;
import io.github.cfstout.jobcoin.helpers.ObjectMapperProvider;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class JobCoinMixerModule extends DropwizardAwareModule<JobCoinMixerConfiguration> {
  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public ObjectMapper objectMapper() {
    return ObjectMapperProvider.getMapper();
  }

  @Provides
  @Singleton
  public AsyncHttpClient asyncHttpClient() {
    return new DefaultAsyncHttpClient();
  }

  @Provides
  @Singleton
  @JobCoinUrl
  public String jobCoinUrl() {
    return configuration().getJobCoinUrl();
  }

  @Provides
  @Singleton
  @HouseAccount
  public String houseAccount() {
    return configuration().getHouseAccount();
  }

  @Provides
  @Singleton
  @TransactionFeePercent
  public double transactionFeePercent() {
    return configuration().getMixerTransactionFeePercent();
  }

  @Provides
  @Singleton
  @MixerPayoutIncrement
  public int mixerPayoutIncrement() {
    return configuration().getMixerPayoutIncrement();
  }
}
