package com.shixianwen.network;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpExtractor {
    private final Set<String> trustedProxies;

    public ClientIpExtractor(
        @Value("${app.network.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String trustedProxies
    ) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public String extract(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!trustedProxies.contains(remoteAddress)) return remoteAddress;

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) return remoteAddress;

        String candidate = normalize(forwardedFor.split(",", 2)[0]);
        return isValid(candidate) ? candidate : remoteAddress;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "0.0.0.0";
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        int zoneIndex = normalized.indexOf('%');
        return zoneIndex < 0 ? normalized : normalized.substring(0, zoneIndex);
    }

    private static boolean isValid(String value) {
        if (value.length() > 45 || !value.matches("[0-9a-fA-F:.]+")) return false;
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
