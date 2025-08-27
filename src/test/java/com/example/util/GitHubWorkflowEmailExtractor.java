package com.example.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubWorkflowEmailExtractor {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    public Set<String> extractEmails(Path workflowYamlPath) throws IOException {
        if (!Files.exists(workflowYamlPath)) {
            throw new IOException("Workflow file not found: " + workflowYamlPath);
        }
        Set<String> emails = new HashSet<>();
        try (InputStream in = Files.newInputStream(workflowYamlPath)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(in);
            scanObject(data, emails);
        }
        return emails;
    }

    @SuppressWarnings("unchecked")
    private void scanObject(Object node, Set<String> emails) {
        if (node == null) return;
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                scanObject(value, emails);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                scanObject(item, emails);
            }
        } else if (node instanceof String str) {
            Matcher matcher = EMAIL_PATTERN.matcher(str);
            while (matcher.find()) {
                emails.add(matcher.group());
            }
        }
    }
}


