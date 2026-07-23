package com.liuhecai.service.impl;

import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.entity.CoinLog;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.CoinLogMapper;
import com.liuhecai.mapper.UserMapper;
import com.liuhecai.service.UserCoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCoinServiceImpl implements UserCoinService {

    private final UserMapper userMapper;
    private final CoinLogMapper coinLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer adjustCoins(Long userId, int amount, String bizType, String remark, Long operatorId) {
        if (amount == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调整金额不能为 0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        int balance = user.getCoinBalance() == null ? 0 : user.getCoinBalance();
        final int after;
        try {
            after = Math.addExact(balance, amount);
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金币余额溢出");
        }
        if (after < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_COINS);
        }
        String type = StringUtils.hasText(bizType) ? bizType : (amount > 0 ? "GRANT" : "ADJUST");

        user.setCoinBalance(after);
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.COIN_CONFLICT);
        }

        CoinLog log = new CoinLog();
        log.setTenantId(user.getTenantId());
        log.setUserId(userId);
        log.setChangeAmount(amount);
        log.setBalanceAfter(after);
        log.setBizType(type);
        log.setBizId(operatorId != null ? String.valueOf(operatorId) : null);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        coinLogMapper.insert(log);
        return after;
    }
}
