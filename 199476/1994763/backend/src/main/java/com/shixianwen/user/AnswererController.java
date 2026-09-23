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
        @PathVariable String uid,
        @RequestParam(required = false) Long experienceId
    ) {
        return ApiResponse.ok(answererService.detail(currentUser.getId(), uid, experienceId));
    }

    @GetMapping("/library")
    public ApiResponse<AnswererService.ExperienceLibraryPage> library(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "FAVORITES") String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(answererService.library(currentUser.getId(), type, page, size));
    }

    @DeleteMapping("/library/recent/{certificationId}")
    public ApiResponse<Void> deleteRecentView(
        @CurrentUser User currentUser,
        @PathVariable Long certificationId
    ) {
        answererService.deleteRecentView(currentUser.getId(), certificationId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/library/recent")
    public ApiResponse<Void> clearRecentViews(@CurrentUser User currentUser) {
        answererService.clearRecentViews(currentUser.getId());
        return ApiResponse.ok();
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

    @PutMapping("/{uid}/experiences/{certificationId}/favorite")
    public ApiResponse<AnswererService.ExperienceFavoriteView> setExperienceFavorite(
        @CurrentUser User currentUser,
        @PathVariable String uid,
        @PathVariable Long certificationId,
        @RequestBody ExperienceFavoriteRequest request
    ) {
        return ApiResponse.ok(answererService.setExperienceFavorite(
            currentUser.getId(), uid, certificationId, request.favorited()
        ));
    }

    public record ExperienceFavoriteRequest(boolean favorited) {}

}
