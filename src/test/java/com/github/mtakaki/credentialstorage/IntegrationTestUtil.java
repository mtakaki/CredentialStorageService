package com.github.mtakaki.credentialstorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class IntegrationTestUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.SNAKE_CASE);

    public static <T> T extractEntity(final Response response, final Class<T> clazz)
            throws IOException, JsonParseException, JsonMappingException {
        final InputStream entityInputStream = (InputStream) response.getEntity();
        return MAPPER.readValue(entityInputStream, clazz);
    }

    public static <T> List<T> extractEntityList(final Response response, final Class<T> clazz)
            throws IOException, JsonParseException, JsonMappingException {
        final InputStream entityInputStream = (InputStream) response.getEntity();
        return MAPPER.readValue(entityInputStream,
                MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}