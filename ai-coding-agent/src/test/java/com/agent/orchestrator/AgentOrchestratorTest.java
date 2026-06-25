package com.agent.orchestrator;

import com.agent.ai.MockModelProvider;
import com.agent.ai.ModelRouter;
import com.agent.ai.PromptManager;
import com.agent.model.AgentContext;
import com.agent.tools.ToolRegistry;
import com.agent.tools.filesystem.ReadFileTool;
import com.agent.tools.filesystem.WriteFileTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class AgentOrchestratorTest {

    @TempDir Path tempDir;
    AgentContext context;
    AgentOrchestrator orchestrator;
    MockModelProvider mockProvider;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        mockProvider = new MockModelProvider();

        ToolRegistry registry = new ToolRegistry()
            .register(new ReadFileTool())
            .register(new WriteFileTool());

        PromptManager promptManager = new PromptManager(registry);
        ModelRouter router = new ModelRouter().addProvider(mockProvider);
        orchestrator = new AgentOrchestrator(router, registry, promptManager);
    }

    @Test
    void shouldCompleteTask() {
        mockProvider.setResponse("I finished the task.\nTASK_COMPLETE");
        String result = orchestrator.run("do something", context);
        assertThat(result).contains("finished");
    }

    @Test
    void shouldCompleteAfterMaxSteps() {
        mockProvider.setResponse("I need another step...");
        String result = orchestrator.run("never ending task", context);
        assertThat(result).contains("maximum steps");
    }

    @Test
    void shouldHandleAbort() {
        context.abort();
        String result = orchestrator.run("any task", context);
        assertThat(result).contains("aborted");
    }

    @Test
    void shouldHandleModelError() {
        mockProvider.setResponse(null);
        AgentOrchestrator orch = new AgentOrchestrator(
            new ModelRouter().addProvider(new MockModelProvider("faulty", true) {
                @Override
                public String complete(String systemPrompt, String userPrompt) {
                    throw new RuntimeException("API error");
                }
            }),
            new ToolRegistry().register(new ReadFileTool()),
            new PromptManager(new ToolRegistry().register(new ReadFileTool()))
        );
        String result = orch.run("task", context);
        assertThat(result).contains("Error");
    }
}
