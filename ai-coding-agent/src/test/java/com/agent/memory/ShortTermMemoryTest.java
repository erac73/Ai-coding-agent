package com.agent.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ShortTermMemoryTest {

    @Test
    void shouldAddMessages() {
        ShortTermMemory memory = new ShortTermMemory();
        memory.addUserMessage("Hello");
        memory.addAssistantMessage("Hi there");

        assertThat(memory.size()).isEqualTo(2);
    }

    @Test
    void shouldCompressWhenExceedingLimit() {
        ShortTermMemory memory = new ShortTermMemory(10);
        for (int i = 0; i < 15; i++) {
            memory.addToolResult("tool" + i, "result" + i);
        }
        assertThat(memory.size()).isLessThanOrEqualTo(13);
    }

    @Test
    void shouldGetLastAssistantMessage() {
        ShortTermMemory memory = new ShortTermMemory();
        memory.addUserMessage("Hello");
        memory.addAssistantMessage("First response");
        memory.addUserMessage("Again");
        memory.addAssistantMessage("Second response");

        assertThat(memory.getLastAssistantMessage()).isEqualTo("Second response");
    }

    @Test
    void shouldReturnNullWhenNoAssistantMessages() {
        ShortTermMemory memory = new ShortTermMemory();
        assertThat(memory.getLastAssistantMessage()).isNull();
    }

    @Test
    void shouldConvertToPromptString() {
        ShortTermMemory memory = new ShortTermMemory();
        memory.addUserMessage("Hello");
        memory.addAssistantMessage("World");

        String prompt = memory.toPromptString();
        assertThat(prompt).contains("[user]: Hello");
        assertThat(prompt).contains("[assistant]: World");
    }
}
