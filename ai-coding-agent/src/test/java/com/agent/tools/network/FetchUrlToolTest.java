package com.agent.tools.network;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FetchUrlToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    FetchUrlTool tool;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        tool = new FetchUrlTool();
    }

    @Test
    void shouldFailForMissingUrl() {
        assertThat(tool.execute(Map.of(), context).success()).isFalse();
    }

    @Test
    void shouldFailForInvalidUrl() {
        ToolResult result = tool.execute(Map.of("url", "not-a-url"), context);
        assertThat(result.success()).isFalse();
    }

    @Test
    void shouldFailForUnreachableUrl() {
        ToolResult result = tool.execute(Map.of("url", "https://nonexistent.example.test"), context);
        assertThat(result.success()).isFalse();
    }
}
