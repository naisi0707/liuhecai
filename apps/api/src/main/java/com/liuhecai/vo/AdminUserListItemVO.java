package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserListItemVO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String username;
    private Integer coinBalance;
    private Integer enabled;
    private LocalDateTime createdAt;
}
