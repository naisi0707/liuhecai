package com.liuhecai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentDashboardVO {
    private Integer days;
    private Kpis kpis = new Kpis();
    private Trends trends = new Trends();

    @Data
    public static class Kpis {
        private long userTotal;
        private long userToday;
        private long topicPending;
        private long rechargePending;
        private long rechargeApprovedAmountToday;
        private long orderCountToday;
        private long orderAmountToday;
    }

    @Data
    public static class Trends {
        private List<String> dates = new ArrayList<>();
        private List<Long> users = new ArrayList<>();
        private List<Long> orders = new ArrayList<>();
        private List<Long> rechargeAmount = new ArrayList<>();
    }
}
