package com.shixianwen.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveWordServiceTest {
    @TempDir
    Path directory;

    @Test
    void masksChineseAndEnglishWordsWithoutChangingOtherContent() throws Exception {
        Path words = directory.resolve("sensitiveWords.txt");
        Files.writeString(words, "敏感词\nBadWord\n", StandardCharsets.UTF_8);
        SensitiveWordService service = new SensitiveWordService(words.toString());
        service.load();

        assertEquals("这段***和*******会被替换", service.mask("这段敏感词和badword会被替换"));
        assertEquals("正常内容", service.mask("正常内容"));
    }
}
