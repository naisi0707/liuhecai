package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.config.CacheConfig;
import com.liuhecai.entity.DemoNote;
import com.liuhecai.entity.Domain;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.DemoNoteMapper;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.service.TenantQueryService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.DemoNoteVO;
import com.liuhecai.vo.TenantDirectoryItemVO;
import com.liuhecai.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQueryServiceImpl implements TenantQueryService {

    private final TenantMapper tenantMapper;
    private final DemoNoteMapper demoNoteMapper;
    private final DomainMapper domainMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Cacheable(
            cacheNames = CacheConfig.TENANT_CURRENT,
            key = "T(com.liuhecai.tenant.TenantContext).get() + ':' + T(com.liuhecai.tenant.TenantContext).getHost()")
    public TenantVO getCurrentTenant() {
        Long tenantId = requireTenantId();
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED);
        }
        Map<String, Object> theme = parseTheme(tenant.getThemeJson());
        TenantVO vo = new TenantVO();
        vo.setId(tenant.getId());
        vo.setName(tenant.getName());
        vo.setStatus(tenant.getStatus());
        vo.setThemeJson(tenant.getThemeJson());
        vo.setPrimaryColor(asString(theme.get("primaryColor"), "#c62828"));
        vo.setFontFamily(asString(theme.get("fontFamily"), "Microsoft YaHei"));
        vo.setLogoUrl(asString(theme.get("logoUrl"), ""));
        vo.setAdBanner(asString(theme.get("adBanner"), ""));
        vo.setKefuWechat(tenant.getKefuWechat());
        vo.setKefuQq(tenant.getKefuQq());
        vo.setAnnouncement(tenant.getAnnouncement());
        vo.setHost(TenantContext.getHost());
        String role = TenantContext.getDomainRole();
        vo.setDomainRole(StringUtils.hasText(role) ? role : "FORUM");
        vo.setForumHost(resolveForumHost(tenantId));
        return vo;
    }

    private String resolveForumHost(Long tenantId) {
        List<Domain> domains = domainMapper.selectList(new LambdaQueryWrapper<Domain>()
                .eq(Domain::getTenantId, tenantId)
                .eq(Domain::getStatus, 1)
                .orderByDesc(Domain::getIsPrimary)
                .orderByAsc(Domain::getId));
        List<Domain> forums = domains.stream()
                .filter(d -> {
                    String r = d.getRole();
                    return !StringUtils.hasText(r) || "FORUM".equalsIgnoreCase(r.trim());
                })
                .toList();
        String current = TenantContext.getHost();
        // entry.xxx → 优先配对论坛域 xxx
        if (StringUtils.hasText(current) && current.toLowerCase().startsWith("entry.")) {
            String paired = current.substring("entry.".length()).toLowerCase();
            for (Domain d : forums) {
                if (paired.equalsIgnoreCase(d.getHost())) {
                    return d.getHost();
                }
            }
        }
        if (!forums.isEmpty()) {
            return forums.get(0).getHost();
        }
        return domains.stream()
                .map(Domain::getHost)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<DemoNoteVO> listCurrentDemoNotes() {
        requireTenantId();
        List<DemoNote> notes = demoNoteMapper.selectList(new LambdaQueryWrapper<>());
        return notes.stream().map(note -> {
            DemoNoteVO vo = new DemoNoteVO();
            vo.setId(note.getId());
            vo.setTitle(note.getTitle());
            vo.setContent(note.getContent());
            return vo;
        }).toList();
    }

    @Override
    public List<TenantDirectoryItemVO> listOtherTenants() {
        Long currentId = requireTenantId();
        List<Tenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1)
                .ne(Tenant::getId, currentId)
                .orderByAsc(Tenant::getId));
        return tenants.stream().map(this::toDirectoryItem).toList();
    }

    private TenantDirectoryItemVO toDirectoryItem(Tenant tenant) {
        Map<String, Object> theme = parseTheme(tenant.getThemeJson());
        TenantDirectoryItemVO vo = new TenantDirectoryItemVO();
        vo.setId(tenant.getId());
        vo.setName(tenant.getName());
        vo.setLogoUrl(asString(theme.get("logoUrl"), ""));
        vo.setPrimaryColor(asString(theme.get("primaryColor"), "#c62828"));
        vo.setPrimaryHost(null);
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        return tenantId;
    }

    private Map<String, Object> parseTheme(String themeJson) {
        if (!StringUtils.hasText(themeJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(themeJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Invalid theme_json: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }
}
