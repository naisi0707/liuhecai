package com.liuhecai.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.Result;
import com.liuhecai.entity.Domain;
import com.liuhecai.entity.Tenant;
import com.liuhecai.common.enums.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.mapper.TenantMapper;
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

    private final DomainMapper domainMapper;
    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    @Value("${liuhecai.tenant.trust-forwarded-host:false}")
    private boolean trustForwardedHost;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/health")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/agent/auth/")
                || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String host = resolveHost(request);
            Domain domain = domainMapper.selectOne(new LambdaQueryWrapper<Domain>()
                    .eq(Domain::getHost, host)
                    .eq(Domain::getStatus, 1)
                    .last("LIMIT 1"));
            if (domain == null) {
                writeBusinessError(response, ErrorCode.DOMAIN_NOT_FOUND, "域名未绑定站点");
                return;
            }
            Tenant tenant = tenantMapper.selectById(domain.getTenantId());
            if (tenant == null) {
                writeBusinessError(response, ErrorCode.TENANT_NOT_FOUND, ErrorCode.TENANT_NOT_FOUND.getMessage());
                return;
            }
            if (tenant.getStatus() == null || tenant.getStatus() != 1) {
                writeBusinessError(response, ErrorCode.TENANT_DISABLED, ErrorCode.TENANT_DISABLED.getMessage());
                return;
            }
            TenantContext.set(tenant.getId());
            TenantContext.setHost(host);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeBusinessError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(errorCode.getCode(), message));
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
