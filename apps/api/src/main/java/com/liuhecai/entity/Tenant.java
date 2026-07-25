package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenants")
public class Tenant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 主代理 agent_accounts.id（库层 NOT NULL + FK） */
    private Long primaryAgentId;
    private String themeJson;
    private String kefuWechat;
    private String kefuQq;
    private String announcement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
