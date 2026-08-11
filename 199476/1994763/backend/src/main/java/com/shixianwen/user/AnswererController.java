package com.shixianwen.user;

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
    public ApiResponse<List<AnswererService.AnswererView>> search(
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(answererService.search(keyword));
    }

    @GetMapping("/{uid}")
    public ApiResponse<AnswererService.AnswererView> detail(@PathVariable String uid) {
        return ApiResponse.ok(answererService.detail(uid));
    }
}
