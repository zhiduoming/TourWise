package com.tourwise.config;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class LocalShellEnv {
    private LocalShellEnv() {
    }

    public static Map<String, String> loadZshrc(String... keys) {
        Path zshrc = Path.of(System.getProperty("user.home"), ".zshrc");
        if (!Files.isRegularFile(zshrc)) {
            return Map.of();
        }
        Map<String, String> wanted = new HashMap<>();
        for (String key : keys) {
            wanted.put(key, null);
        }
        try {
            for (String line : Files.readAllLines(zshrc)) {
                readLine(line, wanted);
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        wanted.entrySet().removeIf(entry -> !StringUtils.hasText(entry.getValue()));
        return wanted;
    }

    private static void readLine(String rawLine, Map<String, String> wanted) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }
        int index = line.indexOf('=');
        if (index <= 0) {
            return;
        }
        String key = line.substring(0, index).trim();
        if (!wanted.containsKey(key)) {
            return;
        }
        String value = line.substring(index + 1).trim();
        wanted.put(key, stripQuotes(value));
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
