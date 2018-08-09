package com.squareup.plutus.actions;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.ncipher.nfast.NFException;
import com.squareup.plutus.InternalCommandConnector;
import com.squareup.plutus.ncipher.NCipher;
import com.squareup.plutus.PlutusCli;
import com.squareup.plutus.wallet.WalletLoader;
import com.squareup.protos.plutus.service.Internal.InternalCommandRequest;
import com.squareup.protos.plutus.service.Internal.InternalCommandResponse;
import com.squareup.protos.plutus.service.Service.CommandRequest;
import com.squareup.protos.plutus.service.Service.CommandResponse;
import com.squareup.protos.plutus.wallet.WalletProto.Wallet;
import java.io.IOException;

import static java.lang.String.format;

public class FinalizeWallet {
  public static CommandResponse.FinalizeWalletResponse finalizeWallet(
      PlutusCli plutus,
      InternalCommandConnector conn,
      CommandRequest request,
      InternalCommandRequest.Builder internalRequest) throws IOException, NFException {

    if (Strings.isNullOrEmpty(plutus.debug)) {
      // Tell user what is going on in interactive mode
      String message = format("You are about to finalize wallet: %d", request.getWalletId());
      if(!plutus.getScreens().approveAction(message)) {
        throw new RuntimeException("User did not approve finalize wallet");
      }
    }

    // Load wallet file
    WalletLoader walletLoader = new WalletLoader();
    Wallet wallet = walletLoader.load(request.getWalletIdOrThrow());

    // Check that the wallet file does not have any enc_pub_keys
    if (wallet.getEncryptedPubKeysCount() > 0) {
      throw new IllegalStateException("wallet already finalized.");
    }

    // Build internal request
    InternalCommandRequest.FinalizeWalletRequest.Builder finalizeWallet =
        InternalCommandRequest.FinalizeWalletRequest.newBuilder();

    finalizeWallet.setEncryptedMasterSeed(wallet.getEncryptedMasterSeedOrThrow());
    finalizeWallet.addAllEncryptedPubKeys(request.getFinalizeWalletOrThrow().getEncryptedPubKeysList());

    NCipher nCipher = null;
    if (plutus.nCipher) {
      nCipher = new NCipher();
      nCipher.loadOcs(plutus.ocsPassword, plutus.getScreens());

      nCipher.loadMasterSeedEncryptionKey(wallet.getMasterSeedEncryptionKeyIdOrThrow());
      byte[] ticket = nCipher.getMasterSeedEncryptionKeyTicket();
      internalRequest.setMasterSeedEncryptionKeyTicket(ByteString.copyFrom(ticket));

      nCipher.loadSoftcard(plutus.config.softcard, plutus.config.getSoftcardPassword(), plutus.config.pubKeyEncryptionKey);
      ticket = nCipher.getPubKeyEncryptionKeyTicket();
      internalRequest.setPubKeyEncryptionKeyTicket(ByteString.copyFrom(ticket));
    }

    // Send Request
    internalRequest.setFinalizeWallet(finalizeWallet);
    InternalCommandResponse.FinalizeWalletResponse iresp =
        conn.run(internalRequest.build()).getFinalizeWalletOrThrow();

    if (plutus.nCipher) {
      nCipher.unloadOcs();
      if (Strings.isNullOrEmpty(plutus.debug)) {
        plutus.getScreens().removeOperatorCard("Please remove Operator Card and return it to the safe. Then hit <enter>.");
      }
    }

    // Save the encrypted pubkeys to the wallet
    Wallet newWallet = Wallet.newBuilder(wallet)
        .addAllEncryptedPubKeys(request.getFinalizeWalletOrThrow().getEncryptedPubKeysList())
        .build();
    walletLoader.save(request.getWalletIdOrThrow(), newWallet);

    // Build response
    return CommandResponse.FinalizeWalletResponse.newBuilder().setPubKey(iresp.getPubKey()).build();
  }
}
