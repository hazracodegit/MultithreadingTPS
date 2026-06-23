package com.aicareer.taskprocessor.dto;

import com.aicareer.taskprocessor.entity.TaskPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record TaskRequest(
        @NotBlank String taskName,
        @NotBlank String taskDescription,
        @NotNull TaskPriority priority,
        @Min(500) long executionTime,
        @Min(1000) long timeoutMillis,
        Instant scheduledAt,
        List<Long> dependencyIds
) {
}
