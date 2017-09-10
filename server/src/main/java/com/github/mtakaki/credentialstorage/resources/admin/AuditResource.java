package com.github.mtakaki.credentialstorage.resources.admin;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hibernate.validator.constraints.NotEmpty;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.mtakaki.credentialstorage.database.CredentialDAO;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.github.mtakaki.credentialstorage.database.model.view.AdminView;
import com.github.mtakaki.credentialstorage.resources.CredentialResource;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreaker;

import jodd.petite.meta.PetiteBean;
import lombok.AllArgsConstructor;

@Path("/audit")
@Consumes
@Produces(MediaType.APPLICATION_JSON)
@PetiteBean
@AllArgsConstructor
public class AuditResource {
    private final CredentialDAO credentialDAO;

    @GET
    @Path("/list")
    public List<String> listKeys() {
        return this.credentialDAO.getAllCredentialsKey();
    }

    @GET
    @Path("/last_accessed")
    public Set<String> getLastAccessedBy(@QueryParam("timestamp") final long unixTimestamp) {
        return this.credentialDAO.getCredentialKeysAccessedSince(unixTimestamp,
                System.currentTimeMillis() / 1000L);
    }

    @GET
    @Timed
    @CircuitBreaker
    @JsonView(AdminView.class)
    public Optional<Credential> getByKey(
            @HeaderParam(CredentialResource.PUBLIC_KEY_HEADER) @NotEmpty final String userPublicKey)
            throws IOException {
        return this.credentialDAO.getCredentialByKey(userPublicKey);
    }
}