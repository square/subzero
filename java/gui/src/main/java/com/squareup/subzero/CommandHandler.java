package com.squareup.subzero;

import com.squareup.subzero.actions.FinalizeWallet;
import com.squareup.subzero.actions.InitWallet;
import com.squareup.subzero.actions.SignTx;
import com.squareup.subzero.shared.Constants;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;

/**
 * CommandHandler dispatches CommandRequests to an appropriate handler, which will make an
 * InternalCommandRequest.
 *
 * This is the shared entry point for the Cli and integration
 */

public class CommandHandler {
  public static CommandResponse dispatch(SubzeroGui subzero, InternalCommandConnector conn,
      CommandRequest serviceRequest) throws Exception {

    InternalCommandRequest.Builder internalRequest = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(serviceRequest.getWalletId());

    CommandResponse.Builder builder = CommandResponse.newBuilder();
    switch (serviceRequest.getCommandCase()) {
      case INITWALLET:
        builder.setInitWallet(
            InitWallet.initWallet(subzero, conn, serviceRequest, internalRequest));
        break;
      case FINALIZEWALLET:
        builder.setFinalizeWallet(
            FinalizeWallet.finalizeWallet(subzero, conn, serviceRequest, internalRequest));
        break;
      case SIGNTX:
        builder.setSignTx(SignTx.signTx(subzero, conn, serviceRequest, internalRequest));
        break;
      default:
        throw new RuntimeException("Invalid request");
    }
    if (serviceRequest.hasToken()) {
      builder.setToken(serviceRequest.getToken());
    }
    return builder.build();
  }
}
