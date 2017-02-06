package com.github.mtakaki.credentialstorage.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
@Table(name = "credential")
@Entity
@DynamicUpdate
@JsonNaming(PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy.class)
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    private int id;

    @Column(name = "`key`", unique = true, nullable = false, length = 736)
    @JsonIgnore
    private String key;

    @Column(name = "symmetric_key", unique = true, nullable = false, length = 736)
    private String symmetricKey;

    // The length would never be longer than 876 because it's encrypted.
    @Column(name = "`primary`", nullable = false, length = 876)
    @NotNull
    private String primary;

    // The length would never be longer than 876 because it's encrypted.
    @Column(name = "`secondary`", length = 876)
    private String secondary;

    @Column(name = "`description`")
    private String description;

    @Column(name = "last_access")
    private Date lastAccess;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}