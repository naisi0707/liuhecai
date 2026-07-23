package com.liuhecai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchEnabledRequest {
    @NotEmpty
    @Size(max = 200)
    private List<Long> ids;

    @NotNull
    private Integer enabled;
}
