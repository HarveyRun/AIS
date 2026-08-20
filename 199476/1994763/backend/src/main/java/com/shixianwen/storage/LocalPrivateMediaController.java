package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/public/local-media")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalPrivateMediaController {
    private final Path root;
    private final LocalMediaSigner signer;

    public LocalPrivateMediaController(
        @Value("${app.storage.root}") String root,
        LocalMediaSigner signer
    ) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.signer = signer;
    }

    @GetMapping
    public ResponseEntity<FileSystemResource> media(
        @RequestParam String key,
        @RequestParam long expires,
        @RequestParam String signature
    ) {
        signer.verify(key, expires, signature);
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root.resolve("private").normalize()) || !Files.isRegularFile(target)) {
            throw BusinessException.notFound("文件不存在");
        }
        String detected;
        try {
            detected = Files.probeContentType(target);
        } catch (Exception ignored) {
            detected = null;
        }
        MediaType contentType = detected == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(detected);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(contentType)
            .body(new FileSystemResource(target));
    }
}
