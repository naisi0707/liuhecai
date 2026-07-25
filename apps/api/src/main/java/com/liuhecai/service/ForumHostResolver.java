package com.liuhecai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.entity.Domain;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ForumHostResolver {

    private final DomainMapper domainMapper;

    /** 取租户主 FORUM 域（优先 is_primary） */
    public String resolveForumHost(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
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

    /**
     * 构造前台可跳转的论坛绝对 URL。
     * 本地 / *.local → 同页 Host + ?host=；生产非本地一律 https://forumHost/
     * （避免 TENANT_CURRENT 缓存被 HTTP 探测写成 http://）
     */
    public String buildForumUrl(String forumHost) {
        if (!StringUtils.hasText(forumHost)) {
            return null;
        }
        String host = forumHost.trim().toLowerCase();
        String requestHost = currentRequestHost();
        boolean pageIsLocal = isLocalHost(requestHost);
        boolean forumIsLocal = isLocalHost(host) || host.endsWith(".local");
        if (pageIsLocal || forumIsLocal) {
            String scheme = currentScheme();
            String page = StringUtils.hasText(requestHost) ? requestHost : "127.0.0.1";
            return scheme + "://" + page + "/?host=" + host;
        }
        return "https://" + host + "/";
    }

    private static boolean isLocalHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String h = host.toLowerCase();
        return "127.0.0.1".equals(h) || "localhost".equals(h);
    }

    private String currentScheme() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-Proto");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim().toLowerCase();
            }
            return request.getScheme();
        }
        return "https";
    }

    private String currentRequestHost() {
        String fromCtx = TenantContext.getHost();
        if (StringUtils.hasText(fromCtx)) {
            return fromCtx.toLowerCase();
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwarded)) {
            return normalizeHost(forwarded.split(",")[0].trim());
        }
        String hostHeader = request.getHeader("Host");
        if (StringUtils.hasText(hostHeader)) {
            return normalizeHost(hostHeader);
        }
        return normalizeHost(request.getServerName());
    }

    private static String normalizeHost(String host) {
        String value = host.trim().toLowerCase();
        int colon = value.indexOf(':');
        if (colon > -1) {
            value = value.substring(0, colon);
        }
        return value;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
