package com.liuhecai.vo;

import lombok.Data;

@Data
public class DomainAdminVO {
    private Long id;
    private String host;
    private Integer isPrimary;
    /** ENTRY / FORUM */
    private String role;
    private Integer status;
}
