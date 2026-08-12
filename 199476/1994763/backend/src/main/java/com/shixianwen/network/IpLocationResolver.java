package com.shixianwen.network;

import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
public class IpLocationResolver {
    private static final String UNKNOWN = "未知";
    private static final String PRIVATE_NETWORK = "内网";

    private final Ip2Region ip2Region;

    public IpLocationResolver(
        @Value("${app.ip-location.ipv4-xdb-path:./ip2region_v4.xdb}") String ipv4XdbPath,
        @Value("${app.ip-location.ipv6-xdb-path:./ip2region_v6.xdb}") String ipv6XdbPath
    ) {
        Path ipv4Path = requiredDatabase(ipv4XdbPath, "IPv4");
        Path ipv6Path = requiredDatabase(ipv6XdbPath, "IPv6");

        try {
            Config ipv4Config = Config.custom()
                .setCachePolicy(Config.VIndexCache)
                .setSearchers(20)
                .setXdbPath(ipv4Path.toString())
                .asV4();
            Config ipv6Config = Config.custom()
                .setCachePolicy(Config.VIndexCache)
                .setSearchers(20)
                .setXdbPath(ipv6Path.toString())
                .asV6();
            this.ip2Region = Ip2Region.create(ipv4Config, ipv6Config);
        } catch (Exception exception) {
            throw new IllegalStateException("IP归属地数据库加载失败", exception);
        }
    }

    public String resolve(String ipAddress) {
        if (isPrivateAddress(ipAddress)) return PRIVATE_NETWORK;

        try {
            return format(ip2Region.search(ipAddress));
        } catch (Exception ignored) {
            return UNKNOWN;
        }
    }

    @PreDestroy
    public void close() {
        try {
            ip2Region.close();
        } catch (Exception ignored) {
            // Spring关闭时不因地址库资源释放失败阻断其他组件。
        }
    }

    private static Path requiredDatabase(String configuredPath, String version) {
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(version + " IP归属地数据库不可读：" + path);
        }
        return path;
    }

    private static String format(String region) {
        if (region == null || region.isBlank()) return UNKNOWN;

        String[] fields = region.split("\\|", -1);
        List<String> locations = Arrays.stream(fields)
            .limit(3)
            .map(String::trim)
            .filter(value -> !value.isEmpty() && !"0".equals(value))
            .distinct()
            .toList();
        return locations.isEmpty() ? UNKNOWN : String.join(" ", locations);
    }

    private static boolean isPrivateAddress(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress();
        } catch (Exception ignored) {
            return true;
        }
    }
}
