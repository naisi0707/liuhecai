package com.liuhecai.service;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.vo.OpAuditLogVO;

public interface AdminAuditService {
    PageResult<OpAuditLogVO> page(String action, String operatorName, String targetType, int page, int size);
}
