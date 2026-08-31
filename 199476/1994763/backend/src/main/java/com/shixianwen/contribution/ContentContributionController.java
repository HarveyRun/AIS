package com.shixianwen.contribution;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-contributions")
@RequiredArgsConstructor
public class ContentContributionController {
    private final ContentContributionService service;

    @GetMapping("/summary")
    public ApiResponse<ContentContributionService.UserSummary> summary(@CurrentUser User user) {
        return ApiResponse.ok(service.summary(user.getId()));
    }

    @GetMapping
    public ApiResponse<ContentContributionService.PageResult> list(
        @CurrentUser User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.userList(user.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentContributionService.ContributionView> detail(
        @CurrentUser User user,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.userDetail(user.getId(), id));
    }

    @PostMapping
    public ApiResponse<ContentContributionService.ContributionView> submit(
        @CurrentUser User user,
        @RequestBody ContentContributionService.SubmitCommand command
    ) {
        return ApiResponse.ok(service.submit(user, command));
    }
}
