package com.liuhecai.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class TenantResolveFilter extends OncePerRequestFilter {

    private final DomainTenantLookup domainTenantLookup;
    private final ObjectMapper objectMapper;

    @Value("${liuhecai.tenant.trust-forwarded-host:false}")
    private boolean trustForwardedHost;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // admin / agent 后台按登录态绑定租户，不依赖浏览器 Host（代理域如 agent.xxx 通常未进 domains）
        return path.startsWith("/api/health")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/agent/")
                || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String host = resolveHost(request);
            DomainTenantLookup.ResolvedTenant resolved = domainTenantLookup.resolveEnabled(host);
            TenantContext.set(resolved.tenantId());
            TenantContext.setDomainId(resolved.domainId());
            TenantContext.setHost(resolved.host());
            TenantContext.setDomainRole(resolved.domainRole());
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            writeBusinessError(response, e.getCode(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void writeBusinessError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }

    private String resolveHost(HttpServletRequest request) {
        if (trustForwardedHost) {
            String forwarded = request.getHeader("X-Forwarded-Host");
            if (StringUtils.hasText(forwarded)) {
                return normalizeHost(forwarded.split(",")[0].trim());
            }
        }
        String hostHeader = request.getHeader("Host");
        if (StringUtils.hasText(hostHeader)) {
            return normalizeHost(hostHeader);
        }
        return normalizeHost(request.getServerName());
    }

    private String normalizeHost(String host) {
        String value = host.trim().toLowerCase();
        int colon = value.indexOf(':');
        if (colon > -1) {
            value = value.substring(0, colon);
        }
        return value;
    }
}
