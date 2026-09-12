package com.shixianwen.curated;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/curated-chat")
@RequiredArgsConstructor
public class CuratedChatAdminController {
    private final CuratedChatService service;

    @GetMapping("/applications")
    public ApiResponse<PageResult> applications(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        int p=Math.max(page,0),s=Math.max(1,Math.min(size,100));
        return ApiResponse.ok(new PageResult(service.adminApplications(keyword,status,p,s),service.adminApplicationCount(keyword,status),p,s));
    }
    @GetMapping("/applications/{id}/materials") public ApiResponse<List<CuratedChatService.MaterialView>> materials(@PathVariable Long id){return ApiResponse.ok(service.adminMaterials(id));}
    @PostMapping("/applications/{id}/job-review") public ApiResponse<Void> job(@CurrentAdmin AdminUser admin,@PathVariable Long id,@RequestBody JobReview request,HttpServletRequest servlet){service.reviewJob(admin,id,request.approved(),request.jobTitle(),request.jobYears(),request.reason(),ip(servlet));return ApiResponse.ok();}
    @PatchMapping("/members/{userId}/status") public ApiResponse<Void> member(@CurrentAdmin AdminUser admin,@PathVariable Long userId,@RequestBody MemberStatus request,HttpServletRequest servlet){service.suspendMembership(admin,userId,request.active(),request.reason(),ip(servlet));return ApiResponse.ok();}
    private String ip(HttpServletRequest r){String f=r.getHeader("X-Forwarded-For");return f==null?r.getRemoteAddr():f.split(",")[0].trim();}
    public record PageResult(List<Map<String,Object>> content,long totalElements,int page,int size){}
    public record JobReview(boolean approved,String jobTitle,Integer jobYears,String reason){}
    public record MemberStatus(boolean active,String reason){}
}
