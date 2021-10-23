package io.github.cfstout.jobcoin;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.cfstout.jobcoin.config.JobCoinMixerConfiguration;
import io.github.cfstout.jobcoin.config.JobCoinMixerModule;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class JobCoinMixer extends Application<JobCoinMixerConfiguration> {

  public static void main(String[] args) throws Exception {
    new JobCoinMixer().run(args);
  }

  @Override
  public void initialize(Bootstrap<JobCoinMixerConfiguration> bootstrap) {
    GuiceBundle guiceBundle = GuiceBundle.builder()
        .modules(new JobCoinMixerModule())
        .enableAutoConfig("io.github.cfstout.jobcoin")
        .build();

    bootstrap.addBundle(guiceBundle);
  }

  @Override
  public void run(JobCoinMixerConfiguration configuration, Environment environment) throws Exception {
    // todo set workers to run
  }
}
