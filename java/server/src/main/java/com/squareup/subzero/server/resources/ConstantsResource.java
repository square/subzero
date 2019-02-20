package com.squareup.subzero.server.resources;

import com.squareup.subzero.shared.Constants;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Sends some constants back to the javascript.
 */
@Path("/constants")
@Produces(MediaType.APPLICATION_JSON)
public class ConstantsResource {

  class Constant {
    public int m;
    public int n;
    public String gateway;
  }

  @GET
  public Constant get() {
    Constant constant = new Constant();
    constant.m = Constants.MULTISIG_THRESHOLD;
    constant.n = Constants.MULTISIG_PARTICIPANTS;
    constant.gateway = "TODO";
    return constant;
  }
}
