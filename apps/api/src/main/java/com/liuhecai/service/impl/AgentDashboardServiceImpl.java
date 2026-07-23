package com.liuhecai.service.impl;

import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.mapper.AgentOpsMapper;
import com.liuhecai.service.AgentDashboardService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.AgentDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentDashboardServiceImpl implements AgentDashboardService {

    private final AgentOpsMapper agentOpsMapper;

    @Override
    public AgentDashboardVO getDashboard(int days) {
        AuthUser agent = requireAgent();
        int safeDays = days == 14 || days == 30 ? days : 7;
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDate sinceDate = today.minusDays(safeDays - 1L);
        LocalDateTime since = sinceDate.atStartOfDay();

        AgentDashboardVO vo = new AgentDashboardVO();
        vo.setDays(safeDays);

        AgentDashboardVO.Kpis kpis = agentOpsMapper.selectKpis(agent.getTenantId(), todayStart);
        vo.setKpis(kpis != null ? kpis : new AgentDashboardVO.Kpis());
        vo.setTrends(buildTrends(agent.getTenantId(), safeDays, sinceDate, since));
        return vo;
    }

    private AgentDashboardVO.Trends buildTrends(Long tenantId, int days, LocalDate sinceDate, LocalDateTime since) {
        Map<LocalDate, Long> users = toDayMap(agentOpsMapper.countUsersByDay(tenantId, since));
        Map<LocalDate, Long> orders = toDayMap(agentOpsMapper.countOrdersByDay(tenantId, since));
        Map<LocalDate, Long> recharge = toDayMap(agentOpsMapper.sumRechargeByDay(tenantId, since));

        AgentDashboardVO.Trends trends = new AgentDashboardVO.Trends();
        for (int i = 0; i < days; i++) {
            LocalDate d = sinceDate.plusDays(i);
            trends.getDates().add(d.toString());
            trends.getUsers().add(users.getOrDefault(d, 0L));
            trends.getOrders().add(orders.getOrDefault(d, 0L));
            trends.getRechargeAmount().add(recharge.getOrDefault(d, 0L));
        }
        return trends;
    }

    private Map<LocalDate, Long> toDayMap(List<Map<String, Object>> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            LocalDate day = toLocalDate(row.get("statDay"));
            if (day != null) {
                map.put(day, toLong(row.get("cnt")));
            }
        }
        return map;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof BigDecimal bd) {
            return bd.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private AuthUser requireAgent() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        TenantContext.set(user.getTenantId());
        return user;
    }
}
