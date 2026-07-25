package com.liuhecai.service.impl;

import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.OpAuditAction;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.util.CsvWriter;
import com.liuhecai.common.util.PageLimits;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.mapper.AdminUserAgentMapper;
import com.liuhecai.service.AdminAgentService;
import com.liuhecai.service.OpAuditService;
import com.liuhecai.service.TenantAdminService;
import com.liuhecai.service.TokenVersionService;
import com.liuhecai.vo.AdminAgentDetailVO;
import com.liuhecai.vo.AdminAgentListItemVO;
import com.liuhecai.vo.AgentAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAgentServiceImpl implements AdminAgentService {

    private static final int DEFAULT_DAYS = 7;
    private static final String TARGET_TYPE_AGENT = "AGENT";

    private final AdminUserAgentMapper adminUserAgentMapper;
    private final TenantAdminService tenantAdminService;
    private final TokenVersionService tokenVersionService;
    private final OpAuditService opAuditService;

    @Override
    public PageResult<AdminAgentListItemVO> page(Long tenantId, String username, Integer enabled, int page, int size) {
        int safePage = PageLimits.clampPage(page);
        int safeSize = PageLimits.clampSize(size);
        String nameFilter = StringUtils.hasText(username) ? username.trim() : null;
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();

        long total = adminUserAgentMapper.countAgents(tenantId, nameFilter, enabled);
        List<AdminAgentListItemVO> records = adminUserAgentMapper.pageAgents(
                tenantId, nameFilter, enabled, sevenDaysAgo,
                (long) (safePage - 1) * safeSize, safeSize);

        PageResult<AdminAgentListItemVO> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }

    @Override
    public AdminAgentDetailVO getDetail(Long id) {
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        AdminAgentListItemVO row = adminUserAgentMapper.selectAgentRow(id, sevenDaysAgo);
        if (row == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDate sinceDate = today.minusDays(DEFAULT_DAYS - 1L);
        LocalDateTime since = sinceDate.atStartOfDay();

        AdminAgentDetailVO perf = adminUserAgentMapper.selectTenantPerf(row.getTenantId(), todayStart);
        AdminAgentDetailVO detail = new AdminAgentDetailVO();
        detail.setId(row.getId());
        detail.setTenantId(row.getTenantId());
        detail.setTenantName(row.getTenantName());
        detail.setUsername(row.getUsername());
        detail.setEnabled(row.getEnabled());
        detail.setIsPrimary(row.getIsPrimary());
        detail.setCreatedAt(row.getCreatedAt());
        detail.setUserCount(row.getUserCount());
        detail.setRechargeAmount7d(row.getRechargeAmount7d());

        if (perf != null) {
            detail.setUserTotal(perf.getUserTotal());
            detail.setUserToday(perf.getUserToday());
            detail.setTopicTotal(perf.getTopicTotal());
            detail.setTopicPending(perf.getTopicPending());
            detail.setRechargePending(perf.getRechargePending());
            detail.setRechargeApprovedAmountToday(perf.getRechargeApprovedAmountToday());
            detail.setOrderCountToday(perf.getOrderCountToday());
            detail.setOrderAmountToday(perf.getOrderAmountToday());
            detail.setOrderAmountTotal(perf.getOrderAmountTotal());
        }

        detail.setTrends(buildTrends(row.getTenantId(), DEFAULT_DAYS, sinceDate, since));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAgentListItemVO updateEnabled(Long id, EnabledRequest request) {
        validateEnabled(request.getEnabled());
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        AdminAgentListItemVO row = adminUserAgentMapper.selectAgentRow(id, sevenDaysAgo);
        if (row == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
        }
        if (adminUserAgentMapper.updateAgentEnabled(id, request.getEnabled()) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
        }
        tokenVersionService.bump(AuthRealm.AGENT, id);
        opAuditService.record(
                request.getEnabled() == 1 ? OpAuditAction.AGENT_ENABLE : OpAuditAction.AGENT_DISABLE,
                TARGET_TYPE_AGENT,
                String.valueOf(id),
                null);
        row.setEnabled(request.getEnabled());
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAdminVO resetPassword(Long id) {
        AgentAdminVO vo = tenantAdminService.resetAgentPassword(id);
        opAuditService.record(OpAuditAction.AGENT_RESET_PASSWORD, TARGET_TYPE_AGENT, String.valueOf(id), null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(Long id) {
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        if (adminUserAgentMapper.selectAgentRow(id, sevenDaysAgo) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
        }
        tokenVersionService.bump(AuthRealm.AGENT, id);
        opAuditService.record(OpAuditAction.AGENT_FORCE_LOGOUT, TARGET_TYPE_AGENT, String.valueOf(id), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long id) {
        tenantAdminService.softDeleteAgent(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateEnabled(List<Long> ids, int enabled) {
        validateEnabled(enabled);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ids 不能为空");
        }
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        for (Long id : ids) {
            if (adminUserAgentMapper.selectAgentRow(id, sevenDaysAgo) == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
            }
            if (adminUserAgentMapper.updateAgentEnabled(id, enabled) == 0) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
            }
            tokenVersionService.bump(AuthRealm.AGENT, id);
        }
        String detail = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        opAuditService.record(
                enabled == 1 ? OpAuditAction.AGENT_ENABLE : OpAuditAction.AGENT_DISABLE,
                TARGET_TYPE_AGENT,
                null,
                detail);
    }

    @Override
    public String exportAgentsCsv(Long tenantId, String username, Integer enabled) {
        String nameFilter = StringUtils.hasText(username) ? username.trim() : null;
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        List<AdminAgentListItemVO> rows = adminUserAgentMapper.listAgentsForExport(
                tenantId, nameFilter, enabled, sevenDaysAgo);
        StringBuilder sb = new StringBuilder();
        sb.append(CsvWriter.row("id", "tenantId", "tenantName", "username", "enabled", "userCount",
                "rechargeAmount7d", "createdAt"));
        sb.append('\n');
        for (AdminAgentListItemVO row : rows) {
            sb.append(CsvWriter.row(
                    row.getId(),
                    row.getTenantId(),
                    row.getTenantName(),
                    row.getUsername(),
                    row.getEnabled(),
                    row.getUserCount(),
                    row.getRechargeAmount7d(),
                    row.getCreatedAt()));
            sb.append('\n');
        }
        return sb.toString();
    }

    private AdminAgentDetailVO.Trends buildTrends(Long tenantId, int days, LocalDate sinceDate, LocalDateTime since) {
        Map<LocalDate, Long> users = toDayMap(
                adminUserAgentMapper.countUsersByDayForTenant(tenantId, since));
        Map<LocalDate, Long> orders = toDayMap(
                adminUserAgentMapper.countOrdersByDayForTenant(tenantId, since));
        Map<LocalDate, Long> recharge = toDayMap(
                adminUserAgentMapper.sumRechargeByDayForTenant(tenantId, since));

        AdminAgentDetailVO.Trends trends = new AdminAgentDetailVO.Trends();
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

    private static void validateEnabled(Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "enabled 只能为 0 或 1");
        }
    }
}
