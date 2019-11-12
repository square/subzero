package com.squareup.subzero.server;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ServerExceptionMapper implements ExceptionMapper<Throwable> {
  public Response toResponse(Throwable exception) {
    return Response.status(400)
        .entity(exception.getMessage())
        .type(MediaType.TEXT_PLAIN)
        .build();
  }
}
