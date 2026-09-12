package com.shixianwen.curated;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/curated-chat")
@RequiredArgsConstructor
public class CuratedChatController {
    private final CuratedChatService service;
    @Value("${app.payment.mock-return-url:http://localhost:5173/profile/wallet}")
    private String mockReturnUrl;

    @GetMapping("/membership")
    public ApiResponse<CuratedChatService.MembershipView> membership(@CurrentUser User user) {
        return ApiResponse.ok(service.status(user.getId()));
    }

    @PostMapping(value="/membership/application", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CuratedChatService.MembershipView> apply(
        @CurrentUser User user,
        @RequestPart(value="jobFiles", required=false) List<MultipartFile> jobFiles
    ) {
        return ApiResponse.ok(service.submitApplication(user.getId(), jobFiles));
    }

    @GetMapping("/membership/materials")
    public ApiResponse<List<CuratedChatService.MaterialView>> materials(@CurrentUser User user) {
        return ApiResponse.ok(service.myMaterials(user.getId()));
    }

    @PostMapping("/membership/orders")
    public ApiResponse<CuratedChatService.PaymentView> createPayment(
        @CurrentUser User user,
        @Valid @RequestBody PaymentRequest request
    ) {
        return ApiResponse.ok(service.createPayment(user.getId(), request.requestId()));
    }

    @GetMapping("/membership/orders/{orderNo}")
    public ApiResponse<CuratedChatService.PaymentView> payment(
        @CurrentUser User user,
        @PathVariable String orderNo
    ) {
        return ApiResponse.ok(service.payment(user.getId(), orderNo));
    }

    @GetMapping(value="/membership/mock-cashier", produces=MediaType.TEXT_HTML_VALUE)
    public String mockCashier(@RequestParam String orderNo) {
        CuratedChatService.PaymentView order=service.mockOrder(orderNo);
        return "<!doctype html><html lang=\"zh-CN\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"+
            "<title>严选直聊模拟支付</title><style>body{margin:0;background:#f5f5f5;font-family:sans-serif;color:#222}.card{max-width:420px;margin:60px auto;background:#fff;padding:28px;border-radius:18px}strong{display:block;font-size:34px;margin:28px 0;color:#1677ff}button{width:100%;border:0;border-radius:12px;padding:14px;background:#1677ff;color:#fff;font-size:16px}</style>"+
            "<div class=\"card\"><h1>开通严选直聊</h1><p>订单号："+order.orderNo()+"</p><strong>¥"+order.amount().stripTrailingZeros().toPlainString()+"</strong><form method=\"post\" action=\"/api/curated-chat/membership/mock-payment\"><input type=\"hidden\" name=\"orderNo\" value=\""+order.orderNo()+"\"><button type=\"submit\">确认支付</button></form></div></html>";
    }

    @PostMapping(value="/membership/mock-payment", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> mockPayment(@RequestParam String orderNo) {
        service.completeMockPayment(orderNo);
        return ResponseEntity.status(303).location(URI.create(mockReturnUrl+"?curated=success&orderNo="+orderNo)).build();
    }

    @GetMapping("/members")
    public ApiResponse<CuratedChatService.PageView<CuratedChatService.MemberView>> members(
        @CurrentUser User user,
        @RequestParam(defaultValue="") String keyword,
        @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size
    ) {
        return ApiResponse.ok(service.onlineMembers(user.getId(),keyword,Math.max(page,0),Math.max(1,Math.min(size,50))));
    }

    @PostMapping("/conversations")
    public ApiResponse<CuratedChatService.ConversationView> open(
        @CurrentUser User user,
        @RequestBody ConversationRequest request
    ) {
        return ApiResponse.ok(service.openConversation(user.getId(),request.otherUserId()));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<CuratedChatService.ConversationView>> conversations(@CurrentUser User user) {
        return ApiResponse.ok(service.conversations(user.getId()));
    }

    @GetMapping("/conversations/unread-count")
    public ApiResponse<Long> unread(@CurrentUser User user) { return ApiResponse.ok(service.unreadCount(user.getId())); }

    @GetMapping("/conversations/{id}")
    public ApiResponse<CuratedChatService.ConversationView> conversation(@CurrentUser User user,@PathVariable Long id) {
        return ApiResponse.ok(service.conversation(user.getId(),id,true));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<CuratedChatService.MessageView>> messages(@CurrentUser User user,@PathVariable Long id,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="0") long beforeId,@RequestParam(defaultValue="100") int limit) {
        return ApiResponse.ok(service.messages(user.getId(),id,afterId,beforeId,limit));
    }

    @PutMapping("/conversations/{id}/read")
    public ApiResponse<Void> read(@CurrentUser User user,@PathVariable Long id) { service.read(user.getId(),id); return ApiResponse.ok(); }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<CuratedChatService.MessageView> send(@CurrentUser User user,@PathVariable Long id,@Valid @RequestBody MessageRequest request) {
        return ApiResponse.ok(service.sendText(user.getId(),id,request.content()));
    }

    @PostMapping(value="/conversations/{id}/images", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CuratedChatService.MessageView> image(@CurrentUser User user,@PathVariable Long id,@RequestPart("image") MultipartFile image) {
        return ApiResponse.ok(service.sendImage(user.getId(),id,image));
    }

    @PostMapping("/conversations/{id}/block")
    public ApiResponse<Void> block(@CurrentUser User user,@PathVariable Long id) { service.block(user.getId(),id); return ApiResponse.ok(); }

    @PostMapping("/conversations/{id}/voice-calls")
    public ApiResponse<CuratedChatService.VoiceCallView> startCall(@CurrentUser User user,@PathVariable Long id) { return ApiResponse.ok(service.startCall(user.getId(),id)); }
    @GetMapping("/voice-calls/{id}") public ApiResponse<CuratedChatService.VoiceCallView> call(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.call(user.getId(),id));}
    @GetMapping("/voice-calls/{id}/ice-config") public ApiResponse<CuratedChatService.IceConfig> ice(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.iceConfig(user.getId(),id));}
    @PostMapping("/voice-calls/{id}/answer") public ApiResponse<CuratedChatService.VoiceCallView> answer(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.answerCall(user.getId(),id));}
    @PostMapping("/voice-calls/{id}/reject") public ApiResponse<CuratedChatService.VoiceCallView> reject(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.rejectCall(user.getId(),id));}
    @PostMapping("/voice-calls/{id}/connected") public ApiResponse<CuratedChatService.VoiceCallView> connected(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.connected(user.getId(),id));}
    @PostMapping("/voice-calls/{id}/end") public ApiResponse<CuratedChatService.VoiceCallView> end(@CurrentUser User user,@PathVariable Long id){return ApiResponse.ok(service.endCall(user.getId(),id));}
    @PostMapping("/voice-calls/{id}/signals") public ApiResponse<CuratedChatService.SignalView> signal(@CurrentUser User user,@PathVariable Long id,@Valid @RequestBody SignalRequest request){return ApiResponse.ok(service.signal(user.getId(),id,request.type(),request.payload()));}
    @GetMapping("/voice-calls/{id}/signals") public ApiResponse<List<CuratedChatService.SignalView>> signals(@CurrentUser User user,@PathVariable Long id,@RequestParam(defaultValue="0") long afterId){return ApiResponse.ok(service.signals(user.getId(),id,afterId));}

    public record PaymentRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{12,64}") String requestId) {}
    public record ConversationRequest(Long otherUserId) {}
    public record MessageRequest(@NotBlank String content) {}
    public record SignalRequest(@NotBlank String type,@NotBlank String payload) {}
}
