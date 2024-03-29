package com.squareup.subzero.actions;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.ncipher.nfast.NFException;
import com.squareup.subzero.InternalCommandConnector;
import com.squareup.subzero.SubzeroGui;
import com.squareup.subzero.ncipher.NCipher;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Internal.InternalCommandResponse;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import com.squareup.subzero.proto.wallet.WalletProto.Wallet;
import com.squareup.subzero.wallet.WalletLoader;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.IOException;

import static java.lang.String.format;

public class FinalizeWallet {
  public static CommandResponse.FinalizeWalletResponse finalizeWallet(
      SubzeroGui subzero,
      InternalCommandConnector conn,
      CommandRequest request,
      InternalCommandRequest.Builder internalRequest) throws IOException, NFException {

    if (Strings.isNullOrEmpty(subzero.debug)) {
      // Tell user what is going on in interactive mode
      String message = format("You are about to finalize wallet: %d", request.getWalletId());
      if(!subzero.getScreens().approveAction(message)) {
        throw new RuntimeException("User did not approve finalize wallet");
      }
    }

    // Load wallet file
    WalletLoader walletLoader = new WalletLoader(subzero.walletDirectory);
    Wallet wallet = walletLoader.load(request.getWalletId());

    // Check that the wallet file does not have any enc_pub_keys
    if (wallet.getEncryptedPubKeysCount() > 0) {
      throw new IllegalStateException("wallet already finalized.");
    }

    // Build internal request
    InternalCommandRequest.FinalizeWalletRequest.Builder finalizeWallet =
        InternalCommandRequest.FinalizeWalletRequest.newBuilder();

    finalizeWallet.setEncryptedMasterSeed(wallet.getEncryptedMasterSeed());
    finalizeWallet.addAllEncryptedPubKeys(request.getFinalizeWallet().getEncryptedPubKeysList());

    NCipher nCipher = null;
    if (subzero.nCipher) {
      nCipher = new NCipher();
      nCipher.loadOcs(subzero.config.dataSignerKey, subzero.ocsPassword, subzero.getScreens());

      nCipher.loadMasterSeedEncryptionKey(wallet.getMasterSeedEncryptionKeyId());
      byte[] ticket = nCipher.getMasterSeedEncryptionKeyTicket();
      internalRequest.setMasterSeedEncryptionKeyTicket(ByteString.copyFrom(ticket));

      nCipher.loadSoftcard(subzero.config.softcard, subzero.config.softcardPassword, subzero.config.pubKeyEncryptionKey);
      ticket = nCipher.getPubKeyEncryptionKeyTicket();
      internalRequest.setPubKeyEncryptionKeyTicket(ByteString.copyFrom(ticket));
    }

    // Send Request
    internalRequest.setFinalizeWallet(finalizeWallet);
    InternalCommandResponse.FinalizeWalletResponse response =
        conn.run(internalRequest.build()).getFinalizeWallet();

    if (subzero.nCipher) {
      nCipher.unloadOcs();
      if (Strings.isNullOrEmpty(subzero.debug)) {
        subzero.getScreens().removeOperatorCard("Please remove Operator Card and return it to the safe. Then hit <enter>.");
      }
    }

    // Save the encrypted pubkeys to the wallet
    Wallet newWallet = Wallet.newBuilder(wallet)
        .addAllEncryptedPubKeys(request.getFinalizeWallet().getEncryptedPubKeysList())
        .setCurrency(getCurrencyFromFinalizeResponse(response))
        .build();
    walletLoader.save(request.getWalletId(), newWallet);

    // Build response
    return CommandResponse.FinalizeWalletResponse.newBuilder().setPubKey(response.getPubKey()).build();
  }

  private static Wallet.Currency getCurrencyFromFinalizeResponse(
      InternalCommandResponse.FinalizeWalletResponse response) {
    String base58PublicKey = response.getPubKey().toStringUtf8();
    try {
      DeterministicKey.deserializeB58(base58PublicKey, MainNetParams.get());
      return Wallet.Currency.MAIN_NET;
    } catch (Exception e) {
      // ignore the error, try again assuming test net
    }
    try {
      DeterministicKey.deserializeB58(base58PublicKey, TestNet3Params.get());
      return Wallet.Currency.TEST_NET;
    } catch (Exception e) {
      throw new RuntimeException("Public key " + base58PublicKey + " does not belong to either MAIN_NET or TEST_NET");
    }
  }
}
