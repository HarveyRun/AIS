package com.diancheng.api;

import com.diancheng.service.AuthService;
import com.diancheng.service.BusinessService;
import com.diancheng.service.SnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppController {
    private final AuthService auth;
    private final BusinessService business;
    private final SnapshotService snapshots;

    public AppController(AuthService auth, BusinessService business, SnapshotService snapshots) {
        this.auth = auth;
        this.business = business;
        this.snapshots = snapshots;
    }

    @GetMapping("/health")
    public Map<String, Object> health() { return Map.of("ok", true, "service", "diancheng-server"); }

    @GetMapping("/state")
    public Map<String, Object> state(HttpServletRequest request) {
        String email = auth.currentEmail(request).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionEmail", email == null ? "" : email);
        result.put("data", snapshots.snapshot(email));
        return result;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Login body) { return auth.login(body.email(), body.password()); }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Register body) { return auth.register(body.email(), body.password(), body.confirmation(), body.inviteDigits()); }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) { auth.logout(request); return Map.of("ok", true); }

    @PostMapping("/ideas")
    public Map<String, Object> addIdea(HttpServletRequest request, @RequestBody IdeaCreate body) {
        return business.addIdea(auth.requireUser(request), body.type(), body.text(), body.parentId(), body.isPublic());
    }

    @PatchMapping("/ideas/{id}/visibility")
    public Map<String, Object> visibility(HttpServletRequest request, @PathVariable String id) {
        business.toggleVisibility(auth.requireUser(request), id); return Map.of("ok", true);
    }

    @PostMapping("/ideas/{id}/like")
    public Map<String, Object> like(HttpServletRequest request, @PathVariable String id) {
        return Map.of("ok", true, "liked", business.toggleLike(auth.requireUser(request), id));
    }

    @PostMapping("/ideas/{id}/pay")
    public Map<String, Object> payIdea(HttpServletRequest request, @PathVariable String id) {
        return business.payIdea(auth.requireUser(request), id);
    }

    @PostMapping("/admin/ideas/{id}/evaluate")
    public Map<String, Object> evaluate(HttpServletRequest request, @PathVariable String id, @RequestBody Evaluation body) {
        return business.evaluateIdea(auth.requireAdmin(request), body.owner(), id, body.level(), body.decision(), body.fee());
    }

    @PatchMapping("/admin/ideas/{id}/status")
    public Map<String, Object> ideaStatus(HttpServletRequest request, @PathVariable String id, @RequestBody IdeaStatus body) {
        business.updateIdeaStatus(auth.requireAdmin(request), body.owner(), id, body.status()); return Map.of("ok", true);
    }

    @PostMapping("/wallet/recharge")
    public Map<String, Object> recharge(HttpServletRequest request, @RequestBody Amount body) {
        business.recharge(auth.requireUser(request), body.amount()); return Map.of("ok", true);
    }

    @PostMapping("/packages/purchase")
    public Map<String, Object> purchase(HttpServletRequest request, @RequestBody PackagePurchase body) {
        return business.purchasePackage(auth.requireUser(request), body.packageId());
    }

    @PutMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest request, @RequestBody Profile body) {
        business.updateProfile(auth.requireUser(request), body.name()); return Map.of("ok", true);
    }

    @PutMapping("/profile/password")
    public Map<String, Object> password(HttpServletRequest request, @RequestBody Password body) {
        return business.changePassword(auth.requireUser(request), body.current(), body.next(), auth.currentToken(request).orElse(""));
    }

    @DeleteMapping("/profile")
    public Map<String, Object> deleteProfile(HttpServletRequest request) {
        business.deleteAccount(auth.requireUser(request)); return Map.of("ok", true);
    }

    @PostMapping("/feedback")
    public Map<String, Object> feedback(HttpServletRequest request, @RequestBody FeedbackCreate body) {
        return business.createFeedback(auth.currentEmail(request).orElse(null), body.content(), body.page(), body.category());
    }

    @PostMapping("/feedback/{id}/messages")
    public Map<String, Object> feedbackMessage(HttpServletRequest request, @PathVariable String id, @RequestBody FeedbackReply body) {
        String actor = auth.requireUser(request);
        if ("admin".equals(body.role())) auth.requireAdmin(request);
        return business.appendFeedback(id, body.role(), actor, body.content());
    }

    @PostMapping("/feedback/{id}/close")
    public Map<String, Object> closeFeedback(HttpServletRequest request, @PathVariable String id) {
        business.closeFeedback(id, auth.requireUser(request)); return Map.of("ok", true);
    }

    @PatchMapping("/notifications/{id}/read")
    public Map<String, Object> readNotification(HttpServletRequest request, @PathVariable String id) {
        business.markNotification(auth.requireUser(request), id); return Map.of("ok", true);
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Object> readAll(HttpServletRequest request) {
        business.markAllNotifications(auth.requireUser(request)); return Map.of("ok", true);
    }

    @PostMapping("/notifications/read-business")
    public Map<String, Object> readBusiness(HttpServletRequest request, @RequestBody BusinessId body) {
        business.markBusinessNotifications(auth.requireUser(request), body.businessId()); return Map.of("ok", true);
    }

    @PostMapping("/notifications/derive")
    public Map<String, Object> derive(HttpServletRequest request) {
        business.ensureDerivedNotifications(auth.requireUser(request)); return Map.of("ok", true);
    }

    @PutMapping("/team/application")
    public Map<String, Object> teamApplication(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        business.submitTeamApplication(auth.requireUser(request), body); return Map.of("ok", true);
    }

    @PatchMapping("/admin/team/{email}/status")
    public Map<String, Object> teamStatus(HttpServletRequest request, @PathVariable String email, @RequestBody TeamStatus body) {
        business.updateTeamStatus(auth.requireAdmin(request), email, body.status()); return Map.of("ok", true);
    }

    @PostMapping("/deposits")
    public Map<String, Object> deposit(HttpServletRequest request, @RequestBody Amount body) {
        return business.createDeposit(auth.requireUser(request), body.amount());
    }

    @PatchMapping("/admin/deposits/{id}")
    public Map<String, Object> depositStatus(HttpServletRequest request, @PathVariable String id, @RequestBody DepositStatus body) {
        business.updateDeposit(auth.requireAdmin(request), id, body.status()); return Map.of("ok", true);
    }

    public record Login(String email, String password) {}
    public record Register(String email, String password, String confirmation, String inviteDigits) {}
    public record IdeaCreate(String type, String text, String parentId, boolean isPublic) {}
    public record Evaluation(String owner, int level, String decision, BigDecimal fee) {}
    public record IdeaStatus(String owner, String status) {}
    public record Amount(BigDecimal amount) {}
    public record PackagePurchase(String packageId) {}
    public record Profile(String name) {}
    public record Password(String current, String next) {}
    public record FeedbackCreate(String content, String page, String category) {}
    public record FeedbackReply(String role, String content) {}
    public record BusinessId(String businessId) {}
    public record TeamStatus(String status) {}
    public record DepositStatus(String status) {}
}
