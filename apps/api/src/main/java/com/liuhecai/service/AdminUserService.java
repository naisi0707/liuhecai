package com.liuhecai.service;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.dto.BatchEnabledRequest;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.vo.AdminUserDetailVO;
import com.liuhecai.vo.AdminUserListItemVO;
import com.liuhecai.vo.PasswordResetVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;

import java.util.List;

public interface AdminUserService {
    PageResult<AdminUserListItemVO> page(Long tenantId, String username, Integer enabled, int page, int size);

    AdminUserDetailVO getDetail(Long id);

    AdminUserDetailVO updateEnabled(Long id, EnabledRequest request);

    PasswordResetVO resetPassword(Long id);

    void forceLogout(Long id);

    void batchUpdateEnabled(List<Long> ids, int enabled);

    String exportUsersCsv(Long tenantId, String username, Integer enabled);

    PageResult<UserCoinLogVO> pageCoinLogs(Long userId, int page, int size);

    PageResult<UserOrderVO> pageOrders(Long userId, int page, int size);
}
