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
}
