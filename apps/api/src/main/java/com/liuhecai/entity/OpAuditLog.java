package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("op_audit_logs")
public class OpAuditLog {
    @TableId(type = IdType.ASSIGN_ID)
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
