package com.liuhecai.service;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.vo.AdminAgentDetailVO;
import com.liuhecai.vo.AdminAgentListItemVO;
import com.liuhecai.vo.AgentAdminVO;

public interface AdminAgentService {
    PageResult<AdminAgentListItemVO> page(Long tenantId, String username, Integer enabled, int page, int size);

    AdminAgentDetailVO getDetail(Long id);

    AdminAgentListItemVO updateEnabled(Long id, EnabledRequest request);

    AgentAdminVO resetPassword(Long id);

    void forceLogout(Long id);

    String exportAgentsCsv(Long tenantId, String username, Integer enabled);
}
