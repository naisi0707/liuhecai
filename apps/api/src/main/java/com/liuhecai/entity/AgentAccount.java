package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_accounts")
public class AgentAccount {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String username;
    private String passwordHash;
    private Integer enabled;
    /** 1=主代理 */
    private Integer isPrimary;
    /**
     * 主代理唯一槽：主代理为 1，非主为 null（配合 uk_agent_tenant_primary）。
     */
    private Integer primaryKey;
    private Integer tokenVersion;
    private LocalDateTime createdAt;
}
