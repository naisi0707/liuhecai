package com.liuhecai.vo;

import lombok.Data;

import java.util.List;

@Data
public class TenantAdminVO {
    private Long id;
    private String name;
    private Integer status;
    private String announcement;
    private String kefuWechat;
    private List<DomainAdminVO> domains;
    private List<AgentAdminVO> agents;
}
