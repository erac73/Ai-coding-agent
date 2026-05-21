package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListDirTool implements AgentTool {

    private static final int MAX_DEPTH = 3;
    private static final int MAX_ENTRIES = 200;

    // Carpetas que no aportan valor al agente
    private static final java.util.Set<String> IGNORED = java.util.Set.of(
        ".git", "node_modules", "target", "build", ".gradle",
        "__pycache__", ".idea", ".vscode", "dist", "out", ".DS_Store"
    );

    @Override
    public String getName() { return "list_dir"; }

    @Override
    public String getDescription() {
        return "Lists the contents of a directory recursively up to 3 levels deep. " +
               "Ignores build artifacts and hidden directories. Use to understand project structure.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "path", "Directory path to list. Defaults to current working directory if not provided."
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String pathStr = params.getOrDefault("path", ".");
        Path dir = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(dir)) {
            return ToolResult.fail(getName(), "Directory not found: " + dir);
        }
        if (!Files.isDirectory(dir)) {
            return ToolResult.fail(getName(), "Not a directory: " + dir);
        }

        try {
            StringBuilder tree = new StringBuilder();
            tree.append(dir).append("\n");
            buildTree(dir, "", tree, 0, new int[]{0});

            return ToolResult.ok(getName(), tree.toString(),
                Map.of("path", dir.toString()));
        } catch (IOException e) {
            return ToolResult.fail(getName(), "Cannot list directory: " + e.getMessage());
        }
    }

    private void buildTree(Path dir, String prefix, StringBuilder out, int depth, int[] count)
            throws IOException {
        if (depth >= MAX_DEPTH || count[0] >= MAX_ENTRIES) return;

        try (Stream<Path> entries = Files.list(dir).sorted()) {
            var list = entries.filter(p -> !IGNORED.contains(p.getFileName().toString()))
                              .collect(Collectors.toList());

            for (int i = 0; i < list.size(); i++) {
                if (count[0] >= MAX_ENTRIES) {
                    out.append(prefix).append("└── [... more entries truncated]\n");
                    break;
                }
                Path entry = list.get(i);
                boolean last = (i == list.size() - 1);
                String connector = last ? "└── " : "├── ";
                String name = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    out.append(prefix).append(connector).append(name).append("/\n");
                    count[0]++;
                    buildTree(entry, prefix + (last ? "    " : "│   "), out, depth + 1, count);
                } else {
                    long size = Files.size(entry);
                    out.append(prefix).append(connector).append(name)
                       .append("  (").append(humanSize(size)).append(")\n");
                    count[0]++;
                }
            }
        }
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        return (bytes / (1024 * 1024)) + "MB";
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
