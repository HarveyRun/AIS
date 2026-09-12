package com.shixianwen.certification;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class CertificationMediaProcessor {
    private static final int MAX_ENTRIES = 500;
    private static final long MAX_EXPANDED_BYTES = 8L * 1024 * 1024 * 1024;
    private static final long MAX_SINGLE_FILE_BYTES = 2L * 1024 * 1024 * 1024;
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    private final CertificationMediaStateService state;
    private final FileStorage storage;
    private final FileTypeDetector fileTypes;

    public CertificationMediaProcessor(
        CertificationMediaStateService state,
        FileStorage storage,
        FileTypeDetector fileTypes
    ) {
        this.state = state;
        this.storage = storage;
        this.fileTypes = fileTypes;
    }

    @Async("certificationMediaExecutor")
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    public void process(CertificationMediaExtractionRequested event) {
        Long certificationId = event.certificationId();
        if (!running.add(certificationId)) return;
        Path workDir = null;
        try {
            CertificationMediaStateService.SourceArchive source = state.begin(certificationId);
            workDir = Files.createTempDirectory("experience-media-");
            Path archivePath = workDir.resolve("source-archive");
            storage.copyTo(source.storageKey(), StorageVisibility.PRIVATE, archivePath);
            ExtractionContext context = new ExtractionContext(certificationId, source, workDir);
            if ("application/vnd.rar".equalsIgnoreCase(source.contentType())) {
                extractRar(archivePath, context);
            } else {
                extractZip(archivePath, context);
            }
            state.ready(certificationId);
            log.info("Experience public media prepared, certificationId={}", certificationId);
        } catch (Exception exception) {
            state.failed(certificationId, readableError(exception));
            log.warn(
                "Experience public media preparation failed, certificationId={}",
                certificationId,
                exception
            );
        } finally {
            deleteTree(workDir);
            running.remove(certificationId);
        }
    }

    private void extractZip(Path archivePath, ExtractionContext context) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                context.countEntry();
                if (!entry.isDirectory()) {
                    Path extracted = context.nextTempFile();
                    long size = copyLimited(zip, extracted, entry.getSize(), context);
                    handleCandidate(extracted, leafName(entry.getName()), size, context);
                }
                zip.closeEntry();
            }
        }
    }

    private void extractRar(Path archivePath, ExtractionContext context) throws Exception {
        try (Archive archive = new Archive(archivePath.toFile())) {
            if (archive.isEncrypted() || archive.isPasswordProtected()) {
                throw new IllegalArgumentException("证明资料不能设置解压密码");
            }
            for (FileHeader header : archive.getFileHeaders()) {
                context.countEntry();
                if (header.isDirectory()) continue;
                if (header.isEncrypted()) {
                    throw new IllegalArgumentException("证明资料不能包含加密文件");
                }
                long declaredSize = header.getFullUnpackSize();
                context.ensureSize(declaredSize);
                Path extracted = context.nextTempFile();
                try (OutputStream output = Files.newOutputStream(extracted)) {
                    archive.extractFile(header, new LimitedOutputStream(output, context));
                }
                long size = Files.size(extracted);
                handleCandidate(extracted, leafName(header.getFileNameString()), size, context);
            }
        }
    }

    private long copyLimited(
        ZipInputStream input,
        Path target,
        long declaredSize,
        ExtractionContext context
    ) throws IOException {
        context.ensureSize(declaredSize);
        long written = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream output = Files.newOutputStream(target)) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                written += count;
                context.addBytes(count);
                if (written > MAX_SINGLE_FILE_BYTES) {
                    throw new IllegalArgumentException("压缩包内存在超过2GB的单个文件");
                }
                output.write(buffer, 0, count);
            }
        }
        return written;
    }

    private void handleCandidate(
        Path file,
        String originalName,
        long size,
        ExtractionContext context
    ) {
        if (size <= 0) return;
        FileTypeDetector.DetectedFile detected = fileTypes.detect(file, originalName);
        if (!java.util.Set.of("IMAGE", "VIDEO", "AUDIO").contains(detected.kind())) return;
        int number = context.nextTypeNumber(detected.kind());
        StoredFile stored = storage.store(
            file,
            originalName,
            context.storageFolder(),
            StorageVisibility.PRIVATE
        );
        state.add(
            context.certificationId,
            context.source.materialId(),
            detected.kind(),
            displayName(detected.kind(), number),
            originalName,
            stored,
            context.nextSortOrder()
        );
    }

    private static String displayName(String kind, int number) {
        return switch (kind) {
            case "IMAGE" -> "图片" + number;
            case "VIDEO" -> "视频" + number;
            case "AUDIO" -> "音频" + number;
            default -> "资料" + number;
        };
    }

    private static String leafName(String value) {
        if (value == null || value.isBlank()) return "资料";
        String normalized = value.replace('\\', '/');
        String leaf = normalized.substring(normalized.lastIndexOf('/') + 1);
        return leaf.length() > 500 ? leaf.substring(leaf.length() - 500) : leaf;
    }

    private static String readableError(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "证明资料整理失败，请联系平台处理";
        if (message.length() > 160) message = message.substring(0, 160);
        return "证明资料整理失败：" + message;
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static final class ExtractionContext {
        private final Long certificationId;
        private final CertificationMediaStateService.SourceArchive source;
        private final Path workDir;
        private final Map<String, Integer> typeCounts = new HashMap<>();
        private int entries;
        private int sortOrder;
        private long expandedBytes;

        private ExtractionContext(
            Long certificationId,
            CertificationMediaStateService.SourceArchive source,
            Path workDir
        ) {
            this.certificationId = certificationId;
            this.source = source;
            this.workDir = workDir;
        }

        private void countEntry() {
            entries++;
            if (entries > MAX_ENTRIES) throw new IllegalArgumentException("压缩包内文件数量不能超过500个");
        }

        private void ensureSize(long size) {
            if (size > MAX_SINGLE_FILE_BYTES) throw new IllegalArgumentException("压缩包内存在超过2GB的单个文件");
            if (size > 0 && expandedBytes + size > MAX_EXPANDED_BYTES) {
                throw new IllegalArgumentException("压缩包解压后的总大小超过8GB");
            }
        }

        private void addBytes(long count) {
            expandedBytes += count;
            if (expandedBytes > MAX_EXPANDED_BYTES) {
                throw new IllegalArgumentException("压缩包解压后的总大小超过8GB");
            }
        }

        private Path nextTempFile() throws IOException {
            return Files.createTempFile(workDir, "entry-", ".bin");
        }

        private int nextTypeNumber(String type) {
            return typeCounts.merge(type, 1, Integer::sum);
        }

        private int nextSortOrder() {
            return ++sortOrder;
        }

        private String storageFolder() {
            String prefix = "TEST".equals(source.accountType()) ? "test/" : "";
            return prefix + "certifications/" + source.uid() + "/public-media/" + certificationId;
        }
    }

    private static final class LimitedOutputStream extends java.io.FilterOutputStream {
        private final ExtractionContext context;
        private long written;

        private LimitedOutputStream(OutputStream output, ExtractionContext context) {
            super(output);
            this.context = context;
        }

        @Override
        public void write(int value) throws IOException {
            ensureAllowed(1);
            out.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ensureAllowed(length);
            out.write(bytes, offset, length);
        }

        private void ensureAllowed(int count) {
            written += count;
            if (written > MAX_SINGLE_FILE_BYTES) {
                throw new IllegalArgumentException("压缩包内存在超过2GB的单个文件");
            }
            context.addBytes(count);
        }
    }
}
