package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.util.PageLimits;
import com.liuhecai.entity.OpAuditLog;
import com.liuhecai.mapper.OpAuditLogMapper;
import com.liuhecai.service.AdminAuditService;
import com.liuhecai.vo.OpAuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private final OpAuditLogMapper opAuditLogMapper;

    @Override
    public PageResult<OpAuditLogVO> page(String action, String operatorName, String targetType, int page, int size) {
        int safePage = PageLimits.clampPage(page);
        int safeSize = PageLimits.clampSize(size);

        LambdaQueryWrapper<OpAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            wrapper.eq(OpAuditLog::getAction, action.trim());
        }
        if (StringUtils.hasText(operatorName)) {
            wrapper.like(OpAuditLog::getOperatorName, operatorName.trim());
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(OpAuditLog::getTargetType, targetType.trim());
        }
        wrapper.orderByDesc(OpAuditLog::getCreatedAt);

        Page<OpAuditLog> mpPage = opAuditLogMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<OpAuditLogVO> records = mpPage.getRecords().stream().map(this::toVo).toList();

        PageResult<OpAuditLogVO> result = new PageResult<>();
        result.setTotal(mpPage.getTotal());
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }

    private OpAuditLogVO toVo(OpAuditLog log) {
        OpAuditLogVO vo = new OpAuditLogVO();
        vo.setId(log.getId());
        vo.setOperatorRealm(log.getOperatorRealm());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setTenantId(log.getTenantId());
        vo.setAction(log.getAction());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
