package com.shixianwen.admin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminManagementController {
    private final AdminManagementService service;
    @GetMapping("/dashboard") public ApiResponse<Map<String,Object>> dashboard(){return ApiResponse.ok(service.dashboard());}
    @GetMapping("/users") public ApiResponse<AdminManagementService.PageResult> users(@RequestParam(defaultValue="")String keyword,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.users(keyword,status,safePage(page),safeSize(size)));}
    @PatchMapping("/users/{id}/status") public ApiResponse<Void> userStatus(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody StatusRequest r,HttpServletRequest req){service.userStatus(a,id,r.status(),ip(req));return ApiResponse.ok();}
    @GetMapping("/{type:certifications|inquiries|withdrawals|feedback|cooperations}") public ApiResponse<AdminManagementService.PageResult> table(@PathVariable String type,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.table(type,status,safePage(page),safeSize(size)));}
    @GetMapping("/certifications/{id}/materials") public ApiResponse<List<Map<String,Object>>> materials(@PathVariable Long id){return ApiResponse.ok(service.certificationMaterials(id));}
    @PostMapping("/certifications/{id}/review") public ApiResponse<Void> review(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody ReviewRequest r,HttpServletRequest req){service.reviewCertification(a,id,r.approved(),r.reason(),ip(req));return ApiResponse.ok();}
    @PatchMapping("/withdrawals/{id}/status") public ApiResponse<Void> withdrawal(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody StatusRequest r,HttpServletRequest req){service.processWithdrawal(a,id,r.status(),ip(req));return ApiResponse.ok();}
    @PatchMapping("/{type:feedback|cooperations}/{id}/status") public ApiResponse<Void> record(@CurrentAdmin AdminUser a,@PathVariable String type,@PathVariable Long id,@RequestBody StatusRequest r,HttpServletRequest req){service.updateRecordStatus(a,type,id,r.status(),ip(req));return ApiResponse.ok();}
    @GetMapping("/audit-logs") public ApiResponse<AdminManagementService.PageResult> logs(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.auditLogs(safePage(page),safeSize(size)));}
    @GetMapping("/customer-service/conversations") public ApiResponse<List<Map<String,Object>>> conversations(){return ApiResponse.ok(service.customerServiceConversations());}
    @GetMapping("/customer-service/users/{userId}/messages") public ApiResponse<List<Map<String,Object>>> messages(@PathVariable Long userId){return ApiResponse.ok(service.customerServiceMessages(userId));}
    @PostMapping("/customer-service/users/{userId}/reply") public ApiResponse<Void> reply(@CurrentAdmin AdminUser a,@PathVariable Long userId,@RequestBody ReplyRequest r,HttpServletRequest req){service.replyCustomerService(a,userId,r.content(),ip(req));return ApiResponse.ok();}
    private String ip(HttpServletRequest r){String f=r.getHeader("X-Forwarded-For");return f==null?r.getRemoteAddr():f.split(",")[0].trim();}
    private int safePage(int page){return Math.max(page,0);}
    private int safeSize(int size){return Math.max(1,Math.min(size,100));}
    public record StatusRequest(String status){} public record ReviewRequest(boolean approved,String reason){} public record ReplyRequest(String content){}
}
