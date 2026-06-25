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

class GrepToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    GrepTool tool;

    @BeforeEach
    void setup() throws Exception {
        context = new AgentContext(tempDir);
        tool = new GrepTool();

        Files.writeString(tempDir.resolve("App.java"), "public class App {\n  public static void main(String[] args) {}\n}");
        Files.writeString(tempDir.resolve("Helper.java"), "public class Helper {\n  public String greet() { return \"hello\"; }\n}");
        Files.createDirectory(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve(".git/config"), "some config");
    }

    @Test
    void shouldFindMatchingPattern() {
        ToolResult result = tool.execute(Map.of("pattern", "class"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("App.java");
        assertThat(result.output()).contains("Helper.java");
    }

    @Test
    void shouldReturnNoMatches() {
        ToolResult result = tool.execute(Map.of("pattern", "nonexistent"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("No matches found");
    }

    @Test
    void shouldRespectFileFilter() {
        ToolResult result = tool.execute(Map.of(
            "pattern", "class",
            "include", "*.java"
        ), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("App.java");
        assertThat(result.output()).contains("Helper.java");
    }

    @Test
    void shouldIgnoreGitDirectory() {
        ToolResult result = tool.execute(Map.of("pattern", "config"), context);
        assertThat(result.output()).doesNotContain(".git");
    }

    @Test
    void shouldRequirePattern() {
        assertThat(tool.execute(Map.of(), context).success()).isFalse();
    }
}
