package com.squareup.subzero.actions;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.ncipher.nfast.NFException;
import com.squareup.subzero.InternalCommandConnector;
import com.squareup.subzero.ncipher.NCipher;
import com.squareup.subzero.SubzeroGui;
import com.squareup.subzero.wallet.WalletLoader;
import com.squareup.subzero.proto.service.Common.Destination;
import com.squareup.subzero.proto.service.Common.TxInput;
import com.squareup.subzero.proto.service.Common.TxOutput;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Internal.InternalCommandResponse;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import com.squareup.subzero.proto.wallet.WalletProto.Wallet;
import java.io.IOException;
import java.text.DecimalFormat;

import static java.lang.String.format;

public class SignTx {
  public static CommandResponse.SignTxResponse signTx(SubzeroGui subzero,
      InternalCommandConnector conn, CommandRequest request,
      InternalCommandRequest.Builder internalRequest) throws IOException, NFException {

    if (Strings.isNullOrEmpty(subzero.debug)) {
      // Compute amount being sent
      long amount = 0L;
      long fee = 0L;
      CommandRequest.SignTxRequest signTxRequest = request.getSignTx();
      for (TxInput input : signTxRequest.getInputsList()) {
        fee += input.getAmount();
      }
      for (TxOutput output : signTxRequest.getOutputsList()) {
        if (output.getDestination() == Destination.GATEWAY) {
          amount += output.getAmount();
        }
        fee -= output.getAmount();
      }

      // Tell user what is going on in interactive mode
      DecimalFormat formatter1 = new DecimalFormat("#,###");
      DecimalFormat formatter2 = new DecimalFormat("#,##0.00");

      StringBuilder s = new StringBuilder();
      s.append(format("SignTx. Wallet %s sending:\n\n", request.getWalletId()));
      s.append(format("%s Satoshi with fee %s Satoshi\n\n",
          formatter1.format(amount), formatter1.format(fee)));
      s.append(format("%f Bitcoin with fee %f Bitcoin\n\n",
          (double)amount / 100_000_000, (double)fee / 100_000_000));
      if (signTxRequest.getLocalRate() > 0) {
        s.append(format("%s %s with fee %s %s\n",
            formatter2.format((double) amount * signTxRequest.getLocalRate()),
            signTxRequest.getLocalCurrency(),
            formatter2.format((double) fee * signTxRequest.getLocalRate()),
            signTxRequest.getLocalCurrency()));
        s.append(format("(assuming 1 BTC = %s %s)",
            formatter2.format((double) 100_000_000 * signTxRequest.getLocalRate()),
            signTxRequest.getLocalCurrency()));
      }

      if(subzero.getScreens() != null && !subzero.getScreens().approveAction(s.toString())) {
        throw new RuntimeException("User did not approve signing operation");
      }
    }

    // Load wallet
    WalletLoader walletLoader = new WalletLoader();
    Wallet wallet;

    if (subzero.signtxTest) {
      // Load hardcode wallet
      wallet = walletLoader.loadTestWallet(subzero.nCipher);
    } else {
      // Load wallet file
      wallet = walletLoader.load(request.getWalletId());

      // Check that the wallet file has enc_pub_keys
      if (wallet.getEncryptedPubKeysCount() == 0) {
        throw new IllegalStateException("wallet not finalized.");
      }
    }
    // Build request
    InternalCommandRequest.SignTxRequest.Builder signTx =
        InternalCommandRequest.SignTxRequest.newBuilder();

    signTx.setEncryptedMasterSeed(wallet.getEncryptedMasterSeed());
    signTx.addAllEncryptedPubKeys(wallet.getEncryptedPubKeysList());

    signTx.addAllInputs(request.getSignTx().getInputsList());
    signTx.addAllOutputs(request.getSignTx().getOutputsList());
    signTx.setLockTime(request.getSignTx().getLockTime());

    NCipher nCipher = null;
    if (subzero.nCipher) {
      nCipher = new NCipher();
      nCipher.loadOcs(subzero.config.dataSignerKey, subzero.ocsPassword, subzero.getScreens());

      // TODO: the wallet contains a backup of the OCS files. We could drop them if they are
      // missing. It might make wallet recovery easier?

      nCipher.loadMasterSeedEncryptionKey(wallet.getMasterSeedEncryptionKeyId());
      byte[] ticket = nCipher.getMasterSeedEncryptionKeyTicket();
      internalRequest.setMasterSeedEncryptionKeyTicket(ByteString.copyFrom(ticket));

      nCipher.loadSoftcard(subzero.config.softcard, subzero.config.softcardPassword, subzero.config.pubKeyEncryptionKey);
      ticket = nCipher.getPubKeyEncryptionKeyTicket();
      internalRequest.setPubKeyEncryptionKeyTicket(ByteString.copyFrom(ticket));
    }

    // Send Request
    internalRequest.setSignTx(signTx);
    InternalCommandResponse.SignTxResponse iresp = conn.run(internalRequest.build()).getSignTx();

    if (subzero.nCipher) {
      nCipher.unloadOcs();
      if (Strings.isNullOrEmpty(subzero.debug)) {
        subzero.getScreens().removeOperatorCard("Please remove Operator Card and return it to the safe. Then hit <enter>.");
      }
    }

    // TODO: Log or something iresp.getTx() for debugging - where do we see this?

    return CommandResponse.SignTxResponse.newBuilder().addAllSignatures(iresp.getSignaturesList()).build();
  }
}
