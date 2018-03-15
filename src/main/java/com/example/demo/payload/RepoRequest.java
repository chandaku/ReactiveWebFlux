package com.example.demo.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class RepoRequest {
    @NotBlank
    private String name;

    private String description;

    @JsonProperty("private")
    private Boolean isPrivate;
}
