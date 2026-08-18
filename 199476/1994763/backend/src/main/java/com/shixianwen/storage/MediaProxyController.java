package com.shixianwen.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/public/media")
@RequiredArgsConstructor
public class MediaProxyController {
    private final MediaProxyService mediaProxyService;

    @GetMapping("/image")
    public ResponseEntity<byte[]> image(@RequestParam String url) {
        MediaProxyService.ProxiedImage image = mediaProxyService.loadImage(url);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
            .body(image.content());
    }
}
