package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class EditFileTool implements AgentTool {

    @Override
    public String getName() { return "edit_file"; }

    @Override
    public String getDescription() {
        return "Performs a targeted text replacement in an existing file. " +
               "Finds 'oldString' in the file and replaces it with 'newString'. " +
               "Use this instead of write_file when you need to make small, " +
               "targeted changes to an existing file.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "path", "Relative or absolute path to the file to edit",
            "oldString", "The exact text to find and replace (must be unique in the file)",
            "newString", "The replacement text"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String pathStr = params.get("path");
        String oldString = params.get("oldString");
        String newString = params.get("newString");

        if (pathStr == null || pathStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'path' is required");
        }
        if (oldString == null || oldString.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'oldString' is required");
        }
        if (newString == null) {
            return ToolResult.fail(getName(), "Parameter 'newString' is required");
        }

        Path resolved = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(resolved)) {
            return ToolResult.fail(getName(), "File not found: " + resolved);
        }
        if (Files.isDirectory(resolved)) {
            return ToolResult.fail(getName(), "Path is a directory, not a file: " + resolved);
        }

        try {
            String content = Files.readString(resolved);
            if (!content.contains(oldString)) {
                return ToolResult.fail(getName(),
                    "oldString not found in file: " + resolved);
            }

            String newContent = content.replace(oldString, newString);

            if (content.equals(newContent)) {
                return ToolResult.fail(getName(), "Replacement produced no changes");
            }

            Files.writeString(resolved, newContent);

            long linesChanged = newContent.lines().count();
            return ToolResult.ok(getName(),
                "File edited successfully: %s".formatted(resolved),
                Map.of("path", resolved.toString(), "lines", linesChanged));

        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot edit file: " + e.getMessage());
        }
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
