package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DeleteFileTool implements AgentTool {

    @Override
    public String getName() { return "delete_file"; }

    @Override
    public String getDescription() {
        return "Deletes a file or empty directory. Use with caution. " +
               "Cannot delete non-empty directories.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "path", "Relative or absolute path to the file or empty directory to delete"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String pathStr = params.get("path");
        if (pathStr == null || pathStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'path' is required");
        }

        Path resolved = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(resolved)) {
            return ToolResult.fail(getName(), "Path not found: " + resolved);
        }

        try {
            if (Files.isDirectory(resolved)) {
                if (Files.list(resolved).findAny().isPresent()) {
                    return ToolResult.fail(getName(), "Directory not empty, cannot delete: " + resolved);
                }
            }
            Files.delete(resolved);
            return ToolResult.ok(getName(), "Deleted: " + resolved,
                Map.of("path", resolved.toString()));
        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot delete: " + e.getMessage());
        }
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
