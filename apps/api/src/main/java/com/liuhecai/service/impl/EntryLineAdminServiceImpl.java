package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.dto.EntryLineItemRequest;
import com.liuhecai.dto.EntryLinesSaveRequest;
import com.liuhecai.entity.Domain;
import com.liuhecai.entity.EntryLine;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.mapper.EntryLineMapper;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.service.EntryLineAdminService;
import com.liuhecai.service.ForumHostResolver;
import com.liuhecai.tenant.DomainTenantLookup;
import com.liuhecai.vo.EntryLineAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryLineAdminServiceImpl implements EntryLineAdminService {

    private static final String[] DEFAULT_LABELS = {
            "电信临时线路", "移动临时线路", "联通临时线路", "广电临时线路", "澳门直达专线"
    };
    private static final String[] DEFAULT_COLORS = {
            "#c62828", "#1565c0", "#2e7d32", "#6a1b9a", "#ef6c00"
    };

    private final EntryLineMapper entryLineMapper;
    private final DomainMapper domainMapper;
    private final TenantMapper tenantMapper;
    private final ForumHostResolver forumHostResolver;
    private final DomainTenantLookup domainTenantLookup;

    @Override
    public List<EntryLineAdminVO> listByDomainId(Long domainId) {
        requireEntryDomain(domainId);
        List<EntryLine> rows = entryLineMapper.selectList(new LambdaQueryWrapper<EntryLine>()
                .eq(EntryLine::getEntryDomainId, domainId)
                .orderByAsc(EntryLine::getSortOrder)
                .orderByAsc(EntryLine::getId));
        return toAdminVos(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<EntryLineAdminVO> replaceLines(Long domainId, EntryLinesSaveRequest request) {
        requireEntryDomain(domainId);
        List<EntryLineItemRequest> items = request.getLines() == null ? List.of() : request.getLines();
        for (EntryLineItemRequest item : items) {
            validateTargetTenant(item.getTargetTenantId());
            if (item.getStatus() == null || (item.getStatus() != 0 && item.getStatus() != 1)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "status 只能为 0 或 1");
            }
        }
        entryLineMapper.delete(new LambdaQueryWrapper<EntryLine>()
                .eq(EntryLine::getEntryDomainId, domainId));
        LocalDateTime now = LocalDateTime.now();
        int idx = 0;
        for (EntryLineItemRequest item : items) {
            EntryLine row = new EntryLine();
            row.setEntryDomainId(domainId);
            row.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : (++idx));
            row.setLabel(item.getLabel().trim());
            row.setColor(item.getColor().trim());
            row.setTargetTenantId(item.getTargetTenantId());
            row.setStatus(item.getStatus());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            entryLineMapper.insert(row);
        }
        domainTenantLookup.evictAll();
        return listByDomainId(domainId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seedDefaultsIfEmpty(Long entryDomainId, Long ownerTenantId) {
        if (entryDomainId == null || ownerTenantId == null) {
            return;
        }
        Long count = entryLineMapper.selectCount(new LambdaQueryWrapper<EntryLine>()
                .eq(EntryLine::getEntryDomainId, entryDomainId));
        if (count != null && count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < DEFAULT_LABELS.length; i++) {
            EntryLine row = new EntryLine();
            row.setEntryDomainId(entryDomainId);
            row.setSortOrder(i + 1);
            row.setLabel(DEFAULT_LABELS[i]);
            row.setColor(DEFAULT_COLORS[i]);
            row.setTargetTenantId(ownerTenantId);
            row.setStatus(1);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            entryLineMapper.insert(row);
        }
        domainTenantLookup.evictAll();
    }

    private Domain requireEntryDomain(Long domainId) {
        Domain domain = domainMapper.selectById(domainId);
        if (domain == null || domain.getStatus() == null || domain.getStatus() != 1) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND, "入口域名不存在或已停用");
        }
        String role = StringUtils.hasText(domain.getRole()) ? domain.getRole().trim().toUpperCase() : "FORUM";
        if (!"ENTRY".equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅 ENTRY 域名可配置线路");
        }
        return domain;
    }

    private void validateTargetTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "目标租户不存在或已停用");
        }
        String forumHost = forumHostResolver.resolveForumHost(tenantId);
        if (!StringUtils.hasText(forumHost)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "目标租户「" + tenant.getName() + "」没有可用的论坛域名");
        }
    }

    private List<EntryLineAdminVO> toAdminVos(List<EntryLine> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> tenantIds = rows.stream()
                .map(EntryLine::getTargetTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Tenant> tenants = new LinkedHashMap<>();
        if (!tenantIds.isEmpty()) {
            for (Tenant t : tenantMapper.selectBatchIds(tenantIds)) {
                tenants.put(t.getId(), t);
            }
        }
        List<EntryLineAdminVO> out = new ArrayList<>(rows.size());
        for (EntryLine row : rows) {
            EntryLineAdminVO vo = new EntryLineAdminVO();
            vo.setId(row.getId());
            vo.setSortOrder(row.getSortOrder());
            vo.setLabel(row.getLabel());
            vo.setColor(row.getColor());
            vo.setTargetTenantId(row.getTargetTenantId());
            vo.setStatus(row.getStatus());
            Tenant t = tenants.get(row.getTargetTenantId());
            if (t != null) {
                vo.setTargetTenantName(t.getName());
            }
            vo.setTargetForumHost(forumHostResolver.resolveForumHost(row.getTargetTenantId()));
            out.add(vo);
        }
        return out;
    }
}
