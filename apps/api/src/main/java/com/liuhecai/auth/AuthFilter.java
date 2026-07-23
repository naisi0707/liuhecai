package com.liuhecai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.result.Result;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.service.AccountStatusService;
import com.liuhecai.service.TokenVersionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/health",
            "/api/tenant/",
            "/api/site/",
            "/api/demo-notes",
            "/api/draws/",
            "/api/admin/auth/",
            "/api/agent/auth/",
            "/api/user/auth/"
    );

    /** 可匿名访问，有 Token 则解析以判断是否已购 */
    private static final String OPTIONAL_AUTH_PREFIX = "/api/topics";

    private final JwtService jwtService;
    private final TokenVersionService tokenVersionService;
    private final AccountStatusService accountStatusService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)
                || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            boolean optionalAuth = path.startsWith(OPTIONAL_AUTH_PREFIX);
            String header = request.getHeader("Authorization");
            boolean hasBearer = StringUtils.hasText(header) && header.startsWith("Bearer ");

            if (!hasBearer) {
                if (optionalAuth) {
                    filterChain.doFilter(request, response);
                    return;
                }
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            AuthUser user;
            try {
                user = jwtService.parseToken(header.substring(7).trim());
            } catch (BusinessException e) {
                if (optionalAuth) {
                    filterChain.doFilter(request, response);
                    return;
                }
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            int dbVersion = tokenVersionService.currentVersion(user.getRealm(), user.getId());
            int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            if (tokenVersion != dbVersion) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            if (!accountStatusService.isActive(user.getRealm(), user.getId())) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            if (path.startsWith("/api/admin/") && user.getRealm() != AuthRealm.SUPER) {
                writeError(response, ErrorCode.FORBIDDEN);
                return;
            }
            if (path.startsWith("/api/agent/") && user.getRealm() != AuthRealm.AGENT) {
                writeError(response, ErrorCode.FORBIDDEN);
                return;
            }
            if (path.startsWith("/api/user/") && user.getRealm() != AuthRealm.USER) {
                writeError(response, ErrorCode.FORBIDDEN);
                return;
            }

            AuthContext.set(user);
            if (user.getRealm() == AuthRealm.USER) {
                Long hostTenant = TenantContext.get();
                if (hostTenant == null || !hostTenant.equals(user.getTenantId())) {
                    // 可选鉴权路径：跨租户 Token 降级匿名，避免硬失败；其它路径拒绝
                    if (optionalAuth) {
                        AuthContext.clear();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeError(response, ErrorCode.FORBIDDEN);
                    return;
                }
            } else if (user.getTenantId() != null && !optionalAuth) {
                // /api/topics 等可选路径禁止 AGENT/SUPER 用 JWT 覆盖 Host 租户（防串租）
                TenantContext.set(user.getTenantId());
            }
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(errorCode));
    }
}
