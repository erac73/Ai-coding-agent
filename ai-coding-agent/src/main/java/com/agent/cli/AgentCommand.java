package com.agent.cli;

import com.agent.config.AgentConfig;
import com.agent.config.AgentFactory;
import com.agent.model.AgentContext;
import com.agent.orchestrator.AgentOrchestrator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Punto de entrada principal del agente.
 *
 * Uso:
 *   java -jar agent.jar "analiza este proyecto"
 *   java -jar agent.jar --dir /path/to/project "corrige todos los errores"
 *   java -jar agent.jar --init
 *   java -jar agent.jar --interactive
 */
@Command(
    name = "agent",
    description = "AI Coding Agent — powered by Claude",
    mixinStandardHelpOptions = true,
    version = "agent 0.1.0"
)
public class AgentCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Task for the agent to execute",
        arity = "0..1"
    )
    private String task;

    @Option(
        names = {"--dir", "-d"},
        description = "Working directory (default: current directory)",
        defaultValue = "."
    )
    private Path workingDirectory;

    @Option(
        names = {"--init"},
        description = "Create default configuration file at ~/.agent/config.json"
    )
    private boolean init;

    @Option(
        names = {"--interactive", "-i"},
        description = "Start interactive REPL mode"
    )
    private boolean interactive;

    @Option(
        names = {"--model", "-m"},
        description = "Override the AI model to use"
    )
    private String modelOverride;

    // ── Entry point ───────────────────────────────────────────────────────

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AgentCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Inicializar configuración
        if (init) {
            return handleInit();
        }

        AgentConfig config = new AgentConfig();
        AgentFactory factory = new AgentFactory(config);

        AgentOrchestrator orchestrator;
        try {
            orchestrator = factory.createOrchestrator();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        // Modo interactivo
        if (interactive) {
            return new InteractiveMode(orchestrator, workingDirectory).run();
        }

        // Modo single-task
        if (task == null || task.isBlank()) {
            System.err.println("   Please provide a task. Example:");
            System.err.println("   agent \"analyze this project and fix errors\"");
            System.err.println("   agent --interactive");
            return 1;
        }

        AgentContext context = new AgentContext(workingDirectory.toAbsolutePath().normalize());
        orchestrator.run(task, context);
        return 0;
    }

    private int handleInit() {
        try {
            AgentConfig.createDefaultConfig();
            System.out.println("   Default config created.");
            System.out.println("   Edit the file and add your API keys.");
            return 0;
        } catch (IOException e) {
            System.err.println("   Failed to create config: " + e.getMessage());
            return 1;
        }
    }
}
