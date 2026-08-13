package com.shixianwen.user;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answerers")
public class AnswererController {
    private final AnswererService answererService;

    public AnswererController(AnswererService answererService) {
        this.answererService = answererService;
    }

    @GetMapping
    public ApiResponse<AnswererService.AnswererPage> search(
        @CurrentUser User currentUser,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(answererService.search(currentUser.getId(), keyword, page, size));
    }

    @GetMapping("/{uid}")
    public ApiResponse<AnswererService.AnswererView> detail(@PathVariable String uid) {
        return ApiResponse.ok(answererService.detail(uid));
    }

    @GetMapping("/by-matter/{matterId}")
    public ApiResponse<List<AnswererService.AnswererView>> byMatter(@PathVariable Long matterId) {
        return ApiResponse.ok(answererService.forMatter(matterId));
    }

    @GetMapping("/by-experience")
    public ApiResponse<List<AnswererService.AnswererView>> byExperience(
        @RequestParam Long experienceId
    ) {
        return ApiResponse.ok(answererService.forExperience(experienceId));
    }
}
