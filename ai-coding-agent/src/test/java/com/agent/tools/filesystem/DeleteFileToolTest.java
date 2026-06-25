package com.agent.tools.filesystem;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DeleteFileToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    DeleteFileTool tool;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        tool = new DeleteFileTool();
    }

    @Test
    void shouldDeleteFile() throws Exception {
        Path file = tempDir.resolve("delete_me.txt");
        Files.writeString(file, "content");
        ToolResult result = tool.execute(Map.of("path", "delete_me.txt"), context);
        assertThat(result.success()).isTrue();
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void shouldFailForNonExistent() {
        ToolResult result = tool.execute(Map.of("path", "nonexistent.txt"), context);
        assertThat(result.success()).isFalse();
    }

    @Test
    void shouldFailForNonEmptyDirectory() throws Exception {
        Path dir = tempDir.resolve("mydir");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file.txt"), "x");
        ToolResult result = tool.execute(Map.of("path", "mydir"), context);
        assertThat(result.success()).isFalse();
    }

    @Test
    void shouldDeleteEmptyDirectory() throws Exception {
        Path dir = tempDir.resolve("emptydir");
        Files.createDirectory(dir);
        ToolResult result = tool.execute(Map.of("path", "emptydir"), context);
        assertThat(result.success()).isTrue();
    }
}
