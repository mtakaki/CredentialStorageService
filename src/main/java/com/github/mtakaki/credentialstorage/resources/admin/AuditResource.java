package com.github.mtakaki.credentialstorage.resources.admin;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.github.mtakaki.credentialstorage.database.CredentialDAO;

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
    public List<String> listKeys() {
        return this.credentialDAO.getAllCredentialsKey();
    }

    @GET
    @Path("/last_accessed")
    public Set<String> getLastAccessedBy(@QueryParam("timestamp") final long unixTimestamp) {
        return this.credentialDAO.getCredentialKeysNotAccessedSince(new Date(unixTimestamp * 1000));
    }
}