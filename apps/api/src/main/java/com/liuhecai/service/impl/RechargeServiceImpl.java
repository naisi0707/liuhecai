package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.util.PageLimits;
import com.liuhecai.dto.RechargeCreateRequest;
import com.liuhecai.dto.RechargeRejectRequest;
import com.liuhecai.entity.CoinLog;
import com.liuhecai.entity.RechargeRequest;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.CoinLogMapper;
import com.liuhecai.mapper.RechargeRequestMapper;
import com.liuhecai.mapper.UserMapper;
import com.liuhecai.service.RechargeService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.RechargeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RechargeServiceImpl implements RechargeService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            STATUS_PENDING, "待确认",
            STATUS_APPROVED, "已通过",
            STATUS_REJECTED, "已拒绝"
    );

    private final RechargeRequestMapper rechargeRequestMapper;
    private final UserMapper userMapper;
    private final CoinLogMapper coinLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeVO create(RechargeCreateRequest request) {
        AuthUser authUser = requireUser();
        LocalDateTime now = LocalDateTime.now();
        RechargeRequest row = new RechargeRequest();
        row.setTenantId(authUser.getTenantId());
        row.setUserId(authUser.getId());
        row.setAmount(request.getAmount());
        row.setPayChannel(trimToNull(request.getPayChannel()));
        row.setRemark(trimToNull(request.getRemark()));
        row.setStatus(STATUS_PENDING);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        rechargeRequestMapper.insert(row);
        User user = userMapper.selectById(authUser.getId());
        return toVo(row, authUser.getUsername(), user == null ? null : user.getCoinBalance());
    }

    @Override
    public List<RechargeVO> listMine() {
        AuthUser authUser = requireUser();
        List<RechargeRequest> list = rechargeRequestMapper.selectList(new LambdaQueryWrapper<RechargeRequest>()
                .eq(RechargeRequest::getUserId, authUser.getId())
                .orderByDesc(RechargeRequest::getCreatedAt)
                .last("LIMIT " + PageLimits.clampSize(100)));
        User user = userMapper.selectById(authUser.getId());
        Integer balance = user == null ? null : user.getCoinBalance();
        return list.stream()
                .map(r -> toVo(r, authUser.getUsername(), balance))
                .collect(Collectors.toList());
    }

    @Override
    public List<RechargeVO> listForAgent(Integer status) {
        requireAgent();
        LambdaQueryWrapper<RechargeRequest> qw = new LambdaQueryWrapper<RechargeRequest>();
        if (status != null) {
            qw.eq(RechargeRequest::getStatus, status);
        }
        qw.orderByDesc(RechargeRequest::getCreatedAt)
                .last("LIMIT " + PageLimits.clampSize(100));
        List<RechargeRequest> list = rechargeRequestMapper.selectList(qw);
        Set<Long> userIds = list.stream().map(RechargeRequest::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        return list.stream().map(r -> {
            User u = users.get(r.getUserId());
            return toVo(r, u == null ? "-" : u.getUsername(), u == null ? null : u.getCoinBalance());
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeVO approve(Long id) {
        AuthUser agent = requireAgent();
        RechargeRequest row = rechargeRequestMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.RECHARGE_NOT_FOUND);
        }
        if (Objects.equals(row.getStatus(), STATUS_APPROVED)) {
            User user = userMapper.selectById(row.getUserId());
            return toVo(row, user == null ? "-" : user.getUsername(), user == null ? null : user.getCoinBalance());
        }
        if (!Objects.equals(row.getStatus(), STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.RECHARGE_ALREADY_HANDLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (rechargeRequestMapper.casUpdateStatus(id, agent.getTenantId(), STATUS_APPROVED, now, agent.getId(), null) == 0) {
            throw new BusinessException(ErrorCode.RECHARGE_ALREADY_HANDLED);
        }

        User user = userMapper.selectById(row.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        int balance = user.getCoinBalance() == null ? 0 : user.getCoinBalance();
        final int after;
        try {
            after = Math.addExact(balance, row.getAmount() == null ? 0 : row.getAmount());
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金币余额溢出，请联系管理员");
        }
        user.setCoinBalance(after);
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.COIN_CONFLICT);
        }

        row.setStatus(STATUS_APPROVED);
        row.setHandledBy(agent.getId());
        row.setHandledAt(now);
        row.setUpdatedAt(now);

        CoinLog log = new CoinLog();
        log.setTenantId(agent.getTenantId());
        log.setUserId(user.getId());
        log.setChangeAmount(row.getAmount());
        log.setBalanceAfter(after);
        log.setBizType("RECHARGE");
        log.setBizId(String.valueOf(row.getId()));
        log.setRemark("充值确认");
        log.setCreatedAt(now);
        coinLogMapper.insert(log);

        return toVo(row, user.getUsername(), after);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeVO reject(Long id, RechargeRejectRequest request) {
        AuthUser agent = requireAgent();
        RechargeRequest row = rechargeRequestMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.RECHARGE_NOT_FOUND);
        }
        if (Objects.equals(row.getStatus(), STATUS_REJECTED)) {
            User user = userMapper.selectById(row.getUserId());
            return toVo(row, user == null ? "-" : user.getUsername(), user == null ? null : user.getCoinBalance());
        }
        if (!Objects.equals(row.getStatus(), STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.RECHARGE_ALREADY_HANDLED);
        }
        LocalDateTime now = LocalDateTime.now();
        String reason = request == null ? null : trimToNull(request.getReason());
        if (rechargeRequestMapper.casUpdateStatus(id, agent.getTenantId(), STATUS_REJECTED, now, agent.getId(), reason) == 0) {
            throw new BusinessException(ErrorCode.RECHARGE_ALREADY_HANDLED);
        }
        row.setStatus(STATUS_REJECTED);
        row.setRejectReason(reason);
        row.setHandledBy(agent.getId());
        row.setHandledAt(now);
        row.setUpdatedAt(now);
        User user = userMapper.selectById(row.getUserId());
        return toVo(row, user == null ? "-" : user.getUsername(), user == null ? null : user.getCoinBalance());
    }

    private RechargeVO toVo(RechargeRequest row, String username, Integer coinBalance) {
        int status = row.getStatus() == null ? STATUS_PENDING : row.getStatus();
        return RechargeVO.builder()
                .id(String.valueOf(row.getId()))
                .username(username)
                .amount(row.getAmount())
                .payChannel(row.getPayChannel())
                .remark(row.getRemark())
                .status(status)
                .statusLabel(STATUS_LABEL.getOrDefault(status, String.valueOf(status)))
                .rejectReason(row.getRejectReason())
                .coinBalance(coinBalance)
                .createdAt(row.getCreatedAt())
                .handledAt(row.getHandledAt())
                .build();
    }

    private AuthUser requireUser() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private AuthUser requireAgent() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        TenantContext.set(user.getTenantId());
        return user;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
