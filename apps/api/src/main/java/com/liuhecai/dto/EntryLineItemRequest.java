package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EntryLineItemRequest {
    /** 可选；整表替换时忽略 */
    private Long id;

    @NotNull
    private Integer sortOrder;

    @NotBlank
    @Size(max = 64)
    private String label;

    @NotBlank
    @Size(max = 16)
    private String color;

    @NotNull
    private Long targetTenantId;

    @NotNull
    private Integer status;
}
