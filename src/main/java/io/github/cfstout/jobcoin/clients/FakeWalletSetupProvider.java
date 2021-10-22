package io.github.cfstout.jobcoin.clients;

import java.util.UUID;

public class FakeWalletSetupProvider implements WalletSetupProvider {
  @Override
  public String selectNewWallet() {
    // For now we'll just generate a random new UUID and treat it as an address
    // todo probably want to select this from a list of wallets we actually own, or do wallet setup here
    return UUID.randomUUID().toString();
  }
}
