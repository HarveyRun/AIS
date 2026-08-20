package com.shixianwen.common;

import com.shixianwen.auth.AccountPenaltyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.network.ClientIpExtractor;
import com.shixianwen.user.User;
import com.shixianwen.auth.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SecurityEventService securityEvents;
    private final ClientIpExtractor clientIpExtractor;
    @ExceptionHandler(AccountPenaltyException.class)
    public ResponseEntity<ApiResponse<AccountPenaltyView>> handleAccountPenalty(
        AccountPenaltyException exception
    ) {
        AccountPenaltyView detail = new AccountPenaltyView(
            "ACCOUNT_PENALTY",
            exception.getReason(),
            exception.getBanUntil(),
            exception.isPermanent()
        );
        return ResponseEntity
            .status(exception.getStatus())
            .body(new ApiResponse<>(false, detail, exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
        BusinessException exception,
        HttpServletRequest request
    ) {
        if (exception.getStatus() == HttpStatus.FORBIDDEN) {
            Object current = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
            Long userId = current instanceof User user ? user.getId() : null;
            securityEvents.recordSafely(
                userId, null, "ACCESS_DENIED", "HIGH", clientIpExtractor.extract(request),
                request.getHeader("X-Device-Id"), "path=" + request.getRequestURI()
            );
        }
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = error == null ? "请求参数不正确" : error.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data constraint violation", exception);
        return ResponseEntity.badRequest().body(ApiResponse.error("数据重复或仍有关联，请检查后重试"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("请求数据格式不正确"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("内容不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception exception) {
        log.error("Unhandled request error", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.error("服务暂时不可用，请稍后重试"));
    }

    public record AccountPenaltyView(
        String type,
        String reason,
        java.time.LocalDateTime banUntil,
        boolean permanent
    ) {
    }
}
