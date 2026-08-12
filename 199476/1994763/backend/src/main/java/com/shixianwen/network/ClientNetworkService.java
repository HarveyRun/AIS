package com.shixianwen.network;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientNetworkService {
    private final ClientIpExtractor ipExtractor;
    private final IpLocationResolver locationResolver;

    public ClientNetworkInfo resolve(HttpServletRequest request) {
        String ipAddress = ipExtractor.extract(request);
        return new ClientNetworkInfo(ipAddress, locationResolver.resolve(ipAddress));
    }
}
