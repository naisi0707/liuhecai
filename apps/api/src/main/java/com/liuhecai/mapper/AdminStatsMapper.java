package com.liuhecai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.liuhecai.vo.AdminDashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AdminStatsMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM tenants) AS tenantTotal,
              (SELECT COUNT(*) FROM tenants WHERE status = 1) AS tenantEnabled,
              (SELECT COUNT(*) FROM tenants WHERE status <> 1) AS tenantDisabled,
              (SELECT COUNT(*) FROM domains) AS domainTotal,
              (SELECT COUNT(*) FROM agent_accounts) AS agentTotal,
              (SELECT COUNT(*) FROM agent_accounts WHERE enabled = 1) AS agentEnabled,
              (SELECT COUNT(*) FROM users) AS userTotal,
              (SELECT COUNT(*) FROM users WHERE created_at >= #{todayStart}) AS userToday,
              (SELECT COUNT(*) FROM topics) AS topicTotal,
              (SELECT COUNT(*) FROM topics WHERE status = 0) AS topicPending,
              (SELECT COUNT(*) FROM topics WHERE status = 1) AS topicPublished,
              (SELECT COUNT(*) FROM recharge_requests WHERE status = 0) AS rechargePending,
              (SELECT COALESCE(SUM(amount), 0) FROM recharge_requests
                WHERE status = 1 AND handled_at >= #{todayStart}) AS rechargeApprovedAmountToday,
              (SELECT COUNT(*) FROM topic_orders WHERE created_at >= #{todayStart}) AS orderCountToday,
              (SELECT COALESCE(SUM(price), 0) FROM topic_orders
                WHERE created_at >= #{todayStart}) AS orderAmountToday,
              (SELECT COALESCE(SUM(change_amount), 0) FROM coin_logs
                WHERE biz_type = 'GRANT' AND created_at >= #{todayStart}) AS coinGrantToday,
              (SELECT COALESCE(SUM(change_amount), 0) FROM coin_logs
                WHERE biz_type = 'RECHARGE' AND created_at >= #{todayStart}) AS coinRechargeToday,
              (SELECT COALESCE(SUM(ABS(change_amount)), 0) FROM coin_logs
                WHERE biz_type = 'PURCHASE' AND created_at >= #{todayStart}) AS coinPurchaseToday
            """)
    AdminDashboardVO.Kpis selectKpis(@Param("todayStart") LocalDateTime todayStart);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM users
            WHERE created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countUsersByDay(@Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM topic_orders
            WHERE created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countOrdersByDay(@Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(handled_at) AS statDay, COALESCE(SUM(amount), 0) AS cnt
            FROM recharge_requests
            WHERE status = 1 AND handled_at >= #{since}
            GROUP BY DATE(handled_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> sumRechargeByDay(@Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM tenants
            WHERE created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countTenantsByDay(@Param("since") LocalDateTime since);

    @Select("""
            SELECT CAST(status AS CHAR) AS name, COUNT(*) AS cnt
            FROM topics
            GROUP BY status
            """)
    List<Map<String, Object>> countTopicsByStatus();

    @Select("""
            SELECT lottery_type AS name, COUNT(*) AS cnt
            FROM topics
            GROUP BY lottery_type
            ORDER BY cnt DESC
            """)
    List<Map<String, Object>> countTopicsByLottery();

    @Select("""
            SELECT a.id, a.tenant_id AS tenantId, t.name AS tenantName,
                   a.username, a.enabled, a.created_at AS createdAt
            FROM agent_accounts a
            LEFT JOIN tenants t ON t.id = a.tenant_id
            ORDER BY a.created_at DESC
            LIMIT 50
            """)
    List<AdminDashboardVO.AgentRow> listAgents();

    @Select("""
            SELECT t.id AS tenantId, t.name AS tenantName, t.status,
                   COALESCE(uc.cnt, 0) AS userCount,
                   COALESCE(oc.cnt, 0) AS orderCount,
                   pd.host AS primaryHost
            FROM tenants t
            LEFT JOIN (
              SELECT tenant_id, COUNT(*) AS cnt FROM users GROUP BY tenant_id
            ) uc ON uc.tenant_id = t.id
            LEFT JOIN (
              SELECT tenant_id, COUNT(*) AS cnt FROM topic_orders GROUP BY tenant_id
            ) oc ON oc.tenant_id = t.id
            LEFT JOIN (
              SELECT tenant_id, host FROM (
                SELECT tenant_id, host,
                       ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY is_primary DESC, id ASC) AS rn
                FROM domains
              ) ranked WHERE rn = 1
            ) pd ON pd.tenant_id = t.id
            ORDER BY orderCount DESC, userCount DESC
            LIMIT 5
            """)
    List<AdminDashboardVO.TenantRank> listTenantRanks();

    @Select("""
            SELECT t.id, t.name, t.status, t.created_at AS createdAt,
                   (SELECT d.host FROM domains d
                     WHERE d.tenant_id = t.id
                     ORDER BY d.is_primary DESC, d.id ASC LIMIT 1) AS primaryHost
            FROM tenants t
            ORDER BY t.created_at DESC
            LIMIT 5
            """)
    List<AdminDashboardVO.RecentTenant> listRecentTenants();

    @Select("""
            SELECT d.lottery_type AS lotteryType, d.issue_no AS issueNo,
                   d.draw_time AS drawTime, d.updated_at AS updatedAt, d.source
            FROM draw_results_global d
            INNER JOIN (
              SELECT lottery_type, MAX(id) AS max_id
              FROM draw_results_global
              GROUP BY lottery_type
            ) latest ON latest.max_id = d.id
            ORDER BY d.lottery_type
            """)
    List<AdminDashboardVO.DrawFreshness> listLatestDraws();

    @Select("""
            SELECT * FROM (
              SELECT 'COIN' AS type,
                     COALESCE(t.name, '-') AS tenantName,
                     c.biz_type AS bizType,
                     NULL AS status,
                     c.change_amount AS amount,
                     c.user_id AS userId,
                     NULL AS topicTitle,
                     c.created_at AS createdAt
              FROM coin_logs c
              LEFT JOIN tenants t ON t.id = c.tenant_id
              ORDER BY c.created_at DESC
              LIMIT 20
            ) coin
            UNION ALL
            SELECT * FROM (
              SELECT 'RECHARGE' AS type,
                     COALESCE(t.name, '-') AS tenantName,
                     NULL AS bizType,
                     r.status AS status,
                     r.amount AS amount,
                     r.user_id AS userId,
                     NULL AS topicTitle,
                     COALESCE(r.handled_at, r.created_at) AS createdAt
              FROM recharge_requests r
              LEFT JOIN tenants t ON t.id = r.tenant_id
              ORDER BY COALESCE(r.handled_at, r.created_at) DESC
              LIMIT 20
            ) recharge
            UNION ALL
            SELECT * FROM (
              SELECT 'TOPIC' AS type,
                     COALESCE(t.name, '-') AS tenantName,
                     NULL AS bizType,
                     tp.status AS status,
                     NULL AS amount,
                     NULL AS userId,
                     LEFT(tp.title, 80) AS topicTitle,
                     tp.updated_at AS createdAt
              FROM topics tp
              LEFT JOIN tenants t ON t.id = tp.tenant_id
              ORDER BY tp.updated_at DESC
              LIMIT 20
            ) topic
            ORDER BY createdAt DESC
            LIMIT 30
            """)
    List<AdminDashboardVO.ActivityItem> listRecentActivities();
}
