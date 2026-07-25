package com.liuhecai.service.impl;

import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.OpAuditAction;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.util.CsvWriter;
import com.liuhecai.common.util.PageLimits;
import com.liuhecai.common.util.PasswordGenerator;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.mapper.AdminUserAgentMapper;
import com.liuhecai.service.AdminUserService;
import com.liuhecai.service.OpAuditService;
import com.liuhecai.service.TokenVersionService;
import com.liuhecai.vo.AdminUserDetailVO;
import com.liuhecai.vo.AdminUserListItemVO;
import com.liuhecai.vo.PasswordResetVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String TARGET_TYPE_USER = "USER";

    private final AdminUserAgentMapper adminUserAgentMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenVersionService tokenVersionService;
    private final OpAuditService opAuditService;

    @Override
    public PageResult<AdminUserListItemVO> page(Long tenantId, String username, Integer enabled, int page, int size) {
        int safePage = PageLimits.clampPage(page);
        int safeSize = PageLimits.clampSize(size);
        String nameFilter = StringUtils.hasText(username) ? username.trim() : null;

        long total = adminUserAgentMapper.countUsers(tenantId, nameFilter, enabled);
        List<AdminUserListItemVO> records = adminUserAgentMapper.pageUsers(
                tenantId, nameFilter, enabled, (long) (safePage - 1) * safeSize, safeSize);

        PageResult<AdminUserListItemVO> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }

    @Override
    public AdminUserDetailVO getDetail(Long id) {
        AdminUserDetailVO detail = adminUserAgentMapper.selectUserDetail(id);
        if (detail == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserDetailVO updateEnabled(Long id, EnabledRequest request) {
        validateEnabled(request.getEnabled());
        AdminUserDetailVO detail = adminUserAgentMapper.selectUserDetail(id);
        if (detail == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (adminUserAgentMapper.updateUserEnabled(id, request.getEnabled()) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(
                request.getEnabled() == 1 ? OpAuditAction.USER_ENABLE : OpAuditAction.USER_DISABLE,
                TARGET_TYPE_USER,
                String.valueOf(id),
                null);
        detail.setEnabled(request.getEnabled());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordResetVO resetPassword(Long id) {
        AdminUserDetailVO detail = adminUserAgentMapper.selectUserDetail(id);
        if (detail == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String raw = PasswordGenerator.randomPassword(10);
        if (adminUserAgentMapper.updateUserPassword(id, passwordEncoder.encode(raw)) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(OpAuditAction.USER_RESET_PASSWORD, TARGET_TYPE_USER, String.valueOf(id), null);
        PasswordResetVO vo = new PasswordResetVO();
        vo.setId(id);
        vo.setUsername(detail.getUsername());
        vo.setRawPassword(raw);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(Long id) {
        ensureUserExists(id);
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(OpAuditAction.USER_FORCE_LOGOUT, TARGET_TYPE_USER, String.valueOf(id), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateEnabled(List<Long> ids, int enabled) {
        validateEnabled(enabled);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ids 不能为空");
        }
        if (adminUserAgentMapper.countUsersByIds(ids) != ids.size()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (adminUserAgentMapper.batchUpdateUserEnabled(ids, enabled) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        for (Long id : ids) {
            tokenVersionService.bump(AuthRealm.USER, id);
        }
        String detail = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        opAuditService.record(
                enabled == 1 ? OpAuditAction.USER_ENABLE : OpAuditAction.USER_BATCH_DISABLE,
                TARGET_TYPE_USER,
                null,
                detail);
    }

    @Override
    public String exportUsersCsv(Long tenantId, String username, Integer enabled) {
        String nameFilter = StringUtils.hasText(username) ? username.trim() : null;
        List<AdminUserListItemVO> rows = adminUserAgentMapper.listUsersForExport(tenantId, nameFilter, enabled);
        StringBuilder sb = new StringBuilder();
        sb.append(CsvWriter.row("id", "tenantId", "tenantName", "username", "coinBalance", "enabled", "createdAt"));
        sb.append('\n');
        for (AdminUserListItemVO row : rows) {
            sb.append(CsvWriter.row(
                    row.getId(),
                    row.getTenantId(),
                    row.getTenantName(),
                    row.getUsername(),
                    row.getCoinBalance(),
                    row.getEnabled(),
                    row.getCreatedAt()));
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public PageResult<UserCoinLogVO> pageCoinLogs(Long userId, int page, int size) {
        ensureUserExists(userId);
        return pageInternal(
                adminUserAgentMapper.countCoinLogs(userId),
                adminUserAgentMapper.pageCoinLogs(userId, offset(page, size), PageLimits.clampSize(size)),
                page, size);
    }

    @Override
    public PageResult<UserOrderVO> pageOrders(Long userId, int page, int size) {
        ensureUserExists(userId);
        return pageInternal(
                adminUserAgentMapper.countOrders(userId),
                adminUserAgentMapper.pageOrders(userId, offset(page, size), PageLimits.clampSize(size)),
                page, size);
    }

    private void ensureUserExists(Long userId) {
        if (adminUserAgentMapper.selectUserDetail(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private static <T> PageResult<T> pageInternal(long total, List<T> records, int page, int size) {
        int safePage = PageLimits.clampPage(page);
        int safeSize = PageLimits.clampSize(size);
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }

    private static long offset(int page, int size) {
        return (long) (PageLimits.clampPage(page) - 1) * PageLimits.clampSize(size);
    }

    private static void validateEnabled(Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "enabled 只能为 0 或 1");
        }
    }
}
