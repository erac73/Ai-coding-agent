package com.agent.cli;

import com.agent.model.AgentContext;
import com.agent.orchestrator.AgentOrchestrator;
import org.jline.reader.*;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Path;

public class InteractiveMode {

    private final AgentOrchestrator orchestrator;
    private final Path workingDirectory;

    public InteractiveMode(AgentOrchestrator orchestrator, Path workingDirectory) {
        this.orchestrator = orchestrator;
        this.workingDirectory = workingDirectory;
    }

    public int run() {
        AgentContext context = new AgentContext(workingDirectory.toAbsolutePath().normalize());

        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(
                    "help", "exit", "quit", "clear", "status", "tasks",
                    "read_file", "write_file", "edit_file", "list_dir",
                    "grep", "git", "run_command", "analyze_code",
                    "delete_file", "move_file", "fetch_url"))
                .variable(LineReader.HISTORY_FILE,
                    Path.of(System.getProperty("user.home"), ".agent", "history").toString())
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                .build();

            System.out.println(banner());
            System.out.println("Working directory: " + context.getWorkingDirectory());
            System.out.println("Type 'exit' or Ctrl+C to quit. Type 'help' for commands.\n");

            while (true) {
                String input;
                try {
                    input = reader.readLine("> ").trim();
                } catch (EndOfFileException | UserInterruptException e) {
                    System.out.println("\n Bye!");
                    return 0;
                }

                if (input.isBlank()) continue;

                switch (input.toLowerCase()) {
                    case "exit", "quit", "q" -> {
                        System.out.println(" Bye!");
                        return 0;
                    }
                    case "help" -> printHelp();
                    case "clear" -> {
                        terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
                        terminal.flush();
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
        } catch (IOException e) {
            System.err.println("Terminal error: " + e.getMessage());
            return 1;
        }
    }

    private void printHelp() {
        System.out.println("""
            Commands:
              exit / quit / q  — Exit the agent
              clear            — Clear the screen
              help             — Show this help

            Example tasks:
              > list all Java files in this project
              > read src/Main.java and explain it
              > run the tests
              > create a class User with name and email
              > git status
              > fetch_url https://docs.oracle.com/en/java/
            """);
    }

    private String banner() {
        return """
            ╔═══════════════════════════════════╗
            ║                                   ║
            ║     AI Coding Agent  v0.2.0       ║
            ╚═══════════════════════════════════╝""";
    }
}
