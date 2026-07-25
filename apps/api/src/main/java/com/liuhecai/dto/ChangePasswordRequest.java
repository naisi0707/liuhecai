package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    @Size(max = 64)
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
}
