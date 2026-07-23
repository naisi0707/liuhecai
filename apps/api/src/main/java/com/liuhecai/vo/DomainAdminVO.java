package com.liuhecai.vo;

import lombok.Data;

@Data
public class DomainAdminVO {
    private Long id;
    private String host;
    private Integer isPrimary;
    private Integer status;
}
