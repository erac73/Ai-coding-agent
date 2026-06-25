package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class MoveFileTool implements AgentTool {

    @Override
    public String getName() { return "move_file"; }

    @Override
    public String getDescription() {
        return "Moves or renames a file or directory. Can also be used to copy " +
               "if 'action' parameter is set to 'copy'. Creates parent directories if needed.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "source", "Path to the file or directory to move",
            "target", "Destination path",
            "action", "(Optional) 'move' (default) or 'copy'"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String sourceStr = params.get("source");
        String targetStr = params.get("target");
        boolean copy = "copy".equalsIgnoreCase(params.get("action"));

        if (sourceStr == null || sourceStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'source' is required");
        }
        if (targetStr == null || targetStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'target' is required");
        }

        Path source = resolve(sourceStr, context.getWorkingDirectory());
        Path target = resolve(targetStr, context.getWorkingDirectory());

        if (!Files.exists(source)) {
            return ToolResult.fail(getName(), "Source not found: " + source);
        }
        if (Files.exists(target) && !copy) {
            return ToolResult.fail(getName(), "Target already exists: " + target);
        }

        try {
            Files.createDirectories(target.getParent());
            if (copy) {
                if (Files.isDirectory(source)) {
                    copyDirectory(source, target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return ToolResult.ok(getName(), "Copied: " + source + " -> " + target,
                    Map.of("source", source.toString(), "target", target.toString(), "action", "copy"));
            } else {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return ToolResult.ok(getName(), "Moved: " + source + " -> " + target,
                    Map.of("source", source.toString(), "target", target.toString(), "action", "move"));
            }
        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot " + (copy ? "copy" : "move") + ": " + e.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            for (var entry : stream.toList()) {
                Path dest = target.resolve(source.relativize(entry));
                if (Files.isDirectory(entry)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(entry, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
