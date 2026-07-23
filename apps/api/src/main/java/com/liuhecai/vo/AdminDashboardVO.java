package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminDashboardVO {
    private Integer days;
    private Kpis kpis = new Kpis();
    private Trends trends = new Trends();
    private List<NameCount> topicStatus = new ArrayList<>();
    private List<NameCount> lotteryTopics = new ArrayList<>();
    private List<AgentRow> agents = new ArrayList<>();
    private List<TenantRank> tenantRanks = new ArrayList<>();
    private List<ActivityItem> activities = new ArrayList<>();
    private List<DrawFreshness> draws = new ArrayList<>();
    private List<RecentTenant> recentTenants = new ArrayList<>();

    @Data
    public static class Kpis {
        private long tenantTotal;
        private long tenantEnabled;
        private long tenantDisabled;
        private long domainTotal;
        private long agentTotal;
        private long agentEnabled;
        private long userTotal;
        private long userToday;
        private long topicTotal;
        private long topicPending;
        private long topicPublished;
        private long rechargePending;
        private long rechargeApprovedAmountToday;
        private long orderCountToday;
        private long orderAmountToday;
        private long coinGrantToday;
        private long coinRechargeToday;
        private long coinPurchaseToday;
    }

    @Data
    public static class Trends {
        /** yyyy-MM-dd */
        private List<String> dates = new ArrayList<>();
        private List<Long> users = new ArrayList<>();
        private List<Long> orders = new ArrayList<>();
        private List<Long> rechargeAmount = new ArrayList<>();
        private List<Long> tenants = new ArrayList<>();
    }

    @Data
    public static class NameCount {
        /** raw key: topic status "0"/"1"... or lotteryType */
        private String name;
        private long count;
    }

    @Data
    public static class AgentRow {
        private Long id;
        private Long tenantId;
        private String tenantName;
        private String username;
        private Integer enabled;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TenantRank {
        private Long tenantId;
        private String tenantName;
        private Integer status;
        private long userCount;
        private long orderCount;
        private String primaryHost;
    }

    @Data
    public static class ActivityItem {
        /** COIN / RECHARGE / TOPIC */
        private String type;
        private String tenantName;
        private String bizType;
        private Integer status;
        private Integer amount;
        private Long userId;
        private String topicTitle;
        private LocalDateTime createdAt;
    }

    @Data
    public static class DrawFreshness {
        private String lotteryType;
        private String issueNo;
        private LocalDateTime drawTime;
        private LocalDateTime updatedAt;
        private String source;
    }

    @Data
    public static class RecentTenant {
        private Long id;
        private String name;
        private Integer status;
        private String primaryHost;
        private LocalDateTime createdAt;
    }
}
