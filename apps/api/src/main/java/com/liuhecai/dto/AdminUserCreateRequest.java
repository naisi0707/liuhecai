package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreateRequest {
    @NotNull
    private Long tenantId;

    @NotBlank
    @Size(max = 64)
    private String username;
}
