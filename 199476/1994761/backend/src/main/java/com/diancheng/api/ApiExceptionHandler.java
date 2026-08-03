package com.diancheng.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException error) {
        return ResponseEntity.status(error.status()).body(Map.of("ok", false, "error", error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .findFirst().map(item -> item.getDefaultMessage()).orElse("提交内容不完整。");
        return ResponseEntity.badRequest().body(Map.of("ok", false, "error", message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnknown(Exception error) {
        error.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", "服务暂时不可用，请稍后重试。"));
    }
}
