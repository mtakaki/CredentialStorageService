package com.github.mtakaki.credentialstorage.client.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = { "symmetricKey" })
public class Credential {
    private String symmetricKey;

    @NotNull
    private String primary;

    private String secondary;
}