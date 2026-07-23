package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCoinLogVO {
    private Long id;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String bizType;
    private String bizId;
    private String remark;
    private LocalDateTime createdAt;
}
