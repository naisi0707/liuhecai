package com.liuhecai.service.impl;

import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.entity.OpAuditLog;
import com.liuhecai.mapper.OpAuditLogMapper;
import com.liuhecai.service.OpAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OpAuditServiceImpl implements OpAuditService {

    private final OpAuditLogMapper opAuditLogMapper;

    @Override
    public void record(String action, String targetType, String targetId, String detail) {
        AuthUser operator = AuthContext.get();
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        OpAuditLog log = new OpAuditLog();
        log.setOperatorRealm(operator.getRealm().name());
        log.setOperatorId(operator.getId());
        log.setOperatorName(operator.getUsername());
        log.setTenantId(operator.getTenantId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        opAuditLogMapper.insert(log);
    }
}
