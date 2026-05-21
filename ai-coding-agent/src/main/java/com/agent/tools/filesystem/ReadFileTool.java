package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ReadFileTool implements AgentTool {

    private static final int MAX_CHARS = 50_000;

    @Override
    public String getName() { return "read_file"; }

    @Override
    public String getDescription() {
        return "Reads the content of a file. Returns the full text content. " +
               "Truncates files larger than 50,000 characters.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of("path", "Relative or absolute path to the file to read");
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String pathStr = params.get("path");
        if (pathStr == null || pathStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'path' is required");
        }

        Path resolved = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(resolved)) {
            return ToolResult.fail(getName(), "File not found: " + resolved);
        }
        if (Files.isDirectory(resolved)) {
            return ToolResult.fail(getName(), "Path is a directory, use list_dir instead: " + resolved);
        }

        try {
            String content = Files.readString(resolved);
            boolean truncated = content.length() > MAX_CHARS;
            if (truncated) {
                content = content.substring(0, MAX_CHARS) + "\n\n[...TRUNCATED — file has more content]";
            }
            return ToolResult.ok(getName(), content, Map.of(
                "path", resolved.toString(),
                "lines", content.lines().count(),
                "truncated", truncated
            ));
        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot read file: " + e.getMessage());
        }
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
