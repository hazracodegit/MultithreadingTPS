package com.aicareer.taskprocessor.scheduler;

import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.entity.TaskStatus;
import com.aicareer.taskprocessor.queue.TaskExecutionQueue;
import com.aicareer.taskprocessor.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskSchedulerService {

    private final TaskRepository taskRepository;
    private final TaskExecutionQueue taskExecutionQueue;

    public TaskSchedulerService(TaskRepository taskRepository, TaskExecutionQueue taskExecutionQueue) {
        this.taskRepository = taskRepository;
        this.taskExecutionQueue = taskExecutionQueue;
    }

    @Scheduled(fixedDelay = 5000)
    public void enqueueDuePendingTasks() {
        for (TaskEntity task : taskRepository.findByStatusAndScheduledAtLessThanEqual(TaskStatus.PENDING, Instant.now())) {
            taskExecutionQueue.enqueue(task);
        }
    }
}
