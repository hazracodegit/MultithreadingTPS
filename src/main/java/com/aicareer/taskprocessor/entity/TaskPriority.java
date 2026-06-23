package com.aicareer.taskprocessor.entity;

public enum TaskPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int weight;

    TaskPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
