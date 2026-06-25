package com.agent.ai;

import java.util.concurrent.atomic.AtomicInteger;

public class MockModelProvider implements ModelProvider {

    private final String name;
    private final boolean available;
    private final AtomicInteger callCount = new AtomicInteger(0);
    private String response = "TASK_COMPLETE";

    public MockModelProvider() {
        this("mock", true);
    }

    public MockModelProvider(String name, boolean available) {
        this.name = name;
        this.available = available;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getCallCount() {
        return callCount.get();
    }

    @Override
    public String getName() { return name; }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        callCount.incrementAndGet();
        return response;
    }
}
