package com.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────
// Resultado de ejecutar una tool
// ─────────────────────────────────────────────
public record ToolResult(
    String toolName,
    boolean success,
    String output,
    String error,
    Map<String, Object> metadata
) {
    public static ToolResult ok(String toolName, String output) {
        return new ToolResult(toolName, true, output, null, Map.of());
    }

    public static ToolResult ok(String toolName, String output, Map<String, Object> metadata) {
        return new ToolResult(toolName, true, output, null, metadata);
    }

    public static ToolResult fail(String toolName, String error) {
        return new ToolResult(toolName, false, null, error, Map.of());
    }

    public String toPromptString() {
        if (success) {
            return "Tool '%s' executed successfully:\n%s".formatted(toolName, output);
        } else {
            return "Tool '%s' FAILED:\n%s".formatted(toolName, error);
        }
    }
}
