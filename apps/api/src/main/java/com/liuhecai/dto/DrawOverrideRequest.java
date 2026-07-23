package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DrawOverrideRequest {
    @NotBlank
    private String lotteryType;

    @NotBlank
    @Size(max = 32)
    private String issueNo;

    @NotNull
    private LocalDateTime drawTime;

    @NotNull
    @Size(min = 6, max = 6)
    private List<@Pattern(regexp = "\\d{1,2}") String> numbers;

    @NotBlank
    @Pattern(regexp = "\\d{1,2}")
    private String specialNumber;

    @Size(max = 256)
    private String note;
}
