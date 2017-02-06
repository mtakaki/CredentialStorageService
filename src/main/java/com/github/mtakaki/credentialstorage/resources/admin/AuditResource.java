package com.github.mtakaki.credentialstorage.resources.admin;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.github.mtakaki.credentialstorage.database.CredentialDAO;
import com.github.mtakaki.credentialstorage.database.model.Credential;

import io.dropwizard.hibernate.UnitOfWork;

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
    @UnitOfWork
    public List<Credential> list() {
        return this.credentialDAO.getAllCredentials();
    }
}