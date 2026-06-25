package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrepTool implements AgentTool {

    private static final int MAX_RESULTS = 100;
    private static final int MAX_DEPTH = 20;

    private static final List<String> IGNORED_DIRS = List.of(
        ".git", "node_modules", "target", "build", ".gradle",
        "__pycache__", ".idea", ".vscode", "dist", "out", "bin"
    );

    @Override
    public String getName() { return "grep"; }

    @Override
    public String getDescription() {
        return "Searches for a regex pattern in file contents across the project. " +
               "Returns matching file paths with line numbers and content. " +
               "Use to find where classes are defined, where methods are called, " +
               "or to search for specific patterns in code.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "pattern", "The regex pattern to search for",
            "path", "(Optional) Directory to search in. Defaults to working directory.",
            "include", "(Optional) File glob filter (e.g. '*.java', '*.{ts,tsx}')"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String patternStr = params.get("pattern");
        if (patternStr == null || patternStr.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'pattern' is required");
        }

        String pathStr = params.getOrDefault("path", ".");
        Path dir = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(dir)) {
            return ToolResult.fail(getName(), "Directory not found: " + dir);
        }
        if (!Files.isDirectory(dir)) {
            return ToolResult.fail(getName(), "Not a directory: " + dir);
        }

        String includeFilter = params.get("include");

        try {
            Pattern regex = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            StringBuilder results = new StringBuilder();
            int[] count = new int[]{0};

            try (Stream<Path> stream = Files.walk(dir, MAX_DEPTH)) {
                stream
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> !isIgnored(p, dir))
                    .filter(p -> includeFilter == null || matchesGlob(p.toString(), includeFilter))
                    .forEach(p -> searchInFile(p, regex, results, count));
            }

            if (count[0] == 0) {
                return ToolResult.ok(getName(), "No matches found for pattern: " + patternStr);
            }

            String header = "Found %d match(es) for '%s':\n".formatted(count[0], patternStr);
            return ToolResult.ok(getName(), header + results.toString(),
                Map.of("matches", count[0], "pattern", patternStr));

        } catch (Exception e) {
            return ToolResult.fail(getName(), "Search failed: " + e.getMessage());
        }
    }

    private void searchInFile(Path file, Pattern regex, StringBuilder out, int[] count) {
        try (Stream<String> lines = Files.lines(file)) {
            var matches = lines.limit(5000)
                .map(line -> Map.entry(line, regex.matcher(line)))
                .filter(entry -> entry.getValue().find())
                .collect(Collectors.toList());

            if (!matches.isEmpty()) {
                out.append("\n").append(file).append(":\n");
                for (var entry : matches) {
                    if (count[0] >= MAX_RESULTS) {
                        out.append("  ... more matches truncated\n");
                        return;
                    }
                    count[0]++;
                    out.append("  L").append(entry.getValue().toString())
                       .append(": ").append(entry.getKey().trim()).append("\n");
                }
            }
        } catch (IOException e) {
            // skip unreadable files
        }
    }

    private boolean isIgnored(Path file, Path baseDir) {
        Path relative = baseDir.relativize(file);
        for (Path part : relative) {
            if (IGNORED_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    private boolean matchesGlob(String path, String glob) {
        if (glob == null) return true;
        String regex = glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return Pattern.compile(regex).matcher(path).find();
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
