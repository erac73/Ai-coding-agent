package com.agent.config;

import com.agent.ai.ModelRouter;
import com.agent.ai.PromptManager;
import com.agent.ai.providers.AnthropicProvider;
import com.agent.orchestrator.AgentOrchestrator;
import com.agent.security.CommandValidator;
import com.agent.tools.ToolRegistry;
import com.agent.tools.filesystem.ListDirTool;
import com.agent.tools.filesystem.ReadFileTool;
import com.agent.tools.filesystem.WriteFileTool;
import com.agent.tools.terminal.RunCommandTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensambla todos los componentes del agente.
 *
 * En el MVP usamos inyección manual.
 */
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private final AgentConfig config;

    public AgentFactory(AgentConfig config) {
        this.config = config;
    }

    public AgentOrchestrator createOrchestrator() {
        // ── Security ──────────────────────────────────────────
        CommandValidator validator = new CommandValidator(config.isStrictSecurity());

        // ── Tools ─────────────────────────────────────────────
        ToolRegistry registry = new ToolRegistry()
            .register(new ReadFileTool())
            .register(new WriteFileTool())
            .register(new ListDirTool())
            .register(new RunCommandTool(validator));

        log.info("Registered {} tools: {}", registry.size(), registry.getToolNames());

        // ── AI Providers ──────────────────────────────────────
        ModelRouter router = new ModelRouter();

        config.getAnthropicApiKey().ifPresentOrElse(
            key -> {
                router.addProvider(new AnthropicProvider(key, config.getAnthropicModel()));
                log.info("Anthropic provider configured (model: {})", config.getAnthropicModel());
            },
            () -> log.warn("ANTHROPIC_API_KEY not set — Anthropic provider disabled")
        );

        if (!router.hasAvailableProvider()) {
            throw new IllegalStateException(
                "\n No AI provider configured!\n" +
                "   Set your API key:\n" +
                "   export ANTHROPIC_API_KEY=sk-ant-...\n" +
                "   Or create ~/.agent/config.json with 'agent --init'");
        }

        // ── Orchestrator ──────────────────────────────────────
        PromptManager promptManager = new PromptManager(registry);
        return new AgentOrchestrator(router, registry, promptManager);
    }
}
