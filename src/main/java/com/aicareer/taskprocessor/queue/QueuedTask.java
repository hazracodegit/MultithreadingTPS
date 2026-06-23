package com.aicareer.taskprocessor.queue;

import com.aicareer.taskprocessor.entity.TaskPriority;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public record QueuedTask(
        Long taskId,
        TaskPriority priority,
        Instant scheduledAt,
        long sequence
) implements Comparable<QueuedTask> {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    public static QueuedTask of(Long taskId, TaskPriority priority, Instant scheduledAt) {
        return new QueuedTask(taskId, priority, scheduledAt, SEQUENCE.incrementAndGet());
    }

    @Override
    public int compareTo(QueuedTask other) {
        int scheduleCompare = scheduledAt.compareTo(other.scheduledAt);
        if (scheduleCompare != 0) {
            return scheduleCompare;
        }
        int priorityCompare = Integer.compare(priority.getWeight(), other.priority.getWeight());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return Long.compare(sequence, other.sequence);
    }
}
