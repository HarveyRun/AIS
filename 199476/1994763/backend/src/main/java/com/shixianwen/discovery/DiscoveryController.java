package com.shixianwen.discovery;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/discovery")
@RequiredArgsConstructor
public class DiscoveryController {
    private final DiscoveryService service;

    @GetMapping("/matter-categories")
    public ApiResponse<DiscoveryService.MainCategoryView> matterCategories(
        @RequestParam String mainCategory
    ) {
        return ApiResponse.ok(service.category(mainCategory, DiscoveryService.ContentType.MATTERS));
    }

    @GetMapping("/experience-categories")
    public ApiResponse<DiscoveryService.MainCategoryView> experienceCategories(
        @RequestParam String mainCategory
    ) {
        return ApiResponse.ok(service.category(mainCategory, DiscoveryService.ContentType.EXPERIENCES));
    }

    @GetMapping("/matters/search")
    public ApiResponse<List<DiscoveryService.MatterSearchView>> searchMatters(
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.ok(service.searchMatters(keyword));
    }

    @GetMapping("/experiences/search")
    public ApiResponse<List<DiscoveryService.ExperienceSearchView>> searchExperiences(
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.ok(service.searchExperiences(keyword));
    }

    @GetMapping("/matters/{id}")
    public ApiResponse<DiscoveryService.MatterView> matter(@PathVariable Long id) {
        return ApiResponse.ok(service.matter(id));
    }

}
