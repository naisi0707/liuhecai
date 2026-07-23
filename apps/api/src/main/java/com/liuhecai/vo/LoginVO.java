package com.liuhecai.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String realm;
    private Long userId;
    private String username;
    private Long tenantId;
}
