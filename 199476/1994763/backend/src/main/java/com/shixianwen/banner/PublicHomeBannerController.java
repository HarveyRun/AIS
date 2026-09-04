package com.shixianwen.banner;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/banners")
@RequiredArgsConstructor
public class PublicHomeBannerController {
    private final HomeBannerService service;

    @GetMapping
    public ApiResponse<List<HomeBannerService.PublicBannerView>> list() {
        return ApiResponse.ok(service.publicBanners());
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<HomeBannerService.BannerAvailabilityView> availability(
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.availability(id));
    }
}
