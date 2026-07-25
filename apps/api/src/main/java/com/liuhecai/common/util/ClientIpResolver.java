package com.liuhecai.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String cf = trimIp(request.getHeader("CF-Connecting-IP"));
        if (cf != null) {
            return cf;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            for (String part : xff.split(",")) {
                String ip = trimIp(part);
                if (ip != null && !isPrivateOrLocal(ip)) {
                    return ip;
                }
            }
            for (String part : xff.split(",")) {
                String ip = trimIp(part);
                if (ip != null) {
                    return ip;
                }
            }
        }
        String xri = trimIp(request.getHeader("X-Real-IP"));
        if (xri != null) {
            return xri;
        }
        return trimIp(request.getRemoteAddr());
    }

    private static String trimIp(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String ip = raw.trim();
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else if (ip.contains(":") && ip.contains(".")) {
            // rare host:port for IPv4
            int idx = ip.lastIndexOf(':');
            if (idx > 0 && ip.indexOf(':') == idx) {
                ip = ip.substring(0, idx);
            }
        }
        return StringUtils.hasText(ip) ? ip : null;
    }

    static boolean isPrivateOrLocal(String ip) {
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
