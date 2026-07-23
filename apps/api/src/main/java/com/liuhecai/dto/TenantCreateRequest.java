package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Size(max = 128)
    private String primaryHost;

    @Size(max = 64)
    private String agentUsername;

    @Size(max = 512)
    private String announcement;
}
