package com.liuhecai.service;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.dto.CoinAdjustRequest;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.vo.AgentUserDetailVO;
import com.liuhecai.vo.AgentUserListItemVO;
import com.liuhecai.vo.PasswordResetVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;

import java.util.List;

public interface AgentUserService {
    PageResult<AgentUserListItemVO> page(String username, Integer enabled, int page, int size);

    AgentUserDetailVO getDetail(Long id);

    AgentUserDetailVO updateEnabled(Long id, EnabledRequest request);

    PasswordResetVO resetPassword(Long id);

    Integer adjustCoins(Long id, CoinAdjustRequest request);

    void forceLogout(Long id);

    void batchUpdateEnabled(List<Long> ids, int enabled);

    String exportUsersCsv(String username, Integer enabled);

    PageResult<UserCoinLogVO> pageCoinLogs(Long userId, int page, int size);

    PageResult<UserOrderVO> pageOrders(Long userId, int page, int size);
}
