package com.aicareer.taskprocessor.dto;

import com.aicareer.taskprocessor.entity.TaskPriority;
import com.aicareer.taskprocessor.entity.TaskStatus;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long taskId,
        String taskName,
        String taskDescription,
        TaskPriority priority,
        TaskStatus status,
        long executionTime,
        long timeoutMillis,
        int retryCount,
        Instant createdAt,
        Instant updatedAt,
        Instant scheduledAt,
        Long actualExecutionTime,
        String failureReason,
        List<Long> dependencyIds
) {
}
