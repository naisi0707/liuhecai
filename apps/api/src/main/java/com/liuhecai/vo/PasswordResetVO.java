package com.liuhecai.vo;

import lombok.Data;

@Data
public class PasswordResetVO {
    private Long id;
    private String username;
    private String rawPassword;
}
