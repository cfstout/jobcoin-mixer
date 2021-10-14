package io.github.cfstout.jobcoin.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class JobCoinMixerConfiguration extends Configuration {
  private String jobCoinUrl;

  @JsonProperty
  public String getJobCoinUrl() {
    return jobCoinUrl;
  }

  @JsonProperty
  public void setJobCoinUrl(String jobCoinUrl) {
    this.jobCoinUrl = jobCoinUrl;
  }

}
