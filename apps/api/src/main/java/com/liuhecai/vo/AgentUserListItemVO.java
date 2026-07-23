package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentUserListItemVO {
    private Long id;
    private String username;
    private Integer coinBalance;
    private Integer enabled;
    private LocalDateTime createdAt;
}
