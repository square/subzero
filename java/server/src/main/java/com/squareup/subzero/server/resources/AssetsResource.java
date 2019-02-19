package com.squareup.subzero.server.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Custom assets handling code. Supports hot-reloading for ease of development.
 */
@Path("/")
public class AssetsResource {
  private static final String defaultFile = "index.html";

  @GET
  public Response getDefaultAsset() throws IOException {
    return getAsset(defaultFile);
  }

  @GET
  @Path("{file}")
  public Response getAsset(@PathParam("file") String file) throws IOException {
    String root = "server/src/main/resources/assets/";
    java.nio.file.Path path = Paths.get(root, file);
    if (!Files.exists(path)) {
      return Response.seeOther(UriBuilder.fromUri("/assets/" + file).build()).build();
    }

    return Response
        .ok(Files.readAllBytes(path))
        .type(getContentType(path.toString()))
        .build();
  }

  public String getContentType(String filename) {
    if (filename.endsWith(".css")) {
      return "text/css";
    }
    if (filename.endsWith(".js")) {
      return "application/javascript";
    }
    return "text/html";
  }
}
