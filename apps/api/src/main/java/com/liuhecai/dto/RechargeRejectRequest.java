package com.liuhecai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RechargeRejectRequest {
    @Size(max = 256)
    private String reason;
}
