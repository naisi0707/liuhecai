package com.liuhecai.service;

public interface UserCoinService {
    /**
     * 调整用户金币余额并写入流水。
     *
     * @return 变更后余额
     */
    Integer adjustCoins(Long userId, int amount, String bizType, String remark, Long operatorId);
}
