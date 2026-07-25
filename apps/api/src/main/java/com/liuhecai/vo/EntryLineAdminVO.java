package com.liuhecai.vo;

import lombok.Data;

@Data
public class EntryLineAdminVO {
    private Long id;
    private Integer sortOrder;
    private String label;
    private String color;
    private Long targetTenantId;
    private String targetTenantName;
    private String targetForumHost;
    private Integer status;
}
