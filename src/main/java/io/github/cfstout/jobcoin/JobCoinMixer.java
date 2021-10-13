package io.github.cfstout.jobcoin;

import io.github.cfstout.jobcoin.config.JobCoinMixerConfiguration;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import io.github.cfstout.jobcoin.resources.HelloWorldResource;

public class JobCoinMixer extends Application<JobCoinMixerConfiguration> {

  public static void main(String[] args) throws Exception {
    new JobCoinMixer().run(args);
  }

  @Override
  public void run(JobCoinMixerConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(new HelloWorldResource());
  }
}
