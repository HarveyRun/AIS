package com.shixianwen.content;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SensitiveWordService {
    private final Path wordFile;
    private Pattern pattern;

    public SensitiveWordService(
        @Value("${app.content.sensitive-words-file:./sensitiveWords.txt}") String wordFile
    ) {
        this.wordFile = Path.of(wordFile).toAbsolutePath().normalize();
    }

    @PostConstruct
    void load() throws IOException {
        List<String> words = Files.readAllLines(wordFile, StandardCharsets.UTF_8).stream()
            .map(String::trim)
            .filter(word -> !word.isEmpty() && !word.startsWith("#"))
            .distinct()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        if (words.isEmpty()) {
            throw new IllegalStateException("敏感词文件不能为空：" + wordFile);
        }
        String expression = words.stream()
            .map(Pattern::quote)
            .reduce((left, right) -> left + "|" + right)
            .orElseThrow();
        pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder(value.length());
        while (matcher.find()) {
            int length = matcher.group().codePointCount(0, matcher.group().length());
            matcher.appendReplacement(result, Matcher.quoteReplacement("*".repeat(length)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
