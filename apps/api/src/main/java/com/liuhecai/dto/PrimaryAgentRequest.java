package com.liuhecai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrimaryAgentRequest {
    @NotNull
    private Long agentId;
}
