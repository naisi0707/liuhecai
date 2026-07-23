package com.liuhecai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.dto.AgentSiteConfigRequest;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.service.AgentSiteService;
import com.liuhecai.service.HtmlSanitizeService;
import com.liuhecai.vo.AgentSiteConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSiteServiceImpl implements AgentSiteService {

    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;
    private final HtmlSanitizeService htmlSanitizeService;

    @Override
    public AgentSiteConfigVO getConfig() {
        return toVO(requireAgentTenant());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSiteConfigVO updateConfig(AgentSiteConfigRequest request) {
        Tenant tenant = requireAgentTenant();
        tenant.setName(request.getName().trim());
        String announcement = trimToNull(request.getAnnouncement());
        if (announcement != null) {
            announcement = htmlSanitizeService.sanitize(announcement);
        }
        tenant.setAnnouncement(announcement);
        tenant.setKefuWechat(trimToNull(request.getKefuWechat()));
        tenant.setKefuQq(trimToNull(request.getKefuQq()));

        Map<String, Object> theme = parseTheme(tenant.getThemeJson());
        theme.put("primaryColor", defaultIfBlank(request.getPrimaryColor(), "#c62828"));
        theme.put("fontFamily", defaultIfBlank(request.getFontFamily(), "Microsoft YaHei"));
        theme.put("logoUrl", htmlSanitizeService.requireSafeResourceUrl(
                defaultIfBlank(request.getLogoUrl(), ""), "logoUrl"));
        theme.put("adBanner", defaultIfBlank(request.getAdBanner(), ""));
        try {
            tenant.setThemeJson(objectMapper.writeValueAsString(theme));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "主题保存失败");
        }
        tenantMapper.updateById(tenant);
        return toVO(tenantMapper.selectById(tenant.getId()));
    }

    private Tenant requireAgentTenant() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Tenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    private AgentSiteConfigVO toVO(Tenant tenant) {
        Map<String, Object> theme = parseTheme(tenant.getThemeJson());
        AgentSiteConfigVO vo = new AgentSiteConfigVO();
        vo.setTenantId(tenant.getId());
        vo.setName(tenant.getName());
        vo.setAnnouncement(tenant.getAnnouncement());
        vo.setKefuWechat(tenant.getKefuWechat());
        vo.setKefuQq(tenant.getKefuQq());
        vo.setPrimaryColor(asString(theme.get("primaryColor"), "#c62828"));
        vo.setFontFamily(asString(theme.get("fontFamily"), "Microsoft YaHei"));
        vo.setLogoUrl(asString(theme.get("logoUrl"), ""));
        vo.setAdBanner(asString(theme.get("adBanner"), ""));
        vo.setThemeJson(tenant.getThemeJson());
        return vo;
    }

    private Map<String, Object> parseTheme(String themeJson) {
        if (!StringUtils.hasText(themeJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(themeJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Invalid theme_json for tenant, fallback empty: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
