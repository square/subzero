package com.squareup.subzero.server.resources;

import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.subzero.proto.service.Common;
import com.squareup.subzero.shared.ColdWallet;
import com.squareup.subzero.shared.Constants;
import com.squareup.subzero.shared.SubzeroUtils;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.bitcoinj.core.NetworkParameters;

import static java.lang.String.format;

/**
 * Extracts xpubs from the finalize wallet responses and derives addresses.
 */
@Path("/compute")
@Produces(MediaType.APPLICATION_JSON)
public class ComputeResource {

  @Path("/xpubs")
  @GET
  public List<String> request(@QueryParam("finalizeResponses") List<String> finalizeResponses)
      throws InvalidProtocolBufferException {
    return SubzeroUtils.finalizeResponsesToAddresses(finalizeResponses);
  }

  @Produces(MediaType.TEXT_PLAIN)
  @Path("/address")
  @GET
  public String request(@QueryParam("finalizeResponses") List<String> finalizeResponses,
      @QueryParam("change") boolean change, @QueryParam("index") int index) throws InvalidProtocolBufferException {
    if (finalizeResponses.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException(format("Expecting %d finalizeResponses, got %d",
          Constants.MULTISIG_PARTICIPANTS, finalizeResponses.size()));
    }

    List<String> addresses = SubzeroUtils.finalizeResponsesToAddresses(finalizeResponses);

    NetworkParameters network = SubzeroUtils.inferNetworkParameters(addresses.get(0));

    ColdWallet coldWallet = new ColdWallet(network, 0, addresses, addresses.get(0));
    Common.Path p = Common.Path.newBuilder()
        .setIsChange(change)
        .setIndex(index)
        .build();
    return coldWallet.address(p).toString();
  }
}
