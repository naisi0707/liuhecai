package com.liuhecai.common.util;

import org.springframework.util.StringUtils;

public final class Ipv4CidrMatcher {

    private Ipv4CidrMatcher() {
    }

    public static boolean matches(String clientIp, String cidrOrIp) {
        if (!StringUtils.hasText(clientIp) || !StringUtils.hasText(cidrOrIp)) {
            return false;
        }
        String target = cidrOrIp.trim();
        String ip = clientIp.trim();
        if (!isIpv4(ip)) {
            return ip.equalsIgnoreCase(target);
        }
        if (!target.contains("/")) {
            return ip.equals(target);
        }
        String[] parts = target.split("/", 2);
        if (parts.length != 2 || !isIpv4(parts[0])) {
            return false;
        }
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (prefix < 0 || prefix > 32) {
            return false;
        }
        long base = ipv4ToLong(parts[0]);
        long client = ipv4ToLong(ip);
        if (prefix == 0) {
            return true;
        }
        long mask = (-1L << (32 - prefix)) & 0xFFFFFFFFL;
        return (base & mask) == (client & mask);
    }

    public static boolean isValidCidrOrIp(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String v = value.trim();
        if (!v.contains("/")) {
            return isIpv4(v);
        }
        String[] parts = v.split("/", 2);
        if (parts.length != 2 || !isIpv4(parts[0])) {
            return false;
        }
        try {
            int prefix = Integer.parseInt(parts[1].trim());
            return prefix >= 0 && prefix <= 32;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isIpv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String p : parts) {
            if (p.isEmpty() || (p.length() > 1 && p.startsWith("0"))) {
                return false;
            }
            try {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        long v = 0;
        for (String p : parts) {
            v = (v << 8) | Integer.parseInt(p);
        }
        return v & 0xFFFFFFFFL;
    }
}
