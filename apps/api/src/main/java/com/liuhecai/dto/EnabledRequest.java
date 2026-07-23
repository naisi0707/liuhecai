package com.liuhecai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnabledRequest {
    /** 1 启用 0 停用 */
    @NotNull
    private Integer enabled;
}
