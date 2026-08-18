package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
public class MediaProxyService {
    private static final int MAX_IMAGE_BYTES = 15 * 1024 * 1024;

    private final HttpClient httpClient;
    private final String allowedHost;

    public MediaProxyService(ThirdPartySettings settings) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.allowedHost = hostOf(
            settings.value("app.storage.oss.public-domain", "oss.domain")
        );
    }

    public ProxiedImage loadImage(String rawUrl) {
        URI source = validate(rawUrl);
        HttpRequest request = HttpRequest.newBuilder(source)
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "image/*")
            .GET()
            .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw BusinessException.serviceUnavailable("图片暂时无法加载");
            }
            String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("application/octet-stream")
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("image/")) {
                throw BusinessException.badRequest("该地址不是图片");
            }
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
                if (bytes.length > MAX_IMAGE_BYTES) {
                    throw BusinessException.badRequest("图片过大，无法预览");
                }
                return new ProxiedImage(bytes, contentType);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessException.serviceUnavailable("图片暂时无法加载");
        } catch (IOException exception) {
            throw BusinessException.serviceUnavailable("图片暂时无法加载");
        }
    }

    private URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || allowedHost.isBlank()) {
            throw BusinessException.badRequest("图片地址无效");
        }
        final URI parsed;
        try {
            parsed = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest("图片地址无效");
        }
        String scheme = parsed.getScheme() == null
            ? ""
            : parsed.getScheme().toLowerCase(Locale.ROOT);
        String host = parsed.getHost() == null
            ? ""
            : parsed.getHost().toLowerCase(Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme))
            || !allowedHost.equals(host)
            || parsed.getUserInfo() != null
            || (parsed.getPort() != -1 && parsed.getPort() != 80 && parsed.getPort() != 443)) {
            throw BusinessException.badRequest("图片地址无效");
        }
        if ("http".equals(scheme)) {
            try {
                return new URI(
                    "https",
                    null,
                    parsed.getHost(),
                    -1,
                    parsed.getPath(),
                    parsed.getQuery(),
                    null
                );
            } catch (Exception exception) {
                throw BusinessException.badRequest("图片地址无效");
            }
        }
        return parsed;
    }

    private static String hostOf(String publicDomain) {
        if (publicDomain == null || publicDomain.isBlank()) return "";
        String normalized = publicDomain.trim();
        if (!normalized.contains("://")) normalized = "https://" + normalized;
        try {
            String host = URI.create(normalized).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    public record ProxiedImage(byte[] content, String contentType) {
    }
}
