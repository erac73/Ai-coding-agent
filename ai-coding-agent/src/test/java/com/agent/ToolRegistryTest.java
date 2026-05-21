package com.agent;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;
import com.agent.tools.ToolRegistry;
import com.agent.tools.filesystem.ReadFileTool;
import com.agent.tools.filesystem.WriteFileTool;
import com.agent.tools.filesystem.ListDirTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ToolRegistryTest {

    @TempDir Path tempDir;
    AgentContext context;
    ToolRegistry registry;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        registry = new ToolRegistry()
            .register(new ReadFileTool())
            .register(new WriteFileTool())
            .register(new ListDirTool());
    }

    @Test
    void shouldRegisterAndRetrieveTools() {
        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.hasTool("read_file")).isTrue();
        assertThat(registry.hasTool("write_file")).isTrue();
        assertThat(registry.hasTool("list_dir")).isTrue();
    }

    @Test
    void shouldReturnErrorForUnknownTool() {
        ToolResult result = registry.execute("unknown_tool", Map.of(), context);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Unknown tool");
    }

    @Test
    void shouldWriteAndReadFile() throws Exception {
        // Write
        ToolResult write = registry.execute("write_file", Map.of(
            "path", "hello.txt",
            "content", "Hello, Agent!"
        ), context);
        assertThat(write.success()).isTrue();

        // Read it back
        ToolResult read = registry.execute("read_file", Map.of(
            "path", "hello.txt"
        ), context);
        assertThat(read.success()).isTrue();
        assertThat(read.output()).isEqualTo("Hello, Agent!");
    }

    @Test
    void shouldListDirectory() throws Exception {
        Files.createFile(tempDir.resolve("App.java"));
        Files.createFile(tempDir.resolve("Main.java"));

        ToolResult result = registry.execute("list_dir", Map.of(), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("App.java");
        assertThat(result.output()).contains("Main.java");
    }

    @Test
    void shouldFailReadingNonExistentFile() {
        ToolResult result = registry.execute("read_file", Map.of(
            "path", "does_not_exist.txt"
        ), context);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("not found");
    }
}
