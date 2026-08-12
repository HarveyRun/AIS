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
    @GetMapping("/jobs") public ApiResponse<List<Map<String,Object>>> jobs(){return ApiResponse.ok(service.jobs());}
    @GetMapping("/jobs/{id}/users") public ApiResponse<List<Map<String,Object>>> jobUsers(@PathVariable Long id){return ApiResponse.ok(service.jobUsers(id));}
    @PostMapping("/jobs") public ApiResponse<Void> createJob(@CurrentAdmin AdminUser a,@RequestBody JobRequest r,HttpServletRequest req){service.createJob(a,r.name(),r.description(),ip(req));return ApiResponse.ok();}
    @PutMapping("/jobs/{id}") public ApiResponse<Void> updateJob(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody JobRequest r,HttpServletRequest req){service.updateJob(a,id,r.name(),r.description(),r.active(),ip(req));return ApiResponse.ok();}
    @DeleteMapping("/jobs/{id}") public ApiResponse<Void> deleteJob(@CurrentAdmin AdminUser a,@PathVariable Long id,HttpServletRequest req){service.deleteJob(a,id,ip(req));return ApiResponse.ok();}
    @GetMapping("/{type:certifications|inquiries|withdrawals|feedback|cooperations}") public ApiResponse<AdminManagementService.PageResult> table(@PathVariable String type,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="")String category,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.table(type,status,category,safePage(page),safeSize(size)));}
    @GetMapping("/certifications/{id}/materials") public ApiResponse<List<Map<String,Object>>> materials(@PathVariable Long id){return ApiResponse.ok(service.certificationMaterials(id));}
    @PostMapping("/certifications/{id}/review") public ApiResponse<Void> review(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody ReviewRequest r,HttpServletRequest req){service.reviewCertification(a,id,r.approved(),r.reason(),r.jobId(),r.years(),ip(req));return ApiResponse.ok();}
    @PatchMapping("/withdrawals/{id}/status") public ApiResponse<Void> withdrawal(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody StatusRequest r,HttpServletRequest req){service.processWithdrawal(a,id,r.status(),ip(req));return ApiResponse.ok();}
    @PatchMapping("/{type:feedback|cooperations}/{id}/status") public ApiResponse<Void> record(@CurrentAdmin AdminUser a,@PathVariable String type,@PathVariable Long id,@RequestBody StatusRequest r,HttpServletRequest req){service.updateRecordStatus(a,type,id,r.status(),ip(req));return ApiResponse.ok();}
    @GetMapping("/audit-logs") public ApiResponse<AdminManagementService.PageResult> logs(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.auditLogs(safePage(page),safeSize(size)));}
    @GetMapping("/customer-service/conversations") public ApiResponse<List<Map<String,Object>>> conversations(){return ApiResponse.ok(service.customerServiceConversations());}
    @GetMapping("/customer-service/users/{userId}/messages") public ApiResponse<List<Map<String,Object>>> messages(@PathVariable Long userId){return ApiResponse.ok(service.customerServiceMessages(userId));}
    @PostMapping("/customer-service/users/{userId}/reply") public ApiResponse<Void> reply(@CurrentAdmin AdminUser a,@PathVariable Long userId,@RequestBody ReplyRequest r,HttpServletRequest req){service.replyCustomerService(a,userId,r.content(),ip(req));return ApiResponse.ok();}
    @GetMapping("/discovery") public ApiResponse<Map<String,Object>> discovery(){return ApiResponse.ok(service.discovery());}
    @PostMapping("/discovery/categories") public ApiResponse<Void> createCategory(@CurrentAdmin AdminUser a,@RequestBody CategoryRequest r,HttpServletRequest req){service.createCategory(a,r.mainCategory(),r.name(),r.sortOrder(),ip(req));return ApiResponse.ok();}
    @PutMapping("/discovery/categories/{id}") public ApiResponse<Void> updateCategory(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody CategoryRequest r,HttpServletRequest req){service.updateCategory(a,id,r.mainCategory(),r.name(),r.sortOrder(),r.active(),ip(req));return ApiResponse.ok();}
    @DeleteMapping("/discovery/categories/{id}") public ApiResponse<Void> deleteCategory(@CurrentAdmin AdminUser a,@PathVariable Long id,HttpServletRequest req){service.deleteCategory(a,id,ip(req));return ApiResponse.ok();}
    @PostMapping("/discovery/matters") public ApiResponse<Void> createMatter(@CurrentAdmin AdminUser a,@RequestBody MatterRequest r,HttpServletRequest req){service.createMatter(a,r.categoryId(),r.title(),r.sortOrder(),r.jobs(),ip(req));return ApiResponse.ok();}
    @PutMapping("/discovery/matters/{id}") public ApiResponse<Void> updateMatter(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody MatterRequest r,HttpServletRequest req){service.updateMatter(a,id,r.categoryId(),r.title(),r.sortOrder(),r.active(),r.jobs(),ip(req));return ApiResponse.ok();}
    @DeleteMapping("/discovery/matters/{id}") public ApiResponse<Void> deleteMatter(@CurrentAdmin AdminUser a,@PathVariable Long id,HttpServletRequest req){service.deleteMatter(a,id,ip(req));return ApiResponse.ok();}
    @PatchMapping("/discovery/experiences/{id}/category") public ApiResponse<Void> classifyExperience(@CurrentAdmin AdminUser a,@PathVariable Long id,@RequestBody ClassificationRequest r,HttpServletRequest req){service.classifyExperience(a,id,r.categoryId(),ip(req));return ApiResponse.ok();}
    private String ip(HttpServletRequest r){String f=r.getHeader("X-Forwarded-For");return f==null?r.getRemoteAddr():f.split(",")[0].trim();}
    private int safePage(int page){return Math.max(page,0);}
    private int safeSize(int size){return Math.max(1,Math.min(size,100));}
    public record StatusRequest(String status){} public record ReviewRequest(boolean approved,String reason,Long jobId,Integer years){} public record ReplyRequest(String content){}
    public record JobRequest(String name,String description,Boolean active){}
    public record CategoryRequest(String mainCategory,String name,Integer sortOrder,Boolean active){}
    public record MatterRequest(Long categoryId,String title,Integer sortOrder,Boolean active,List<MatterJobRequest> jobs){}
    public record MatterJobRequest(Long jobId,String type){}
    public record ClassificationRequest(Long categoryId){}
}
