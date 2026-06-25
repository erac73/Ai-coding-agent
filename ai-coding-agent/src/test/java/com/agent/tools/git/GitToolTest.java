package com.agent.tools.git;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GitToolTest {

    @TempDir Path tempDir;
    AgentContext context;
    GitTool tool;

    @BeforeEach
    void setup() throws Exception {
        context = new AgentContext(tempDir);
        tool = new GitTool();
        Git.init().setDirectory(tempDir.toFile()).call();
        Files.writeString(tempDir.resolve("test.txt"), "Hello Git!");
        Git.open(tempDir.toFile()).add().addFilepattern(".").call();
        Git.open(tempDir.toFile()).commit().setMessage("Initial commit").call();
    }

    @Test
    void shouldShowStatus() {
        ToolResult result = tool.execute(Map.of("action", "status"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Branch");
    }

    @Test
    void shouldShowLog() {
        ToolResult result = tool.execute(Map.of("action", "log"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Initial commit");
    }

    @Test
    void shouldShowDiff() {
        ToolResult result = tool.execute(Map.of("action", "diff"), context);
        assertThat(result.success()).isTrue();
    }

    @Test
    void shouldAddFiles() throws Exception {
        Files.writeString(tempDir.resolve("newfile.txt"), "new content");
        ToolResult result = tool.execute(Map.of("action", "add", "path", "newfile.txt"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("newfile.txt");
    }

    @Test
    void shouldCreateBranch() {
        ToolResult result = tool.execute(Map.of("action", "branch", "branch", "feature/test"), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("feature/test");
    }

    @Test
    void shouldCommit() {
        ToolResult result = tool.execute(Map.of(
            "action", "commit",
            "message", "test commit"
        ), context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("test commit");
    }

    @Test
    void shouldFailWithoutAction() {
        ToolResult result = tool.execute(Map.of(), context);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("action");
    }
}
