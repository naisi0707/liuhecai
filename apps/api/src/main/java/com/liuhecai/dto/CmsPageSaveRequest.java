package com.liuhecai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CmsPageSaveRequest {
    @NotBlank
    @Size(max = 128)
    private String title;

    @NotNull
    private JsonNode content;
}
