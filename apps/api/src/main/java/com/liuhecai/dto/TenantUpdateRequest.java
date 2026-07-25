package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantUpdateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 512)
    private String announcement;
}
