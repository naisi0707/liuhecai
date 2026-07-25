package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.OpAuditAction;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.util.CsvWriter;
import com.liuhecai.common.util.PageLimits;
import com.liuhecai.common.util.PasswordGenerator;
import com.liuhecai.dto.CoinAdjustRequest;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.entity.Tenant;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.AgentOpsMapper;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.mapper.UserMapper;
import com.liuhecai.service.AgentUserService;
import com.liuhecai.service.OpAuditService;
import com.liuhecai.service.TokenVersionService;
import com.liuhecai.service.UserCoinService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.AgentUserDetailVO;
import com.liuhecai.vo.AgentUserListItemVO;
import com.liuhecai.vo.PasswordResetVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentUserServiceImpl implements AgentUserService {

    private static final String TARGET_TYPE_USER = "USER";

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final AgentOpsMapper agentOpsMapper;
    private final UserCoinService userCoinService;
    private final PasswordEncoder passwordEncoder;
    private final TokenVersionService tokenVersionService;
    private final OpAuditService opAuditService;

    @Override
    public PageResult<AgentUserListItemVO> page(String username, Integer enabled, int page, int size) {
        requireAgent();
        int safePage = PageLimits.clampPage(page);
        int safeSize = PageLimits.clampSize(size);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username.trim());
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        long total = userMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + (safePage - 1) * safeSize + ", " + safeSize);
        List<AgentUserListItemVO> records = userMapper.selectList(wrapper).stream()
                .map(this::toListItem)
                .toList();

        PageResult<AgentUserListItemVO> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }

    @Override
    public AgentUserDetailVO getDetail(Long id) {
        requireAgent();
        User user = requireUserInTenant(id);
        return toDetail(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentUserDetailVO updateEnabled(Long id, EnabledRequest request) {
        requireAgent();
        validateEnabled(request.getEnabled());
        User user = requireUserInTenant(id);
        user.setEnabled(request.getEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(
                request.getEnabled() == 1 ? OpAuditAction.USER_ENABLE : OpAuditAction.USER_DISABLE,
                TARGET_TYPE_USER,
                String.valueOf(id),
                null);
        return toDetail(userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordResetVO resetPassword(Long id) {
        requireAgent();
        User user = requireUserInTenant(id);
        String raw = PasswordGenerator.randomPassword(10);
        user.setPasswordHash(passwordEncoder.encode(raw));
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(OpAuditAction.USER_RESET_PASSWORD, TARGET_TYPE_USER, String.valueOf(id), null);
        PasswordResetVO vo = new PasswordResetVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRawPassword(raw);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer adjustCoins(Long id, CoinAdjustRequest request) {
        AuthUser agent = requireAgent();
        requireUserInTenant(id);
        if (request.getAmount() == null || request.getAmount() == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调整金额不能为 0");
        }
        String remark = StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "代理调整金币";
        Integer balance = userCoinService.adjustCoins(id, request.getAmount(), null, remark, agent.getId());
        opAuditService.record(
                OpAuditAction.USER_COIN_ADJUST,
                TARGET_TYPE_USER,
                String.valueOf(id),
                "amount=" + request.getAmount());
        return balance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(Long id) {
        requireAgent();
        requireUserInTenant(id);
        tokenVersionService.bump(AuthRealm.USER, id);
        opAuditService.record(OpAuditAction.USER_FORCE_LOGOUT, TARGET_TYPE_USER, String.valueOf(id), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateEnabled(List<Long> ids, int enabled) {
        AuthUser agent = requireAgent();
        validateEnabled(enabled);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ids 不能为空");
        }
        List<User> found = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, ids)
                .eq(User::getTenantId, agent.getTenantId()));
        if (found.size() != ids.size()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .in(User::getId, ids)
                .eq(User::getTenantId, agent.getTenantId());
        User update = new User();
        update.setEnabled(enabled);
        update.setUpdatedAt(LocalDateTime.now());
        if (userMapper.update(update, wrapper) == 0) {
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
    public String exportUsersCsv(String username, Integer enabled) {
        AuthUser agent = requireAgent();
        Long tenantId = agent.getTenantId();
        Tenant tenant = tenantMapper.selectById(tenantId);
        String tenantName = tenant != null ? tenant.getName() : "";

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId);
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username.trim());
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled);
        }
        wrapper.orderByDesc(User::getCreatedAt).last("LIMIT 10000");
        List<User> users = userMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder();
        sb.append(CsvWriter.row("id", "tenantId", "tenantName", "username", "coinBalance", "enabled", "createdAt"));
        sb.append('\n');
        for (User user : users) {
            sb.append(CsvWriter.row(
                    user.getId(),
                    tenantId,
                    tenantName,
                    user.getUsername(),
                    user.getCoinBalance(),
                    user.getEnabled(),
                    user.getCreatedAt()));
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public PageResult<UserCoinLogVO> pageCoinLogs(Long userId, int page, int size) {
        requireAgent();
        requireUserInTenant(userId);
        return pageInternal(
                agentOpsMapper.countCoinLogs(userId),
                agentOpsMapper.pageCoinLogs(userId, offset(page, size), PageLimits.clampSize(size)),
                page, size);
    }

    @Override
    public PageResult<UserOrderVO> pageOrders(Long userId, int page, int size) {
        requireAgent();
        requireUserInTenant(userId);
        return pageInternal(
                agentOpsMapper.countOrders(userId),
                agentOpsMapper.pageOrders(userId, offset(page, size), PageLimits.clampSize(size)),
                page, size);
    }

    private User requireUserInTenant(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private AgentUserListItemVO toListItem(User user) {
        AgentUserListItemVO vo = new AgentUserListItemVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCoinBalance(user.getCoinBalance());
        vo.setEnabled(user.getEnabled());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private AgentUserDetailVO toDetail(User user) {
        AgentUserDetailVO vo = new AgentUserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCoinBalance(user.getCoinBalance());
        vo.setEnabled(user.getEnabled());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
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

    private AuthUser requireAgent() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        TenantContext.set(user.getTenantId());
        return user;
    }
}
