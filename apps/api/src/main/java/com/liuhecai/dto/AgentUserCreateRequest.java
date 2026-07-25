package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentUserCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String username;
}
