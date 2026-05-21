package com.agent.model;

import java.nio.file.Path;
import java.util.*;

/**
 * Contexto completo de una sesión del agente.
 * Se crea al inicio y se pasa a través de todo el sistema.
 */
public class AgentContext {

    private final String sessionId;
    private final Path workingDirectory;
    private final List<String> conversationHistory;
    private final Map<String, Object> metadata;
    private int stepCount;
    private boolean aborted;

    public AgentContext(Path workingDirectory) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.workingDirectory = workingDirectory;
        this.conversationHistory = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.stepCount = 0;
        this.aborted = false;
    }

    public void addToHistory(String role, String content) {
        conversationHistory.add("[%s]: %s".formatted(role, content));
    }

    public String getHistoryAsString() {
        return String.join("\n", conversationHistory);
    }

    public void incrementStep() {
        stepCount++;
    }

    public void abort() {
        this.aborted = true;
    }

    // ── Getters ──────────────────────────────────────────

    public String getSessionId()              { return sessionId; }
    public Path getWorkingDirectory()         { return workingDirectory; }
    public List<String> getConversationHistory() { return Collections.unmodifiableList(conversationHistory); }
    public Map<String, Object> getMetadata()  { return metadata; }
    public int getStepCount()                 { return stepCount; }
    public boolean isAborted()                { return aborted; }
}
