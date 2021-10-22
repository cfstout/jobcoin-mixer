package io.github.cfstout.jobcoin.controllers;

import java.util.List;

import com.google.inject.Inject;

import io.github.cfstout.jobcoin.clients.WalletSetupProvider;
import io.github.cfstout.jobcoin.db.MixerAddressTracker;

public class SetUpController {
  private final MixerAddressTracker mixerAddressTracker;
  private final WalletSetupProvider walletSetupProvider;

  @Inject
  public SetUpController(
      MixerAddressTracker mixerAddressTracker,
      WalletSetupProvider walletSetupProvider) {
    this.mixerAddressTracker = mixerAddressTracker;
    this.walletSetupProvider = walletSetupProvider;
  }

  public String setUpDepositAddress(List<String> returnAddresses) {
    String depositAddress = walletSetupProvider.selectNewWallet();
    mixerAddressTracker.initializeMixerMapping(depositAddress, returnAddresses);
    return depositAddress;
  }
}
