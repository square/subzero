package com.squareup.plutus;

import com.squareup.plutus.actions.FinalizeWallet;
import com.squareup.plutus.actions.InitWallet;
import com.squareup.plutus.actions.SignTx;
import com.squareup.plutus.shared.Constants;
import com.squareup.protos.plutus.service.Internal.InternalCommandRequest;
import com.squareup.protos.plutus.service.Service.CommandRequest;
import com.squareup.protos.plutus.service.Service.CommandResponse;

/**
 * CommandHandler dispatches CommandRequests to an appropriate handler, which will make an
 * InternalCommandRequest.
 *
 * This is the shared entry point for the Cli and integration
 */

public class CommandHandler {
  public static CommandResponse dispatch(PlutusCli plutus, InternalCommandConnector conn,
      CommandRequest serviceRequest) throws Exception {

    InternalCommandRequest.Builder internalRequest = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(serviceRequest.getWalletId());

    CommandResponse.Builder builder = CommandResponse.newBuilder();
    switch (serviceRequest.getCommandCase()) {
      case INITWALLET:
        builder.setInitWallet(
            InitWallet.initWallet(plutus, conn, serviceRequest, internalRequest));
        break;
      case FINALIZEWALLET:
        builder.setFinalizeWallet(
            FinalizeWallet.finalizeWallet(plutus, conn, serviceRequest, internalRequest));
        break;
      case SIGNTX:
        builder.setSignTx(SignTx.signTx(plutus, conn, serviceRequest, internalRequest));
        break;
      default:
        throw new RuntimeException("Invalid request");
    }
    builder.setOrClearToken(serviceRequest.getToken());
    return builder.build();
  }
}
