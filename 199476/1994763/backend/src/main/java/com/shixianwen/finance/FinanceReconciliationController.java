package com.shixianwen.finance;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/finance-reconciliation")
@RequiredArgsConstructor
public class FinanceReconciliationController {
    private final FinanceReconciliationService service;

    @GetMapping("/summary") public ApiResponse<Map<String,Object>> summary(){ return ApiResponse.ok(service.summary()); }
    @GetMapping("/tasks") public ApiResponse<FinanceReconciliationService.PageResult> tasks(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.tasks(Math.max(0,page),Math.max(1,Math.min(100,size))));}
    @GetMapping("/differences") public ApiResponse<FinanceReconciliationService.PageResult> differences(@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.differences(status,Math.max(0,page),Math.max(1,Math.min(100,size))));}
    @GetMapping("/vouchers") public ApiResponse<FinanceReconciliationService.PageResult> vouchers(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.vouchers(keyword,Math.max(0,page),Math.max(1,Math.min(100,size))));}
    @PostMapping("/run") public ApiResponse<Map<String,Object>> run(@CurrentAdmin AdminUser admin,HttpServletRequest request){return ApiResponse.ok(service.runInternal(admin,ip(request)));}
    @PostMapping("/alipay-bills") public ApiResponse<Map<String,Object>> alipay(@CurrentAdmin AdminUser admin,@RequestPart MultipartFile file,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate billDate,HttpServletRequest request){return ApiResponse.ok(service.importAlipayBill(admin,file,billDate,ip(request)));}
    @PostMapping("/withdrawal-results") public ApiResponse<Map<String,Object>> withdrawals(@CurrentAdmin AdminUser admin,@RequestPart MultipartFile file,HttpServletRequest request){return ApiResponse.ok(service.importWithdrawalResults(admin,file,ip(request)));}
    @PostMapping("/differences/{id}/resolve") public ApiResponse<Void> resolve(@CurrentAdmin AdminUser admin,@PathVariable Long id,@RequestBody ResolveRequest body,HttpServletRequest request){service.resolveDifference(admin,id,body.resolution(),ip(request));return ApiResponse.ok();}
    private String ip(HttpServletRequest request){String value=request.getHeader("X-Forwarded-For");return value==null?request.getRemoteAddr():value.split(",")[0].trim();}
    public record ResolveRequest(String resolution){}
}
