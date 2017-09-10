package com.github.mtakaki.credentialstorage.database.model;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {
    private String key;
    private Map<String, String> headers;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Credential.TIMESTAMP_FORMAT)
    private Date timestamp;
}