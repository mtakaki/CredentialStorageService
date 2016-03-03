package com.github.mtakaki.credentialstorage;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class IntegrationTestUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    public static <T> T extractEntity(final Response response, final Class<T> clazz)
            throws IOException, JsonParseException, JsonMappingException {
        final InputStream entityInputStream = (InputStream) response.getEntity();
        final T entity = MAPPER.readValue(entityInputStream, clazz);
        return entity;
    }
}