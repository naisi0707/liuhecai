package com.liuhecai.vo;

import lombok.Data;

@Data
public class AgentAdminVO {
    private Long id;
    private String username;
    private Integer enabled;
    /** 1=主代理 */
    private Integer isPrimary;
    /** 仅创建/重置时返回明文密码 */
    private String rawPassword;
}
