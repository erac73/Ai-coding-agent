package com.agent.tools.terminal;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.security.CommandValidator;
import com.agent.tools.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RunCommandTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(RunCommandTool.class);
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_CHARS = 10_000;

    private final CommandValidator validator;

    public RunCommandTool(CommandValidator validator) {
        this.validator = validator;
    }

    @Override
    public String getName() { return "run_command"; }

    @Override
    public String getDescription() {
        return "Executes a shell command in the working directory. " +
               "Use for running tests, builds, git commands, file operations. " +
               "Returns stdout + stderr. Times out after 30 seconds.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "command", "The shell command to execute (e.g. 'mvn test', 'git status', 'ls -la')",
            "timeout", "(Optional) Timeout in seconds. Default is 30."
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String command = params.get("command");
        if (command == null || command.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'command' is required");
        }

        // Validar seguridad antes de ejecutar
        var validation = validator.validate(command);
        if (!validation.isAllowed()) {
            log.warn("Blocked dangerous command: {} | reason: {}", command, validation.reason());
            return ToolResult.fail(getName(),
                "Command blocked for security reasons: " + validation.reason() +
                "\nIf this is intentional, please confirm.");
        }

        int timeout = parseInt(params.getOrDefault("timeout", "30"), TIMEOUT_SECONDS);

        try {
            String[] cmd = OSAbstraction.wrapCommand(command);
            ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(context.getWorkingDirectory().toFile())
                .redirectErrorStream(true);

            // Propagar variables de entorno relevantes
            pb.environment().put("TERM", "dumb");

            log.info("Running command: {}", command);
            Process process = pb.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            if (!finished) {
                process.destroyForcibly();
                return ToolResult.fail(getName(),
                    "Command timed out after %d seconds.\nPartial output:\n%s"
                        .formatted(timeout, truncate(output)));
            }

            int exitCode = process.exitValue();
            String truncatedOutput = truncate(output);

            if (exitCode == 0) {
                return ToolResult.ok(getName(), truncatedOutput,
                    Map.of("exit_code", exitCode, "command", command));
            } else {
                // Exit code != 0 no es siempre un error fatal (ej: git diff cuando no hay cambios)
                return ToolResult.ok(getName(),
                    "Exit code: %d\n%s".formatted(exitCode, truncatedOutput),
                    Map.of("exit_code", exitCode, "command", command, "success", false));
            }

        } catch (Exception e) {
            log.error("Failed to run command '{}': {}", command, e.getMessage(), e);
            return ToolResult.fail(getName(), "Failed to execute: " + e.getMessage());
        }
    }

    private String truncate(String s) {
        if (s.length() <= MAX_OUTPUT_CHARS) return s;
        return s.substring(0, MAX_OUTPUT_CHARS) + "\n[...OUTPUT TRUNCATED]";
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
