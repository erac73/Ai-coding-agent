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

class MoveFileToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    MoveFileTool tool;

    @BeforeEach
    void setup() {
        context = new AgentContext(tempDir);
        tool = new MoveFileTool();
    }

    @Test
    void shouldMoveFile() throws Exception {
        Files.writeString(tempDir.resolve("source.txt"), "hello");
        ToolResult result = tool.execute(Map.of(
            "source", "source.txt",
            "target", "dest.txt"
        ), context);
        assertThat(result.success()).isTrue();
        assertThat(Files.exists(tempDir.resolve("dest.txt"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("source.txt"))).isFalse();
    }

    @Test
    void shouldCopyFile() throws Exception {
        Files.writeString(tempDir.resolve("original.txt"), "data");
        ToolResult result = tool.execute(Map.of(
            "source", "original.txt",
            "target", "copy.txt",
            "action", "copy"
        ), context);
        assertThat(result.success()).isTrue();
        assertThat(Files.exists(tempDir.resolve("original.txt"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("copy.txt"))).isTrue();
    }

    @Test
    void shouldFailWithoutSource() {
        assertThat(tool.execute(Map.of("target", "x"), context).success()).isFalse();
    }

    @Test
    void shouldFailWithoutTarget() {
        assertThat(tool.execute(Map.of("source", "x"), context).success()).isFalse();
    }
}
