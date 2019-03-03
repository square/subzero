package com.squareup.subzero.server.resources;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.subzero.proto.service.Service;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.google.common.io.BaseEncoding.base64;

/**
 * Decodes a CommandRequest or CommandResponse base64-encoded protobuf.
 *
 * We don't have a way to tell a request from a response apart. We could implement a heuristic (see
 * which fields are present and compare them with what we expect), but this could backfire if
 * someone is debugging broken/incomplete data.
 */
@Path("/pretty-print")
@Produces(MediaType.TEXT_HTML)
public class PrettyPrintResource {

  @Path("/request")
  @GET
  public String request(@QueryParam("raw") String raw) {
    try {
      byte[] rawBytes = base64().decode(raw);
      Service.CommandRequest command = Service.CommandRequest.parseFrom(rawBytes);
      String json = JsonFormat.printer().print(command);
      if (command.hasInitWallet()) {
        return "init wallet request: " + json;
      }
      if (command.hasFinalizeWallet()) {
        return "finalize wallet request: " + json;
      }
      if (command.hasSignTx()) {
        return "sign tx request: " + json;
      }
      return json;
    } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
      return e.getMessage();
    }
  }

  @Path("/response")
  @GET
  public String response(@QueryParam("raw") String raw) {
    try {
      byte[] rawBytes = base64().decode(raw);
      Service.CommandResponse command = Service.CommandResponse.parseFrom(rawBytes);
      String json = JsonFormat.printer().print(command);
      if (command.hasInitWallet()) {
        return "init wallet response: " + json;
      }
      if (command.hasFinalizeWallet()) {
        return "finalize wallet response: " + json;
      }
      if (command.hasSignTx()) {
        return "sign tx response: " + json;
      }
      return json;
    } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
      return e.getMessage();
    }
  }
}
