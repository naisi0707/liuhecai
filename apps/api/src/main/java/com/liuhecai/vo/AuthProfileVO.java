package com.liuhecai.vo;

import lombok.Data;

@Data
public class AuthProfileVO {
    private Long id;
    private String username;
    private String realm;
    private Long tenantId;
    private Integer coinBalance;
}
