package com.shixianwen.discovery;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/discovery")
@RequiredArgsConstructor
public class DiscoveryController {
    private final DiscoveryService service;

    @GetMapping("/catalog")
    public ApiResponse<DiscoveryService.CatalogView> catalog() {
        return ApiResponse.ok(service.catalog());
    }
}
