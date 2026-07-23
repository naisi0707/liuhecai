package com.liuhecai.vo;

import lombok.Data;

@Data
public class TenantCreateResultVO {
    private TenantAdminVO tenant;
    private AgentAdminVO agent;
}
