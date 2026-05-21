package com.agent.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Memoria de corto plazo: historial de la sesión actual en RAM.
 *
 * Gestiona el context window: si el historial supera el límite,
 * comprime los mensajes más antiguos para no perder el hilo.
 */
public class ShortTermMemory {

    public record Message(String role, String content) {
        @Override
        public String toString() {
            return "[%s]: %s".formatted(role, content);
        }
    }

    private final List<Message> messages = new ArrayList<>();
    private final int maxMessages;

    public ShortTermMemory() {
        this(50); // 50 mensajes por defecto antes de comprimir
    }

    public ShortTermMemory(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public void addUserMessage(String content) {
        add("user", content);
    }

    public void addAssistantMessage(String content) {
        add("assistant", content);
    }

    public void addSystemMessage(String content) {
        add("system", content);
    }

    public void addToolResult(String toolName, String result) {
        add("tool", "[%s result]: %s".formatted(toolName, result));
    }

    private void add(String role, String content) {
        messages.add(new Message(role, content));
        if (messages.size() > maxMessages) {
            compress();
        }
    }

    /**
     * Compresión simple: elimina los mensajes de tool más antiguos
     * para liberar espacio, manteniendo el contexto reciente.
     */
    private void compress() {
        // Mantener los primeros 2 mensajes (system prompt)
        // y eliminar los tool results más viejos
        int removeTarget = maxMessages / 4;
        int removed = 0;
        for (int i = 2; i < messages.size() && removed < removeTarget; i++) {
            if ("tool".equals(messages.get(i).role())) {
                messages.remove(i);
                i--;
                removed++;
            }
        }
    }

    /** Convierte el historial en un string para incluir en el prompt. */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (!"system".equals(m.role())) {
                sb.append(m).append("\n");
            }
        }
        return sb.toString();
    }

    public List<Message> getMessages()       { return Collections.unmodifiableList(messages); }
    public int size()                        { return messages.size(); }
    public void clear()                      { messages.clear(); }

    public String getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }
}
