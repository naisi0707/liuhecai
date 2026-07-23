package com.liuhecai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CoinAdjustRequest {
    /** 正数加币、负数扣币，不可为 0；绝对值上限 100 万 */
    @NotNull
    @Min(-1_000_000)
    @Max(1_000_000)
    private Integer amount;

    @Size(max = 256)
    private String remark;
}
