package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DomainBindRequest {
    @NotBlank
    @Size(max = 128)
    private String host;

    /** 1 主域 0 普通，默认 0 */
    private Integer isPrimary;

    /** ENTRY 入口伪装 / FORUM 论坛，默认 FORUM */
    @Size(max = 16)
    private String role;
}
