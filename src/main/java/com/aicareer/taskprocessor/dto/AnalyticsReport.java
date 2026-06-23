package com.aicareer.taskprocessor.dto;

public record AnalyticsReport(
        double taskThroughputPerMinute,
        double averageProcessingTime,
        double failureRate,
        double retryRate
) {
}
