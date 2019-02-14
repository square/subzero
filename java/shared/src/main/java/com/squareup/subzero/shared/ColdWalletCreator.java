package com.squareup.subzero.shared;

import com.squareup.protos.subzero.service.Common.EncryptedPubKey;
import com.squareup.protos.subzero.service.Service.CommandRequest;
import com.squareup.protos.subzero.service.Service.CommandResponse;
import java.util.Map;
import java.util.Optional;

/**
 * ColdWalletCreator handles the creation of a new cold wallet.  There are two
 * offline steps to this process (InitWallet and FinalizeWallet), which are
 * handled by three static functions here.
 */
public class ColdWalletCreator {
  /**
   * Create a new wallet.  Call this multiple times, if you want to use multiple tokens.
   *
   * @param token A token for tracking this request.
   * @param walletId An arbitrary wallet ID number,  used to identify the wallet in all requests to
   * subzero.  A Plutus instance will reject this request if a wallet with that ID already exists.
   * @return The request to send to Plutus
   */
  public static CommandRequest init(String token, int walletId) {
    return CommandRequest.newBuilder()
        .setToken(token)
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
  }

  /**
   * Combine all of the responses from init.  The token from the response will be copied to the
   * returned CommandRequests so you can match them up.
   *
   * @param tokenToEncryptedPubKeyMap map of Persephone element tokens to those element's encrypted public key.
   * @param elementToken token of element who's request we want to construct.
   * @param walletId id of cold wallet in Plutus that will be finalized with this request
   * @return FinalizeWallet CommandRequests for Plutus to execute. Will return
   */
  public static Optional<CommandRequest> combine(
      Map<String, EncryptedPubKey> tokenToEncryptedPubKeyMap, String elementToken, int walletId) {
    if (!tokenToEncryptedPubKeyMap.containsKey(elementToken)
        || tokenToEncryptedPubKeyMap.containsValue(null)
        || tokenToEncryptedPubKeyMap.size() != Constants.ENCRYPTED_PUB_KEYS_MAX_COUNT) {
      return Optional.empty();
    }
    return Optional.of(CommandRequest.newBuilder()
        .setToken(elementToken)
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(tokenToEncryptedPubKeyMap.values()))
        .build());
  }

  /**
   * Get the "xpub..." public key, which you can pass to the ColdWallet constructor.
   *
   * @param finalizeWalletResponses The response from Plutus, completing wallet setup.
   * @return A public key string suitable for passing to DeterministicKey.deserializeB58
   */
  public static String finalize(CommandResponse finalizeWalletResponses) {
    // TODO: In the future, we should have a way of validating some kind of signature
    // so we know these genuinely came from our HSM-backed storage.

    return finalizeWalletResponses.getFinalizeWallet().getPubKeyOrThrow().toStringUtf8();
  }
}
