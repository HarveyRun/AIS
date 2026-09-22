package com.shixianwen.certification;

import com.shixianwen.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ExperienceCategoryController {
    private final ExperienceCategoryService service;

    public ExperienceCategoryController(ExperienceCategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/experience-categories")
    public ApiResponse<List<ExperienceCategoryService.CategoryView>> available() {
        return ApiResponse.ok(service.available());
    }

    @GetMapping("/api/admin/experience-categories")
    public ApiResponse<List<ExperienceCategoryService.CategoryView>> all() {
        return ApiResponse.ok(service.all());
    }

    @PostMapping("/api/admin/experience-categories")
    public ApiResponse<ExperienceCategoryService.CategoryView> create(
        @RequestBody ExperienceCategoryService.CategoryInput input
    ) {
        return ApiResponse.ok(service.save(null, input));
    }

    @PutMapping("/api/admin/experience-categories/{id}")
    public ApiResponse<ExperienceCategoryService.CategoryView> update(
        @PathVariable Long id, @RequestBody ExperienceCategoryService.CategoryInput input
    ) {
        return ApiResponse.ok(service.save(id, input));
    }

    @PatchMapping("/api/admin/experience-categories/{id}/enabled")
    public ApiResponse<ExperienceCategoryService.CategoryView> enabled(
        @PathVariable Long id, @RequestBody EnabledInput input
    ) {
        return ApiResponse.ok(service.setEnabled(id, input.enabled()));
    }

    @DeleteMapping("/api/admin/experience-categories/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    public record EnabledInput(boolean enabled) {}
}
