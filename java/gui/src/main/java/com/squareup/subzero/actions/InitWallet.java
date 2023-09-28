package com.squareup.subzero.actions;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.squareup.subzero.InternalCommandConnector;
import com.squareup.subzero.ncipher.NCipher;
import com.squareup.subzero.SubzeroGui;
import com.squareup.subzero.wallet.WalletLoader;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Internal.InternalCommandResponse;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import com.squareup.subzero.proto.wallet.WalletProto.Wallet;
import java.security.SecureRandom;
import org.bouncycastle.util.encoders.Hex;

import static java.lang.String.format;

public class InitWallet {
  public static CommandResponse.InitWalletResponse initWallet(SubzeroGui subzero,
      InternalCommandConnector conn,
      CommandRequest request,
      InternalCommandRequest.Builder internalRequest) throws Exception {

    if (Strings.isNullOrEmpty(subzero.debug)) {
      // Tell user what is going on in interactive mode
      String message = format("You are about to initialize wallet: %d", request.getWalletId());
      if(!subzero.getScreens().approveAction(message)) {
        throw new RuntimeException("User did not approve init wallet");
      }
    }

    // Ensure we don't already have a wallet
    WalletLoader walletLoader = new WalletLoader(subzero.walletDirectory);
    walletLoader.ensureDoesNotExist(request.getWalletId());
    Wallet.Builder wallet = Wallet.newBuilder()
        .setCurrency(Wallet.Currency.TEST_NET);

    InternalCommandRequest.InitWalletRequest.Builder initWalletBuilder =
        InternalCommandRequest.InitWalletRequest.newBuilder();

    // Provide some random bytes to the nCipher
    byte[] randomBytes = new byte[64];
    new SecureRandom().nextBytes(randomBytes);
    initWalletBuilder.setRandomBytes(ByteString.copyFrom(randomBytes));

    NCipher nCipher = null;
    if (subzero.nCipher) {
      // Load security world, OCS and the master_seed encryption key.
      // Then get a ticket for the key, which we send to the CodeSafe module.
      // TODO(alok): this hangs if the nCipher is not properly configured. In dev, this happens
      // when I fail to open my ssh tunnels. Perhaps we need a loading screen here?
      nCipher = new NCipher();
      nCipher.loadOcs(subzero.config.dataSignerKey, subzero.ocsPassword, subzero.getScreens());

      wallet.setOcsId(nCipher.getOcsId());
      wallet.setOcsCardsFile(ByteString.copyFrom(nCipher.getOcsCardsFile()));
      wallet.setOcsCardOneFile(ByteString.copyFrom(nCipher.getOcsCardOneFile()));

      String key = nCipher.createMasterSeedEncryptionKey();
      wallet.setMasterSeedEncryptionKeyId(key);

      internalRequest.setMasterSeedEncryptionKeyTicket(ByteString.copyFrom(nCipher.getMasterSeedEncryptionKeyTicket()));

      nCipher.loadSoftcard(subzero.config.softcard, subzero.config.softcardPassword, subzero.config.pubKeyEncryptionKey);

      internalRequest.setPubKeyEncryptionKeyTicket(ByteString.copyFrom(nCipher.getPubKeyEncryptionKeyTicket()));
    }

    // Build and send internal request
    internalRequest.setInitWallet(initWalletBuilder);
    InternalCommandResponse response = conn.run(internalRequest.build());

    if (subzero.nCipher) {
      nCipher.unloadOcs();
      if (Strings.isNullOrEmpty(subzero.debug)) {
        subzero.getScreens().removeOperatorCard("Please remove Operator Card and return it to the safe. Then hit <enter>.");
      }
    }

    InternalCommandResponse.InitWalletResponse initWalletResponse = response.getInitWallet();

    // Save result
    System.out.println(Hex.toHexString(initWalletResponse.getEncryptedMasterSeed().getEncryptedMasterSeed().toByteArray()));
    wallet.setEncryptedMasterSeed(initWalletResponse.getEncryptedMasterSeed());

    System.out.println("encryptedPubKey: ");
    System.out.println(Hex.toHexString(initWalletResponse.getEncryptedPubKey().getEncryptedPubKey().toByteArray()));

    walletLoader.save(request.getWalletId(), wallet.build());

    CommandResponse.InitWalletResponse.Builder initResponse = CommandResponse.InitWalletResponse.newBuilder();
    initResponse.setEncryptedPubKey(initWalletResponse.getEncryptedPubKey());

    return initResponse.build();
  }
}
