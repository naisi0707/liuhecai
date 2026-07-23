package com.liuhecai.service.impl;

import com.liuhecai.mapper.AdminStatsMapper;
import com.liuhecai.service.AdminDashboardService;
import com.liuhecai.vo.AdminDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminStatsMapper adminStatsMapper;

    @Override
    public AdminDashboardVO getDashboard(int days) {
        int safeDays = days == 14 || days == 30 ? days : 7;
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDate sinceDate = today.minusDays(safeDays - 1L);
        LocalDateTime since = sinceDate.atStartOfDay();

        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setDays(safeDays);

        AdminDashboardVO.Kpis kpis = adminStatsMapper.selectKpis(todayStart);
        vo.setKpis(kpis != null ? kpis : new AdminDashboardVO.Kpis());

        vo.setTrends(buildTrends(safeDays, sinceDate, since));
        vo.setTopicStatus(toNameCounts(adminStatsMapper.countTopicsByStatus()));
        vo.setLotteryTopics(toNameCounts(adminStatsMapper.countTopicsByLottery()));
        vo.setAgents(nullToEmpty(adminStatsMapper.listAgents()));
        vo.setTenantRanks(nullToEmpty(adminStatsMapper.listTenantRanks()));
        vo.setRecentTenants(nullToEmpty(adminStatsMapper.listRecentTenants()));
        vo.setDraws(nullToEmpty(adminStatsMapper.listLatestDraws()));
        vo.setActivities(mergeActivities());

        return vo;
    }

    private AdminDashboardVO.Trends buildTrends(int days, LocalDate sinceDate, LocalDateTime since) {
        Map<LocalDate, Long> users = toDayMap(adminStatsMapper.countUsersByDay(since));
        Map<LocalDate, Long> orders = toDayMap(adminStatsMapper.countOrdersByDay(since));
        Map<LocalDate, Long> recharge = toDayMap(adminStatsMapper.sumRechargeByDay(since));
        Map<LocalDate, Long> tenants = toDayMap(adminStatsMapper.countTenantsByDay(since));

        AdminDashboardVO.Trends trends = new AdminDashboardVO.Trends();
        for (int i = 0; i < days; i++) {
            LocalDate d = sinceDate.plusDays(i);
            trends.getDates().add(d.toString());
            trends.getUsers().add(users.getOrDefault(d, 0L));
            trends.getOrders().add(orders.getOrDefault(d, 0L));
            trends.getRechargeAmount().add(recharge.getOrDefault(d, 0L));
            trends.getTenants().add(tenants.getOrDefault(d, 0L));
        }
        return trends;
    }

    private List<AdminDashboardVO.ActivityItem> mergeActivities() {
        List<AdminDashboardVO.ActivityItem> merged = new ArrayList<>();
        merged.addAll(nullToEmpty(adminStatsMapper.listCoinActivities()));
        merged.addAll(nullToEmpty(adminStatsMapper.listRechargeActivities()));
        merged.addAll(nullToEmpty(adminStatsMapper.listTopicActivities()));
        return merged.stream()
                .sorted(Comparator.comparing(
                        AdminDashboardVO.ActivityItem::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(30)
                .toList();
    }

    private List<AdminDashboardVO.NameCount> toNameCounts(List<Map<String, Object>> rows) {
        List<AdminDashboardVO.NameCount> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            AdminDashboardVO.NameCount item = new AdminDashboardVO.NameCount();
            item.setName(String.valueOf(row.get("name")));
            item.setCount(toLong(row.get("cnt")));
            list.add(item);
        }
        return list;
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

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
