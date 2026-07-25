package com.liuhecai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.result.Result;
import com.liuhecai.service.IpWhitelistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class IpWhitelistFilter extends OncePerRequestFilter {

    private final IpWhitelistService ipWhitelistService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/admin/") || path.startsWith("/api/agent/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!ipWhitelistService.isAllowed(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Result.fail(ErrorCode.IP_NOT_ALLOWED));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
