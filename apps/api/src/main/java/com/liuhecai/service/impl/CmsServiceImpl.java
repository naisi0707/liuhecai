package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.config.CacheConfig;
import com.liuhecai.dto.CmsMenusSaveRequest;
import com.liuhecai.dto.CmsPageSaveRequest;
import com.liuhecai.entity.SiteMenu;
import com.liuhecai.entity.SitePage;
import com.liuhecai.mapper.SiteMenuMapper;
import com.liuhecai.mapper.SitePageMapper;
import com.liuhecai.service.CmsSeedService;
import com.liuhecai.service.CmsService;
import com.liuhecai.service.HtmlSanitizeService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.SiteMenuVO;
import com.liuhecai.vo.SitePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsServiceImpl implements CmsService {

    private static final Set<String> ALLOWED_PAGE_KEYS = Set.of("home", "rules", "recharge", "kefu");

    private final SiteMenuMapper siteMenuMapper;
    private final SitePageMapper sitePageMapper;
    private final CmsSeedService cmsSeedService;
    private final ObjectMapper objectMapper;
    private final HtmlSanitizeService htmlSanitizeService;

    @Override
    @Cacheable(cacheNames = CacheConfig.CMS_MENUS, key = "T(com.liuhecai.tenant.TenantContext).get()")
    public List<SiteMenuVO> listPublicMenus() {
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        return siteMenuMapper.selectList(new LambdaQueryWrapper<SiteMenu>()
                        .eq(SiteMenu::getTenantId, tenantId)
                        .eq(SiteMenu::getVisible, 1)
                        .orderByAsc(SiteMenu::getSortNo)
                        .orderByAsc(SiteMenu::getId))
                .stream()
                .map(this::toMenuVO)
                .toList();
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.CMS_PAGES,
            key = "T(com.liuhecai.tenant.TenantContext).get() + ':' + #pageKey")
    public SitePageVO getPublicPage(String pageKey) {
        validatePageKey(pageKey);
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        SitePage page = findPage(tenantId, pageKey);
        if (page == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "页面不存在: " + pageKey);
        }
        return toPageVO(page);
    }

    @Override
    public List<SiteMenuVO> listAgentMenus() {
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        return siteMenuMapper.selectList(new LambdaQueryWrapper<SiteMenu>()
                        .eq(SiteMenu::getTenantId, tenantId)
                        .orderByAsc(SiteMenu::getSortNo)
                        .orderByAsc(SiteMenu::getId))
                .stream()
                .map(this::toMenuVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheConfig.CMS_MENUS, CacheConfig.CMS_PAGES}, allEntries = true)
    public List<SiteMenuVO> saveAgentMenus(CmsMenusSaveRequest request) {
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        Map<String, SiteMenu> existingByCode = siteMenuMapper.selectList(new LambdaQueryWrapper<SiteMenu>()
                        .eq(SiteMenu::getTenantId, tenantId))
                .stream()
                .collect(Collectors.toMap(SiteMenu::getCode, Function.identity(), (a, b) -> a));
        Set<String> codes = new HashSet<>();
        for (CmsMenusSaveRequest.Item item : request.getItems()) {
            if (!codes.add(item.getCode())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单 code 重复: " + item.getCode());
            }
            validateMenuPath(item.getPath());
            SiteMenu existing = existingByCode.get(item.getCode());
            if (existing == null) {
                SiteMenu m = new SiteMenu();
                m.setTenantId(tenantId);
                m.setCode(item.getCode().trim());
                m.setTitle(item.getTitle().trim());
                m.setPath(item.getPath().trim());
                m.setSortNo(item.getSortNo());
                m.setVisible(item.getVisible() != null && item.getVisible() == 1 ? 1 : 0);
                siteMenuMapper.insert(m);
            } else {
                existing.setTitle(item.getTitle().trim());
                existing.setPath(item.getPath().trim());
                existing.setSortNo(item.getSortNo());
                existing.setVisible(item.getVisible() != null && item.getVisible() == 1 ? 1 : 0);
                siteMenuMapper.updateById(existing);
            }
        }
        return listAgentMenus();
    }

    @Override
    public List<SitePageVO> listAgentPages() {
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        return sitePageMapper.selectList(new LambdaQueryWrapper<SitePage>()
                        .eq(SitePage::getTenantId, tenantId)
                        .orderByAsc(SitePage::getPageKey))
                .stream()
                .map(this::toPageVO)
                .toList();
    }

    @Override
    public SitePageVO getAgentPage(String pageKey) {
        return getPublicPage(pageKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheConfig.CMS_MENUS, CacheConfig.CMS_PAGES}, allEntries = true)
    public SitePageVO saveAgentPage(String pageKey, CmsPageSaveRequest request) {
        Long tenantId = requireTenantId();
        cmsSeedService.seedDefaultsIfEmpty(tenantId);
        String key = pageKey.trim();
        validatePageKey(key);
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageKey 不能为空");
        }
        SitePage page = findPage(tenantId, key);
        JsonNode sanitized = sanitizePageContent(key, request.getContent());
        String json;
        try {
            json = objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content JSON 无效");
        }
        if (page == null) {
            page = new SitePage();
            page.setTenantId(tenantId);
            page.setPageKey(key);
            page.setTitle(request.getTitle().trim());
            page.setContentJson(json);
            sitePageMapper.insert(page);
        } else {
            page.setTitle(request.getTitle().trim());
            page.setContentJson(json);
            sitePageMapper.updateById(page);
        }
        return toPageVO(findPage(tenantId, key));
    }

    private JsonNode sanitizePageContent(String pageKey, JsonNode content) {
        if (content == null || content.isNull()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content 不能为空");
        }
        var root = content.deepCopy();
        if (!(root instanceof com.fasterxml.jackson.databind.node.ObjectNode obj)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content 必须是对象");
        }
        switch (pageKey) {
            case "rules" -> {
                sanitizeTextField(obj, "intro");
                if (obj.has("guarantees") && obj.get("guarantees").isArray()) {
                    for (JsonNode g : obj.get("guarantees")) {
                        if (g instanceof com.fasterxml.jackson.databind.node.ObjectNode go) {
                            sanitizeHtmlField(go, "body");
                        }
                    }
                }
            }
            case "recharge" -> {
                sanitizeHtmlField(obj, "declareText");
                if (obj.has("notes") && obj.get("notes").isArray()) {
                    var arr = objectMapper.createArrayNode();
                    for (JsonNode n : obj.get("notes")) {
                        arr.add(htmlSanitizeService.sanitize(n.asText("")));
                    }
                    obj.set("notes", arr);
                }
                sanitizeUrlField(obj, "qrWechatUrl");
                sanitizeUrlField(obj, "qrQqUrl");
            }
            case "kefu" -> {
                sanitizeHtmlField(obj, "intro");
                sanitizeUrlField(obj, "qrWechatUrl");
                sanitizeUrlField(obj, "qrQqUrl");
            }
            case "home" -> {
                sanitizeUrlField(obj, "bannerUrl");
                sanitizeUrlField(obj, "drawIframeUrl");
                sanitizeUrlField(obj, "liveIframeUrl");
                sanitizeUrlField(obj, "qrWechatUrl");
                sanitizeUrlField(obj, "qrQqUrl");
                if (obj.has("bottomImages") && obj.get("bottomImages").isArray()) {
                    for (JsonNode img : obj.get("bottomImages")) {
                        if (img instanceof com.fasterxml.jackson.databind.node.ObjectNode io) {
                            sanitizeUrlField(io, "src");
                        }
                    }
                }
                if (obj.has("sisterSites") && obj.get("sisterSites").isArray()) {
                    for (JsonNode s : obj.get("sisterSites")) {
                        if (s instanceof com.fasterxml.jackson.databind.node.ObjectNode so) {
                            sanitizeUrlField(so, "href");
                        }
                    }
                }
            }
            default -> {
                // other keys: no-op
            }
        }
        return obj;
    }

    private void sanitizeHtmlField(com.fasterxml.jackson.databind.node.ObjectNode obj, String field) {
        if (!obj.has(field) || obj.get(field).isNull()) {
            return;
        }
        obj.put(field, htmlSanitizeService.sanitize(obj.get(field).asText("")));
    }

    private void sanitizeTextField(com.fasterxml.jackson.databind.node.ObjectNode obj, String field) {
        // intro on rules can stay plain or html — treat as html-safe text
        sanitizeHtmlField(obj, field);
    }

    private void sanitizeUrlField(com.fasterxml.jackson.databind.node.ObjectNode obj, String field) {
        if (!obj.has(field) || obj.get(field).isNull()) {
            return;
        }
        String raw = obj.get(field).asText("");
        if (!StringUtils.hasText(raw)) {
            obj.put(field, "");
            return;
        }
        obj.put(field, htmlSanitizeService.requireSafeResourceUrl(raw, field));
    }

    private void validatePageKey(String pageKey) {
        if (!StringUtils.hasText(pageKey) || !ALLOWED_PAGE_KEYS.contains(pageKey.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 pageKey");
        }
    }

    private void validateMenuPath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单 path 不能为空");
        }
        String value = path.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("://")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单 path 必须是站内相对路径");
        }
    }

    private SitePage findPage(Long tenantId, String pageKey) {
        return sitePageMapper.selectOne(new LambdaQueryWrapper<SitePage>()
                .eq(SitePage::getTenantId, tenantId)
                .eq(SitePage::getPageKey, pageKey)
                .last("LIMIT 1"));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        return tenantId;
    }

    private SiteMenuVO toMenuVO(SiteMenu m) {
        SiteMenuVO vo = new SiteMenuVO();
        vo.setId(String.valueOf(m.getId()));
        vo.setCode(m.getCode());
        vo.setTitle(m.getTitle());
        vo.setPath(m.getPath());
        vo.setSortNo(m.getSortNo());
        vo.setVisible(m.getVisible());
        return vo;
    }

    private SitePageVO toPageVO(SitePage page) {
        SitePageVO vo = new SitePageVO();
        vo.setId(String.valueOf(page.getId()));
        vo.setPageKey(page.getPageKey());
        vo.setTitle(page.getTitle());
        try {
            JsonNode node = objectMapper.readTree(page.getContentJson());
            vo.setContent(node);
        } catch (Exception e) {
            vo.setContent(objectMapper.createObjectNode());
        }
        return vo;
    }
}
