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
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class MediaProxyService {
    private static final int MAX_IMAGE_BYTES = 15 * 1024 * 1024;

    private final HttpClient httpClient;
    private final Set<String> allowedHosts;
    private final FileTypeDetector fileTypeDetector;

    public MediaProxyService(ThirdPartySettings settings, FileTypeDetector fileTypeDetector) {
        this.fileTypeDetector = fileTypeDetector;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.allowedHosts = allowedHosts(settings);
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
                FileTypeDetector.DetectedFile detected = fileTypeDetector.detect(bytes);
                if (!"IMAGE".equals(detected.kind())) {
                    throw BusinessException.badRequest("图片内容无法识别");
                }
                return new ProxiedImage(bytes, detected.contentType());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessException.serviceUnavailable("图片暂时无法加载");
        } catch (IOException exception) {
            throw BusinessException.serviceUnavailable("图片暂时无法加载");
        }
    }

    private URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || allowedHosts.isEmpty()) {
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
            || !allowedHosts.contains(host)
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

    private static Set<String> allowedHosts(ThirdPartySettings settings) {
        Set<String> hosts = new LinkedHashSet<>();
        addHost(hosts, settings.value("app.storage.oss.public-domain", "oss.domain"));

        String endpointHost = hostOf(settings.value("app.storage.oss.endpoint", "oss.endpoint"));
        if (!endpointHost.isBlank()) {
            hosts.add(endpointHost);
            addBucketHost(hosts, settings.value("app.storage.oss.public-bucket"), endpointHost);
            addBucketHost(hosts, settings.value("app.storage.oss.bucket", "oss.bucket"), endpointHost);
            addBucketHost(hosts, settings.value("app.storage.oss.private-bucket"), endpointHost);
        }
        return Set.copyOf(hosts);
    }

    private static void addHost(Set<String> hosts, String value) {
        String host = hostOf(value);
        if (!host.isBlank()) hosts.add(host);
    }

    private static void addBucketHost(Set<String> hosts, String bucket, String endpointHost) {
        if (bucket != null && !bucket.isBlank()) {
            hosts.add(bucket.trim().toLowerCase(Locale.ROOT) + "." + endpointHost);
        }
    }

    public record ProxiedImage(byte[] content, String contentType) {
    }
}
