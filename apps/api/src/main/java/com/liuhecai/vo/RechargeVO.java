package com.liuhecai.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RechargeVO {
    private String id;
    private String username;
    private Integer amount;
    private String payChannel;
    private String remark;
    private Integer status;
    private String statusLabel;
    private String rejectReason;
    private Integer coinBalance;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
