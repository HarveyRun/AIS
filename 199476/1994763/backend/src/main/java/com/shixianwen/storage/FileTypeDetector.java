package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import com.shixianwen.security.SecurityEventService;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

@Component
public class FileTypeDetector {
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;

    @Autowired(required = false)
    private SecurityEventService securityEvents;

    public DetectedFile detect(MultipartFile file) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("文件不能为空");
        byte[] header = readHeader(file, 32);
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) return image(file, "image/jpeg", ".jpg", true);
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return image(file, "image/png", ".png", true);
        }
        if (header.length >= 12 && ascii(header, 0, "RIFF")) {
            if (ascii(header, 8, "WEBP")) {
                return image(file, "image/webp", ".webp", false);
            }
            if (ascii(header, 8, "WAVE")) {
                return new DetectedFile("AUDIO", "audio/wav", ".wav");
            }
        }
        if (header.length >= 12 && ascii(header, 4, "ftyp")) {
            if (ascii(header, 8, "M4A ") || ascii(header, 8, "M4B ")) {
                return new DetectedFile("AUDIO", "audio/mp4", ".m4a");
            }
            return new DetectedFile("VIDEO", "video/mp4", ".mp4");
        }
        if (ascii(header, 0, "ID3") || isMp3Frame(header)) {
            return new DetectedFile("AUDIO", "audio/mpeg", ".mp3");
        }
        if (ascii(header, 0, "fLaC")) {
            return new DetectedFile("AUDIO", "audio/flac", ".flac");
        }
        if (ascii(header, 0, "OggS")) {
            return new DetectedFile("AUDIO", "audio/ogg", ".ogg");
        }
        if (isAacFrame(header)) {
            return new DetectedFile("AUDIO", "audio/aac", ".aac");
        }
        if (startsWith(header, 0x1A, 0x45, 0xDF, 0xA3)) {
            return new DetectedFile("VIDEO", "video/webm", ".webm");
        }
        if (startsWith(header, 0x50, 0x4B, 0x03, 0x04)
            || startsWith(header, 0x50, 0x4B, 0x05, 0x06)
            || startsWith(header, 0x50, 0x4B, 0x07, 0x08)) {
            return new DetectedFile("ARCHIVE", "application/zip", ".zip");
        }
        if (startsWith(header, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00)
            || startsWith(header, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)) {
            return new DetectedFile("ARCHIVE", "application/vnd.rar", ".rar");
        }
        if (securityEvents != null) {
            securityEvents.recordSafely(
                null, null, "MALICIOUS_UPLOAD_REJECTED", "HIGH", null, null,
                "无法识别真实文件类型，name=" + safeName(file.getOriginalFilename())
            );
        }
        return new DetectedFile("UNKNOWN", "application/octet-stream", "");
    }

    public DetectedFile detect(byte[] content) {
        byte[] header = content == null ? new byte[0] : Arrays.copyOf(content, Math.min(content.length, 16));
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) return new DetectedFile("IMAGE", "image/jpeg", ".jpg");
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new DetectedFile("IMAGE", "image/png", ".png");
        }
        if (header.length >= 12 && ascii(header, 0, "RIFF") && ascii(header, 8, "WEBP")) {
            return new DetectedFile("IMAGE", "image/webp", ".webp");
        }
        return new DetectedFile("UNKNOWN", "application/octet-stream", "");
    }

    public DetectedFile requireImage(MultipartFile file) {
        DetectedFile detected = detect(file);
        if (!"IMAGE".equals(detected.kind())) throw BusinessException.badRequest("文件内容不是受支持的图片");
        return detected;
    }

    public DetectedFile requireVideo(MultipartFile file) {
        DetectedFile detected = detect(file);
        if (!"VIDEO".equals(detected.kind())) throw BusinessException.badRequest("文件内容不是受支持的录像");
        return detected;
    }

    public DetectedFile requireAudioOrVideo(MultipartFile file) {
        DetectedFile detected = detect(file);
        if (!"AUDIO".equals(detected.kind()) && !"VIDEO".equals(detected.kind())) {
            throw BusinessException.badRequest("认证凭证必须是录音或录像文件");
        }
        return detected;
    }

    public DetectedFile requireArchive(MultipartFile file) {
        DetectedFile detected = detect(file);
        if (!"ARCHIVE".equals(detected.kind())) {
            throw BusinessException.badRequest("文件内容不是有效的 ZIP 或 RAR 压缩包");
        }
        return detected;
    }

    private DetectedFile image(MultipartFile file, String mimeType, String extension, boolean inspectDimensions) {
        if (inspectDimensions) validateImageDimensions(file);
        return new DetectedFile("IMAGE", mimeType, extension);
    }

    private void validateImageDimensions(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) throw BusinessException.badRequest("图片内容无法识别");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw BusinessException.badRequest("图片内容无法识别");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw BusinessException.badRequest("图片尺寸过大或内容无效");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw BusinessException.badRequest("图片内容无法识别");
        }
    }

    private byte[] readHeader(MultipartFile file, int size) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(size);
        } catch (IOException exception) {
            throw BusinessException.badRequest("文件内容无法读取");
        }
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        byte[] expected = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + expected.length), expected);
    }

    private static boolean isMp3Frame(byte[] bytes) {
        return bytes.length >= 2
            && (bytes[0] & 0xFF) == 0xFF
            && ((bytes[1] & 0xE0) == 0xE0)
            && ((bytes[1] & 0x06) != 0);
    }

    private static boolean isAacFrame(byte[] bytes) {
        return bytes.length >= 2
            && (bytes[0] & 0xFF) == 0xFF
            && ((bytes[1] & 0xF6) == 0xF0);
    }

    private static String safeName(String value) {
        if (value == null) return "";
        String safe = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.substring(0, Math.min(safe.length(), 120));
    }

    public record DetectedFile(String kind, String contentType, String extension) {
    }
}
