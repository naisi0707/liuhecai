package com.liuhecai.mapper;

import com.liuhecai.vo.AgentDashboardVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AgentOpsMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE tenant_id = #{tenantId}) AS userTotal,
              (SELECT COUNT(*) FROM users WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS userToday,
              (SELECT COUNT(*) FROM topics WHERE tenant_id = #{tenantId} AND status = 0) AS topicPending,
              (SELECT COUNT(*) FROM recharge_requests WHERE tenant_id = #{tenantId} AND status = 0) AS rechargePending,
              (SELECT COALESCE(SUM(amount), 0) FROM recharge_requests
                WHERE tenant_id = #{tenantId} AND status = 1 AND handled_at >= #{todayStart}) AS rechargeApprovedAmountToday,
              (SELECT COUNT(*) FROM topic_orders WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS orderCountToday,
              (SELECT COALESCE(SUM(price), 0) FROM topic_orders
                WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS orderAmountToday
            """)
    AgentDashboardVO.Kpis selectKpis(@Param("tenantId") Long tenantId,
                                       @Param("todayStart") LocalDateTime todayStart);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM users
            WHERE tenant_id = #{tenantId} AND created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countUsersByDay(@Param("tenantId") Long tenantId,
                                              @Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM topic_orders
            WHERE tenant_id = #{tenantId} AND created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countOrdersByDay(@Param("tenantId") Long tenantId,
                                              @Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(handled_at) AS statDay, COALESCE(SUM(amount), 0) AS cnt
            FROM recharge_requests
            WHERE tenant_id = #{tenantId} AND status = 1 AND handled_at >= #{since}
            GROUP BY DATE(handled_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> sumRechargeByDay(@Param("tenantId") Long tenantId,
                                               @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM coin_logs WHERE user_id = #{userId}")
    long countCoinLogs(@Param("userId") Long userId);

    @Select("""
            SELECT id, change_amount AS changeAmount, balance_after AS balanceAfter,
                   biz_type AS bizType, biz_id AS bizId, remark, created_at AS createdAt
            FROM coin_logs
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT #{offset}, #{limit}
            """)
    List<UserCoinLogVO> pageCoinLogs(@Param("userId") Long userId,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM topic_orders WHERE user_id = #{userId}")
    long countOrders(@Param("userId") Long userId);

    @Select("""
            SELECT o.id, o.topic_id AS topicId, tp.title AS topicTitle,
                   o.price, o.created_at AS createdAt
            FROM topic_orders o
            LEFT JOIN topics tp ON tp.id = o.topic_id
            WHERE o.user_id = #{userId}
            ORDER BY o.created_at DESC
            LIMIT #{offset}, #{limit}
            """)
    List<UserOrderVO> pageOrders(@Param("userId") Long userId,
                                 @Param("offset") long offset,
                                 @Param("limit") long limit);
}
