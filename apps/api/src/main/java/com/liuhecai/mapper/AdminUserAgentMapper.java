package com.liuhecai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.liuhecai.vo.AdminAgentDetailVO;
import com.liuhecai.vo.AdminAgentListItemVO;
import com.liuhecai.vo.AdminUserDetailVO;
import com.liuhecai.vo.AdminUserListItemVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AdminUserAgentMapper {

    @Select("""
            <script>
            SELECT COUNT(*) FROM agent_accounts a
            WHERE 1=1
            <if test="tenantId != null">AND a.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND a.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND a.enabled = #{enabled}</if>
            </script>
            """)
    long countAgents(@Param("tenantId") Long tenantId,
                     @Param("username") String username,
                     @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT a.id, a.tenant_id AS tenantId, t.name AS tenantName,
                   a.username, a.enabled, a.created_at AS createdAt,
                   (SELECT COUNT(*) FROM users u WHERE u.tenant_id = a.tenant_id) AS userCount,
                   (SELECT COALESCE(SUM(r.amount), 0) FROM recharge_requests r
                     WHERE r.tenant_id = a.tenant_id AND r.status = 1
                       AND r.handled_at &gt;= #{sevenDaysAgo}) AS rechargeAmount7d
            FROM agent_accounts a
            LEFT JOIN tenants t ON t.id = a.tenant_id
            WHERE 1=1
            <if test="tenantId != null">AND a.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND a.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND a.enabled = #{enabled}</if>
            ORDER BY a.created_at DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<AdminAgentListItemVO> pageAgents(@Param("tenantId") Long tenantId,
                                          @Param("username") String username,
                                          @Param("enabled") Integer enabled,
                                          @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo,
                                          @Param("offset") long offset,
                                          @Param("limit") long limit);

    @Select("""
            SELECT a.id, a.tenant_id AS tenantId, t.name AS tenantName,
                   a.username, a.enabled, a.created_at AS createdAt,
                   (SELECT COUNT(*) FROM users u WHERE u.tenant_id = a.tenant_id) AS userCount,
                   (SELECT COALESCE(SUM(r.amount), 0) FROM recharge_requests r
                     WHERE r.tenant_id = a.tenant_id AND r.status = 1
                       AND r.handled_at >= #{sevenDaysAgo}) AS rechargeAmount7d
            FROM agent_accounts a
            LEFT JOIN tenants t ON t.id = a.tenant_id
            WHERE a.id = #{id}
            """)
    AdminAgentListItemVO selectAgentRow(@Param("id") Long id,
                                        @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE tenant_id = #{tenantId}) AS userTotal,
              (SELECT COUNT(*) FROM users WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS userToday,
              (SELECT COUNT(*) FROM topics WHERE tenant_id = #{tenantId}) AS topicTotal,
              (SELECT COUNT(*) FROM topics WHERE tenant_id = #{tenantId} AND status = 0) AS topicPending,
              (SELECT COUNT(*) FROM recharge_requests WHERE tenant_id = #{tenantId} AND status = 0) AS rechargePending,
              (SELECT COALESCE(SUM(amount), 0) FROM recharge_requests
                WHERE tenant_id = #{tenantId} AND status = 1 AND handled_at >= #{todayStart}) AS rechargeApprovedAmountToday,
              (SELECT COUNT(*) FROM topic_orders WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS orderCountToday,
              (SELECT COALESCE(SUM(price), 0) FROM topic_orders
                WHERE tenant_id = #{tenantId} AND created_at >= #{todayStart}) AS orderAmountToday,
              (SELECT COALESCE(SUM(price), 0) FROM topic_orders WHERE tenant_id = #{tenantId}) AS orderAmountTotal
            """)
    AdminAgentDetailVO selectTenantPerf(@Param("tenantId") Long tenantId,
                                        @Param("todayStart") LocalDateTime todayStart);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM users
            WHERE tenant_id = #{tenantId} AND created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countUsersByDayForTenant(@Param("tenantId") Long tenantId,
                                                        @Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(created_at) AS statDay, COUNT(*) AS cnt
            FROM topic_orders
            WHERE tenant_id = #{tenantId} AND created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> countOrdersByDayForTenant(@Param("tenantId") Long tenantId,
                                                          @Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(handled_at) AS statDay, COALESCE(SUM(amount), 0) AS cnt
            FROM recharge_requests
            WHERE tenant_id = #{tenantId} AND status = 1 AND handled_at >= #{since}
            GROUP BY DATE(handled_at)
            ORDER BY statDay
            """)
    List<Map<String, Object>> sumRechargeByDayForTenant(@Param("tenantId") Long tenantId,
                                                          @Param("since") LocalDateTime since);

    @Update("UPDATE agent_accounts SET enabled = #{enabled} WHERE id = #{id}")
    int updateAgentEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT COUNT(*) FROM users u
            WHERE 1=1
            <if test="tenantId != null">AND u.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND u.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND u.enabled = #{enabled}</if>
            </script>
            """)
    long countUsers(@Param("tenantId") Long tenantId,
                    @Param("username") String username,
                    @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT u.id, u.tenant_id AS tenantId, t.name AS tenantName,
                   u.username, u.coin_balance AS coinBalance, u.enabled, u.created_at AS createdAt
            FROM users u
            LEFT JOIN tenants t ON t.id = u.tenant_id
            WHERE 1=1
            <if test="tenantId != null">AND u.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND u.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND u.enabled = #{enabled}</if>
            ORDER BY u.created_at DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<AdminUserListItemVO> pageUsers(@Param("tenantId") Long tenantId,
                                        @Param("username") String username,
                                        @Param("enabled") Integer enabled,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    @Select("""
            SELECT u.id, u.tenant_id AS tenantId, t.name AS tenantName,
                   u.username, u.coin_balance AS coinBalance, u.enabled,
                   u.created_at AS createdAt, u.updated_at AS updatedAt
            FROM users u
            LEFT JOIN tenants t ON t.id = u.tenant_id
            WHERE u.id = #{id}
            """)
    AdminUserDetailVO selectUserDetail(@Param("id") Long id);

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

    @Update("UPDATE users SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int updateUserEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Update("UPDATE users SET password_hash = #{passwordHash}, updated_at = NOW() WHERE id = #{id}")
    int updateUserPassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Select("SELECT username FROM users WHERE id = #{id}")
    String selectUserUsername(@Param("id") Long id);

    @Update("UPDATE users SET token_version = token_version + 1, updated_at = NOW() WHERE id = #{id}")
    int bumpUserTokenVersion(@Param("id") Long id);

    @Update("UPDATE agent_accounts SET token_version = token_version + 1 WHERE id = #{id}")
    int bumpAgentTokenVersion(@Param("id") Long id);

    @Select("SELECT token_version FROM users WHERE id = #{id}")
    Integer selectUserTokenVersion(@Param("id") Long id);

    @Select("SELECT token_version FROM agent_accounts WHERE id = #{id}")
    Integer selectAgentTokenVersion(@Param("id") Long id);

    @Update("""
            <script>
            UPDATE users SET enabled = #{enabled}, updated_at = NOW()
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int batchUpdateUserEnabled(@Param("ids") List<Long> ids, @Param("enabled") int enabled);

    @Select("""
            <script>
            SELECT u.id, u.tenant_id AS tenantId, t.name AS tenantName,
                   u.username, u.coin_balance AS coinBalance, u.enabled, u.created_at AS createdAt
            FROM users u
            LEFT JOIN tenants t ON t.id = u.tenant_id
            WHERE 1=1
            <if test="tenantId != null">AND u.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND u.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND u.enabled = #{enabled}</if>
            ORDER BY u.created_at DESC
            LIMIT 10000
            </script>
            """)
    List<AdminUserListItemVO> listUsersForExport(@Param("tenantId") Long tenantId,
                                                 @Param("username") String username,
                                                 @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT a.id, a.tenant_id AS tenantId, t.name AS tenantName,
                   a.username, a.enabled, a.created_at AS createdAt,
                   (SELECT COUNT(*) FROM users u WHERE u.tenant_id = a.tenant_id) AS userCount,
                   (SELECT COALESCE(SUM(r.amount), 0) FROM recharge_requests r
                     WHERE r.tenant_id = a.tenant_id AND r.status = 1
                       AND r.handled_at &gt;= #{sevenDaysAgo}) AS rechargeAmount7d
            FROM agent_accounts a
            LEFT JOIN tenants t ON t.id = a.tenant_id
            WHERE 1=1
            <if test="tenantId != null">AND a.tenant_id = #{tenantId}</if>
            <if test="username != null and username != ''">AND a.username LIKE CONCAT('%', #{username}, '%')</if>
            <if test="enabled != null">AND a.enabled = #{enabled}</if>
            ORDER BY a.created_at DESC
            LIMIT 10000
            </script>
            """)
    List<AdminAgentListItemVO> listAgentsForExport(@Param("tenantId") Long tenantId,
                                                   @Param("username") String username,
                                                   @Param("enabled") Integer enabled,
                                                   @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);
}
