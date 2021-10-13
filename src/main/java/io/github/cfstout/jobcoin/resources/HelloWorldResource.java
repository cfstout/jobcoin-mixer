package io.github.cfstout.jobcoin.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cfstout.jobcoin.models.HelloWorldResponse;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
  private static final Logger LOG = LoggerFactory.getLogger(HelloWorldResource.class);

  @GET
  public Response helloWorld() {
    LOG.info("Saying hello");
    return Response.ok(new HelloWorldResponse("Hello World"))
        .build();
  }
}
