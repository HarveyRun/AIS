package com.shixianwen.realtime;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeController {
    private final RealtimeTicketService tickets;

    @PostMapping("/tickets")
    public ApiResponse<RealtimeTicketService.TicketView> ticket(@CurrentUser User user) {
        return ApiResponse.ok(tickets.issueUser(user.getId()));
    }
}
