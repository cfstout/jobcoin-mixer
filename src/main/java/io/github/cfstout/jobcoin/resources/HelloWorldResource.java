package io.github.cfstout.jobcoin.resources;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import io.github.cfstout.jobcoin.clients.JobCoinClient;
import io.github.cfstout.jobcoin.models.HelloWorldResponse;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
  private static final Logger LOG = LoggerFactory.getLogger(HelloWorldResource.class);

  private final JobCoinClient jobCoinClient;

  @Inject
  public HelloWorldResource(JobCoinClient jobCoinClient) {
    this.jobCoinClient = jobCoinClient;
  }

  @GET
  public Response helloWorld() throws ExecutionException, InterruptedException, TimeoutException {
    LOG.info("Saying hello");
    String response = jobCoinClient.getAddressInfo("Alice").get(5, TimeUnit.SECONDS).toString();
    return Response.ok(new HelloWorldResponse(response))
        .build();
  }
}
