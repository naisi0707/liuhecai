package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpAuditLogVO {
    private Long id;
    private String operatorRealm;
    private Long operatorId;
    private String operatorName;
    private Long tenantId;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private LocalDateTime createdAt;
}
