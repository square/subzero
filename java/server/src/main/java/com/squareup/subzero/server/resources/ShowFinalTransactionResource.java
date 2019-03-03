package com.squareup.subzero.server.resources;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.subzero.proto.service.Common;
import com.squareup.subzero.proto.service.Service;
import com.squareup.subzero.shared.ColdWallet;
import com.squareup.subzero.shared.ColdWalletCreator;
import com.squareup.subzero.shared.Constants;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.Networks;

import static java.lang.String.format;
import static org.bitcoinj.crypto.DeterministicKey.deserializeB58;

/**
 * Verifies and merges signatures. Returns final transaction.
 */
@Path("/show-final-transaction")
@Produces(MediaType.TEXT_PLAIN)
public class ShowFinalTransactionResource {

  @GET
  public String request(@QueryParam("signTxRequest") String signTxRequest,
      @QueryParam("finalizeResponses") List<String> finalizeResponses,
      @QueryParam("gateway") String gateway,
      @QueryParam("signTxResponses") List<String> signTxResponses)
      throws InvalidProtocolBufferException {
    if (finalizeResponses.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException(format("Expecting %d finalizeResponses, got %d",
          Constants.MULTISIG_PARTICIPANTS, finalizeResponses.size()));
    }
    if (signTxResponses.size() != Constants.MULTISIG_THRESHOLD) {
      throw new IllegalArgumentException(format("Expecting %d signTxResponses, got %d",
          Constants.MULTISIG_THRESHOLD, signTxResponses.size()));
    }

    // TODO: network detection should probably live somewhere else.
    NetworkParameters params = null;
    for (NetworkParameters network : Networks.get()) {
      try {
        deserializeB58(gateway, network);
        params = network;
        break;
      } catch (IllegalArgumentException e) {
        continue;
      }
    }

    Service.CommandRequest initialSignTxRequest = Service.CommandRequest.parseFrom(
        BaseEncoding.base64().decode(signTxRequest));

    List<String> addresses = Lists.newArrayList();
    for (String finalizeResponse : finalizeResponses) {
      byte[] rawFinalizeResponse = BaseEncoding.base64().decode(finalizeResponse);
      Service.CommandResponse commandResponse = Service.CommandResponse.parseFrom(rawFinalizeResponse);
      addresses.add(ColdWalletCreator.finalize(commandResponse));
    }

    ColdWallet coldWallet = new ColdWallet(params, initialSignTxRequest.getWalletId(), addresses, gateway);

    List<List<Common.Signature>> signatures = Lists.newArrayList();
    for (String signTxResponse : signTxResponses) {
      Service.CommandResponse sig = Service.CommandResponse.parseFrom(BaseEncoding.base64().decode(signTxResponse));
      signatures.add(sig.getSignTx().getSignaturesList());
    }

    String transaction = BaseEncoding.base16().encode(
        coldWallet.createTransaction(initialSignTxRequest.getSignTx().getInputsList(),
            initialSignTxRequest.getSignTx().getOutputsList(), signatures)).toLowerCase();
    return transaction;
  }
}
