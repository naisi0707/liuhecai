package com.liuhecai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicStatusRequest {
    @NotNull
    @Min(0)
    @Max(3)
    private Integer status;
}
