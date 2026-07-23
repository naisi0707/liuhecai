package com.liuhecai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantStatusRequest {
    /** 1 启用 0 停用 */
    @NotNull
    private Integer status;
}
