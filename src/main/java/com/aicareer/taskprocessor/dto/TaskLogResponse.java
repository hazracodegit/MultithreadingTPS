package com.aicareer.taskprocessor.dto;

import java.time.Instant;

public record TaskLogResponse(
        Long id,
        Long taskId,
        String event,
        Instant startTime,
        Instant completionTime,
        String failureReason,
        int retryAttempt,
        Instant createdAt
) {
}
