package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAgentListItemVO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String username;
    private Integer enabled;
    private LocalDateTime createdAt;
    private Long userCount;
    private Long rechargeAmount7d;
}
