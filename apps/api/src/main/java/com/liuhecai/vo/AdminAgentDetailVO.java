package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminAgentDetailVO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String username;
    private Integer enabled;
    private Integer isPrimary;
    private LocalDateTime createdAt;
    private Long userCount;
    private Long rechargeAmount7d;

    private Long userTotal;
    private Long userToday;
    private Long topicTotal;
    private Long topicPending;
    private Long rechargePending;
    private Long rechargeApprovedAmountToday;
    private Long orderCountToday;
    private Long orderAmountToday;
    private Long orderAmountTotal;

    private Trends trends = new Trends();

    @Data
    public static class Trends {
        private List<String> dates = new ArrayList<>();
        private List<Long> users = new ArrayList<>();
        private List<Long> orders = new ArrayList<>();
        private List<Long> rechargeAmount = new ArrayList<>();
    }
}
