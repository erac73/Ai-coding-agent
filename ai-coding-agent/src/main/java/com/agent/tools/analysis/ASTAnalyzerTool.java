package com.agent.tools.analysis;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ASTAnalyzerTool implements AgentTool {

    @Override
    public String getName() { return "analyze_code"; }

    @Override
    public String getDescription() {
        return "Analyzes Java source code structure using AST parsing. " +
               "Supports actions: analyze (class details), summarize (project overview), " +
               "find (search usages), metrics (cyclomatic complexity).";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "action", "Operation: analyze | summarize | find | metrics",
            "path", "File or directory path to analyze",
            "query", "(Optional) Search query for 'find' action"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String action = params.get("action");
        if (action == null || action.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'action' is required: analyze, summarize, find, metrics");
        }

        String pathStr = params.getOrDefault("path", ".");
        Path resolved = resolve(pathStr, context.getWorkingDirectory());

        if (!Files.exists(resolved)) {
            return ToolResult.fail(getName(), "Path not found: " + resolved);
        }

        try {
            return switch (action.toLowerCase()) {
                case "analyze" -> analyzeFile(resolved);
                case "summarize" -> summarize(resolved);
                case "find" -> findUsages(resolved, params.get("query"));
                case "metrics" -> calculateMetrics(resolved);
                default -> ToolResult.fail(getName(), "Unknown action: " + action);
            };
        } catch (Exception e) {
            return ToolResult.fail(getName(), "Analysis failed: " + e.getMessage());
        }
    }

    private ToolResult analyzeFile(Path file) throws IOException {
        if (Files.isDirectory(file)) {
            return ToolResult.fail(getName(), "Path is a directory, use 'summarize' instead");
        }
        if (!file.toString().endsWith(".java")) {
            return ToolResult.fail(getName(), "Not a Java file: " + file);
        }

        CompilationUnit cu = StaticJavaParser.parse(file);
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(file.getFileName()).append("\n");
        sb.append("Package: ").append(cu.getPackageDeclaration().map(p -> p.getName().toString()).orElse("(default)")).append("\n");

        for (var type : cu.getTypes()) {
            sb.append("\nClass: ").append(type.getNameAsString());
            type.getExtendedTypes().forEach(e -> sb.append(" extends ").append(e));
            type.getImplementedTypes().forEach(i -> sb.append(" implements ").append(i));
            sb.append("\n");

            type.getFields().forEach(f ->
                sb.append("  Field: ").append(f.getVariables().stream()
                    .map(v -> v.getName() + ": " + f.getElementType())
                    .collect(Collectors.joining(", "))).append("\n"));

            type.getMethods().forEach(m ->
                sb.append("  Method: ").append(m.getType()).append(" ").append(m.getName())
                  .append("(").append(m.getParameters().stream()
                    .map(p -> p.getType() + " " + p.getName())
                    .collect(Collectors.joining(", "))).append(")\n"));
        }

        return ToolResult.ok(getName(), sb.toString(), Map.of("file", file.toString(), "action", "analyze"));
    }

    private ToolResult summarize(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return analyzeFile(dir);
        }

        List<Path> javaFiles;
        try (var stream = Files.walk(dir, 10)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                .collect(Collectors.toList());
        }

        if (javaFiles.isEmpty()) {
            return ToolResult.ok(getName(), "No Java files found in " + dir);
        }

        int totalClasses = 0, totalMethods = 0, totalFields = 0;
        Map<String, Integer> methodsPerFile = new LinkedHashMap<>();

        for (Path f : javaFiles) {
            CompilationUnit cu = StaticJavaParser.parse(f);
            int classes = cu.getTypes().size();
            int methods = cu.getTypes().stream().mapToInt(t -> t.getMethods().size()).sum();
            int fields = cu.getTypes().stream().mapToInt(t -> t.getFields().size()).sum();
            totalClasses += classes;
            totalMethods += methods;
            totalFields += fields;
            methodsPerFile.put(f.getFileName().toString(), methods);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Project summary for: ").append(dir).append("\n");
        sb.append("Java files: ").append(javaFiles.size()).append("\n");
        sb.append("Classes: ").append(totalClasses).append("\n");
        sb.append("Methods: ").append(totalMethods).append("\n");
        sb.append("Fields: ").append(totalFields).append("\n\n");
        sb.append("Methods per file:\n");
        methodsPerFile.forEach((f, m) -> sb.append("  ").append(f).append(": ").append(m).append(" methods\n"));

        return ToolResult.ok(getName(), sb.toString(),
            Map.of("files", javaFiles.size(), "classes", totalClasses, "methods", totalMethods));
    }

    private ToolResult findUsages(Path dir, String query) throws IOException {
        if (query == null || query.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'query' is required for 'find' action");
        }

        List<Path> javaFiles;
        try (var stream = Files.walk(dir, 10)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                .collect(Collectors.toList());
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Path f : javaFiles) {
            CompilationUnit cu = StaticJavaParser.parse(f);
            var usages = cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> m.getNameAsString().equals(query))
                .collect(Collectors.toList());

            var types = cu.findAll(ClassOrInterfaceType.class).stream()
                .filter(t -> t.getNameAsString().equals(query))
                .collect(Collectors.toList());

            if (!usages.isEmpty() || !types.isEmpty()) {
                sb.append("\n").append(f.getFileName()).append(":\n");
                usages.forEach(u -> { sb.append("  call: ").append(u).append("\n"); count++; });
                types.forEach(t -> { sb.append("  type: ").append(t).append("\n"); count++; });
            }
        }

        return count == 0
            ? ToolResult.ok(getName(), "No usages found for: " + query)
            : ToolResult.ok(getName(), "Found " + count + " usages of '" + query + "':\n" + sb,
                Map.of("results", count, "query", query));
    }

    private ToolResult calculateMetrics(Path dir) throws IOException {
        List<Path> javaFiles;
        if (Files.isDirectory(dir)) {
            try (var stream = Files.walk(dir, 10)) {
                javaFiles = stream.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                    .collect(Collectors.toList());
            }
        } else {
            javaFiles = List.of(dir);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Complexity metrics:\n");

        for (Path f : javaFiles) {
            CompilationUnit cu = StaticJavaParser.parse(f);
            for (var type : cu.getTypes()) {
                for (var method : type.getMethods()) {
                    int complexity = calculateCyclomatic(method);
                    sb.append("  ").append(f.getFileName()).append(" :: ")
                      .append(type.getName()).append(".").append(method.getName())
                      .append(": CC=").append(complexity).append("\n");
                }
            }
        }

        return ToolResult.ok(getName(), sb.toString(), Map.of("action", "metrics"));
    }

    private int calculateCyclomatic(MethodDeclaration method) {
        int complexity = 1;
        var body = method.getBody();
        if (body.isEmpty()) return complexity;

        String bodyStr = body.get().toString();
        complexity += countOccurrences(bodyStr, "if") + countOccurrences(bodyStr, "else if");
        complexity += countOccurrences(bodyStr, "for") + countOccurrences(bodyStr, "while");
        complexity += countOccurrences(bodyStr, "case") + countOccurrences(bodyStr, "&&");
        complexity += countOccurrences(bodyStr, "||") + countOccurrences(bodyStr, "catch");
        return complexity;
    }

    private int countOccurrences(String text, String word) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }

    private Path resolve(String pathStr, Path workingDir) {
        Path p = Path.of(pathStr);
        return p.isAbsolute() ? p : workingDir.resolve(p).normalize();
    }
}
