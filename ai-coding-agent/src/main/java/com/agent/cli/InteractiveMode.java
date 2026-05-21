package com.agent.cli;

import com.agent.model.AgentContext;
import com.agent.orchestrator.AgentOrchestrator;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Modo interactivo (REPL) del agente.
 *
 * Permite conversaciones continuas sin reiniciar el proceso.
 * El contexto se mantiene entre mensajes.
 *
 * historial de comandos, autocompletado y colores ANSI.
 */
public class InteractiveMode {

    private final AgentOrchestrator orchestrator;
    private final Path workingDirectory;

    public InteractiveMode(AgentOrchestrator orchestrator, Path workingDirectory) {
        this.orchestrator = orchestrator;
        this.workingDirectory = workingDirectory;
    }

    public int run() {
        AgentContext context = new AgentContext(workingDirectory.toAbsolutePath().normalize());

        System.out.println(banner());
        System.out.println("Working directory: " + context.getWorkingDirectory());
        System.out.println("Type 'exit' or Ctrl+C to quit. Type 'help' for commands.\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                System.out.flush();

                if (!scanner.hasNextLine()) break;
                String input = scanner.nextLine().trim();

                if (input.isBlank()) continue;

                switch (input.toLowerCase()) {
                    case "exit", "quit", "q" -> {
                        System.out.println(" Bye!");
                        return 0;
                    }
                    case "help" -> printHelp();
                    case "clear" -> {
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                    }
                    default -> {
                        try {
                            orchestrator.run(input, context);
                        } catch (Exception e) {
                            System.err.println("\n Error: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return 0;
    }

    private void printHelp() {
        System.out.println("""
            Commands:
              exit / quit / q  — Exit the agent
              clear            — Clear the screen
              help             — Show this help

            Example tasks:
              > list all Java files in this project
              > read the file src/Main.java and explain it
              > run the tests and show me which ones fail
              > create a new class User with name and email fields
            """);
    }

    private String banner() {
        return """
            ╔═══════════════════════════════════╗
            ║                                   ║
            ║     AI Coding Agent  v0.1.0       ║
            ╚═══════════════════════════════════╝""";
    }
}
