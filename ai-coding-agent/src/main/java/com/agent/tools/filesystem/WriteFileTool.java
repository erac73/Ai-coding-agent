package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class WriteFileTool implements AgentTool {

    @Override
    public String getName() { return "write_file"; }

    @Override
    public String getDescription() {
        return "Creates or overwrites a file with the given content. " +
               "Creates parent directories automatically if they don't exist.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "path", "Relative or absolute path to the file to write",
            "content", "The full content to write into the file"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String pathStr = params.get("path");
        String content = params.get("content");

        if (pathStr == null || pathStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'path' is required");
        }
        if (content == null) {
            return ToolResult.fail(getName(), "Parameter 'content' is required");
        }

        Path resolved = resolve(pathStr, context.getWorkingDirectory());

        try {
            Files.createDirectories(resolved.getParent());
            Files.writeString(resolved, content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            long lines = content.lines().count();
            return ToolResult.ok(getName(),
                "File written successfully: %s (%d lines)".formatted(resolved, lines),
                Map.of("path", resolved.toString(), "lines", lines));
        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot write file: " + e.getMessage());
        }
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
