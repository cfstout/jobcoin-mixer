package io.github.cfstout.jobcoin.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobCoinMixerConfiguration extends Configuration {
  private final String jobCoinUrl;
  private final String houseAccount;

  @JsonCreator
  public JobCoinMixerConfiguration(@JsonProperty("jobCoinUrl") String jobCoinUrl,
                                   @JsonProperty("houseAccount") String houseAccount) {
    this.jobCoinUrl = jobCoinUrl;
    this.houseAccount = houseAccount;
  }
}
