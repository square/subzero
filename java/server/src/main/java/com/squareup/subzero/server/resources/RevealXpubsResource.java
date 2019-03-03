package com.squareup.subzero.server.resources;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.subzero.proto.service.Service;
import com.squareup.subzero.shared.ColdWalletCreator;
import com.squareup.subzero.shared.Constants;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static java.lang.String.format;

/**
 * Reveals xpubs from the finalize wallet responses.
 */
@Path("/reveal-xpubs")
@Produces(MediaType.APPLICATION_JSON)
public class RevealXpubsResource {

  @GET
  public List<String> request(@QueryParam("finalizeResponses") List<String> finalizeResponses)
      throws InvalidProtocolBufferException {
    if (finalizeResponses.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException(format("Expecting %d finalizeResponses, got %d",
          Constants.MULTISIG_PARTICIPANTS, finalizeResponses.size()));
    }

    List<String> r = Lists.newArrayList();
    for (String finalizeResponse : finalizeResponses) {
      byte[] rawFinalizeResponse = BaseEncoding.base64().decode(finalizeResponse);
      Service.CommandResponse commandResponse = Service.CommandResponse.parseFrom(rawFinalizeResponse);
      r.add(ColdWalletCreator.finalize(commandResponse));
    }

    return r;
  }
}
