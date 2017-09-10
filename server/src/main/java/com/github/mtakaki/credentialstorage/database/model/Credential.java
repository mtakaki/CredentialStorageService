package com.github.mtakaki.credentialstorage.database.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.mtakaki.credentialstorage.database.model.view.AdminView;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an entry in the database, storing a credential pair (encrypted
 * using the symmetric key), the public asymmetric key, and the symmetric key
 * (encrypted using the public asymmetric key).
 *
 * @author mtakaki
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credential {
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'hh:mm:ss.SSS";

    @JsonView(AdminView.class)
    private String key;

    private String symmetricKey;

    // The length would never be longer than 876 because it's encrypted.
    @NotNull
    private String primary;

    // The length would never be longer than 876 because it's encrypted.
    private String secondary;

    @JsonView(AdminView.class)
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIMESTAMP_FORMAT)
    @JsonView(AdminView.class)
    private Date lastAccess;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIMESTAMP_FORMAT)
    @JsonView(AdminView.class)
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIMESTAMP_FORMAT)
    @JsonView(AdminView.class)
    private Date updatedAt;
}