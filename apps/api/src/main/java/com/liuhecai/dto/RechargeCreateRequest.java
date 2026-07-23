package com.liuhecai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RechargeCreateRequest {
    @NotNull
    @Min(1)
    @Max(1_000_000)
    private Integer amount;

    @Size(max = 64)
    private String payChannel;

    @Size(max = 256)
    private String remark;
}
