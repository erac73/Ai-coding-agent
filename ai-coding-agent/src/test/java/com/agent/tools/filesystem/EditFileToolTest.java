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

class EditFileToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    EditFileTool tool;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        tool = new EditFileTool();
    }

    @Test
    void shouldEditExistingFile() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello World!\nThis is a test.");

        ToolResult result = tool.execute(Map.of(
            "path", "test.txt",
            "oldString", "World",
            "newString", "Java"
        ), context);

        assertThat(result.success()).isTrue();
        assertThat(Files.readString(file)).contains("Hello Java!");
    }

    @Test
    void shouldFailWhenOldStringNotFound() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello World!");

        ToolResult result = tool.execute(Map.of(
            "path", "test.txt",
            "oldString", "Nonexistent",
            "newString", "Replacement"
        ), context);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("not found");
    }

    @Test
    void shouldFailWhenFileNotFound() {
        ToolResult result = tool.execute(Map.of(
            "path", "nonexistent.txt",
            "oldString", "foo",
            "newString", "bar"
        ), context);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("not found");
    }

    @Test
    void shouldRequireAllParameters() {
        assertThat(tool.execute(Map.of(), context).success()).isFalse();
        assertThat(tool.execute(Map.of("path", "x"), context).success()).isFalse();
        assertThat(tool.execute(Map.of("path", "x", "oldString", "y"), context).success()).isFalse();
    }
}
