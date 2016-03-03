package com.github.mtakaki.credentialstorage.database.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "credential")
@Entity
@DynamicUpdate
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

    @Column(name = "`primary`", nullable = false, length = 876)
    @NotNull
    private String primary;

    @Column(name = "`secondary`", length = 876)
    private String secondary;
}