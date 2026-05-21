package com.agent.ai;

import com.agent.tools.ToolRegistry;

/**
 * Construye y gestiona los prompts del sistema.
 *
 * Centralizar los prompts aquí facilita iterar sobre ellos
 * sin tocar la lógica del orquestador.
 */
public class PromptManager {

    private final ToolRegistry toolRegistry;

    public PromptManager(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * System prompt principal del agente.
     * Define el comportamiento, las tools disponibles y el formato de respuesta.
     */
    public String buildSystemPrompt() {
        return """
            You are an expert AI coding agent. You help developers analyze, write, refactor,
            and debug code. You work autonomously by using tools to read files, run commands,
            and make changes.

            ## Behavior
            - Think step by step before acting
            - Read files before modifying them
            - Prefer small, targeted changes over large rewrites
            - Always explain what you're doing and why
            - If a task is unclear, ask for clarification before acting
            - Never make destructive changes without explaining them first

            ## Response format
            When you need to use a tool, respond ONLY with a JSON block in this exact format:
            ```tool
            {
              "tool": "<tool_name>",
              "params": {
                "<param_name>": "<param_value>"
              },
              "reasoning": "<why you're using this tool>"
            }
            ```

            When you have completed the task or need to respond to the user, write
            your response normally in plain text.

            To indicate the task is complete, end your message with: TASK_COMPLETE

            %s
            """.formatted(toolRegistry.toSystemPromptBlock());
    }

    /**
     * Prompt para análisis inicial de un proyecto.
     */
    public String buildAnalysisPrompt(String projectPath) {
        return """
            Analyze the project at: %s

            Steps:
            1. List the directory structure
            2. Identify the project type (Java, Python, Node.js, etc.)
            3. Find the main entry points
            4. Summarize what the project does
            5. Identify any obvious issues or improvements

            Be concise and actionable.
            """.formatted(projectPath);
    }
}
