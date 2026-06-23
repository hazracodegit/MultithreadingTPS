package com.aicareer.taskprocessor.service;

import com.aicareer.taskprocessor.dto.AnalyticsReport;
import com.aicareer.taskprocessor.dto.DashboardStats;
import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.entity.TaskStatus;
import com.aicareer.taskprocessor.queue.TaskExecutionQueue;
import com.aicareer.taskprocessor.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class MonitoringService {

    private final TaskRepository taskRepository;
    private final TaskExecutionQueue taskExecutionQueue;
    private final Instant startedAt = Instant.now();

    public MonitoringService(TaskRepository taskRepository, TaskExecutionQueue taskExecutionQueue) {
        this.taskRepository = taskRepository;
        this.taskExecutionQueue = taskExecutionQueue;
    }

    public DashboardStats dashboardStats() {
        List<TaskEntity> tasks = taskRepository.findAll();
        long total = tasks.size();
        long completed = count(tasks, TaskStatus.COMPLETED);
        long failed = count(tasks, TaskStatus.FAILED);
        long pending = count(tasks, TaskStatus.PENDING) + count(tasks, TaskStatus.RETRYING);
        double average = tasks.stream()
                .filter(task -> task.getActualExecutionTime() != null)
                .mapToLong(TaskEntity::getActualExecutionTime)
                .average()
                .orElse(0);
        double successRate = total == 0 ? 0 : (completed * 100.0) / total;
        return new DashboardStats(
                taskExecutionQueue.activeThreadCount(),
                taskExecutionQueue.size(),
                total,
                completed,
                failed,
                pending,
                successRate,
                average
        );
    }

    public AnalyticsReport analyticsReport() {
        List<TaskEntity> tasks = taskRepository.findAll();
        long total = tasks.size();
        long completed = count(tasks, TaskStatus.COMPLETED);
        long failed = count(tasks, TaskStatus.FAILED);
        long retried = tasks.stream().filter(task -> task.getRetryCount() > 0).count();
        double minutes = Math.max(1.0, Duration.between(startedAt, Instant.now()).toMillis() / 60000.0);
        double average = tasks.stream()
                .filter(task -> task.getActualExecutionTime() != null)
                .mapToLong(TaskEntity::getActualExecutionTime)
                .average()
                .orElse(0);
        return new AnalyticsReport(
                completed / minutes,
                average,
                total == 0 ? 0 : (failed * 100.0) / total,
                total == 0 ? 0 : (retried * 100.0) / total
        );
    }

    private long count(List<TaskEntity> tasks, TaskStatus status) {
        return tasks.stream().filter(task -> task.getStatus() == status).count();
    }
}
