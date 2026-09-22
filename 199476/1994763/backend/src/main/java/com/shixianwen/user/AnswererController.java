package com.shixianwen.user;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

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
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(
            answererService.search(
                currentUser.getId(),
                keyword,
                sortBy,
                sortDirection,
                categoryId,
                page,
                size
            )
        );
    }

    @GetMapping("/{uid}")
    public ApiResponse<AnswererService.AnswererView> detail(
        @CurrentUser User currentUser,
        @PathVariable String uid
    ) {
        return ApiResponse.ok(answererService.detail(currentUser.getId(), uid));
    }

    @PutMapping("/{uid}/experiences/{certificationId}/like")
    public ApiResponse<AnswererService.ExperienceLikeView> setExperienceLike(
        @CurrentUser User currentUser,
        @PathVariable String uid,
        @PathVariable Long certificationId,
        @RequestBody ExperienceLikeRequest request
    ) {
        return ApiResponse.ok(
            answererService.setExperienceLike(currentUser.getId(), uid, certificationId, request.liked())
        );
    }

    public record ExperienceLikeRequest(boolean liked) {}

}
