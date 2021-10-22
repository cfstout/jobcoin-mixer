package io.github.cfstout.jobcoin.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import io.github.cfstout.jobcoin.controllers.SetUpController;
import io.github.cfstout.jobcoin.models.SetUpDepositRequest;
import io.github.cfstout.jobcoin.models.SetUpDepositResponse;

@Path("/mixer")
@Produces(MediaType.APPLICATION_JSON)
public class MixerAddressResource {
  private static final Logger LOG = LoggerFactory.getLogger(MixerAddressResource.class);

  private final SetUpController setUpController;

  @Inject
  public MixerAddressResource(SetUpController setUpController) {
    this.setUpController = setUpController;
  }

  @POST
  @Path("/set-up")
  public Response setUpDepositAddress(SetUpDepositRequest request) {
    LOG.info("Saying hello");
    if (request.getReturnAddresses().isEmpty()) {
      return Response
          .status(Status.BAD_REQUEST.getStatusCode(), "Must provide at least one return address")
          .build();
    }
    String depositAddress = setUpController.setUpDepositAddress(request.getReturnAddresses());
    return Response.ok(new SetUpDepositResponse(depositAddress))
        .build();
  }
}
