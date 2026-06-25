package com.agent.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ToolResultTest {

    @Test
    void shouldCreateSuccessResult() {
        ToolResult result = ToolResult.ok("test_tool", "Operation completed");
        assertThat(result.success()).isTrue();
        assertThat(result.toolName()).isEqualTo("test_tool");
        assertThat(result.output()).isEqualTo("Operation completed");
        assertThat(result.error()).isNull();
    }

    @Test
    void shouldCreateFailResult() {
        ToolResult result = ToolResult.fail("test_tool", "Something went wrong");
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("Something went wrong");
        assertThat(result.output()).isNull();
    }

    @Test
    void shouldTruncateLongOutputInPromptString() {
        String longOutput = "a".repeat(5000);
        ToolResult result = ToolResult.ok("test_tool", longOutput);
        String prompt = result.toPromptString();
        assertThat(prompt).contains("[...TRUNCATED]");
        assertThat(prompt.length()).isLessThan(longOutput.length() + 100);
    }

    @Test
    void shouldNotTruncateShortOutput() {
        ToolResult result = ToolResult.ok("test_tool", "short output");
        String prompt = result.toPromptString();
        assertThat(prompt).doesNotContain("TRUNCATED");
        assertThat(prompt).contains("short output");
    }
}
