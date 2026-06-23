package com.aicareer.taskprocessor.dto;

public record DashboardStats(
        int activeThreadCount,
        int queueSize,
        long totalTasks,
        long completedTasks,
        long failedTasks,
        long pendingTasks,
        double successRate,
        double averageExecutionTime
) {
}
