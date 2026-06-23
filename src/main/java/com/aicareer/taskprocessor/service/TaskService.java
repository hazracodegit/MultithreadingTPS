package com.aicareer.taskprocessor.service;

import com.aicareer.taskprocessor.dto.TaskLogResponse;
import com.aicareer.taskprocessor.dto.TaskRequest;
import com.aicareer.taskprocessor.dto.TaskResponse;
import com.aicareer.taskprocessor.entity.TaskDependency;
import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.entity.TaskLog;
import com.aicareer.taskprocessor.entity.TaskStatus;
import com.aicareer.taskprocessor.entity.UserEntity;
import com.aicareer.taskprocessor.exception.ApiException;
import com.aicareer.taskprocessor.queue.TaskExecutionQueue;
import com.aicareer.taskprocessor.repository.TaskDependencyRepository;
import com.aicareer.taskprocessor.repository.TaskLogRepository;
import com.aicareer.taskprocessor.repository.TaskRepository;
import com.aicareer.taskprocessor.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskLogRepository taskLogRepository;
    private final UserRepository userRepository;
    private final TaskExecutionQueue taskExecutionQueue;

    public TaskService(TaskRepository taskRepository,
                       TaskDependencyRepository dependencyRepository,
                       TaskLogRepository taskLogRepository,
                       UserRepository userRepository,
                       TaskExecutionQueue taskExecutionQueue) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.taskLogRepository = taskLogRepository;
        this.userRepository = userRepository;
        this.taskExecutionQueue = taskExecutionQueue;
    }

    @Transactional
    public TaskResponse create(TaskRequest request, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        TaskEntity task = new TaskEntity();
        applyRequest(task, request);
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedBy(user);
        TaskEntity saved = taskRepository.save(task);
        saveDependencies(saved, request.dependencyIds());
        log(saved, "CREATED", null);
        if (!saved.getScheduledAt().isAfter(Instant.now())) {
            taskExecutionQueue.enqueue(saved);
        }
        return toResponse(saved);
    }

    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparing(TaskEntity::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse findOne(Long id) {
        return toResponse(getTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ApiException(HttpStatus.CONFLICT, "Running tasks cannot be edited");
        }
        applyRequest(task, request);
        dependencyRepository.deleteByTaskTaskId(id);
        TaskEntity saved = taskRepository.save(task);
        saveDependencies(saved, request.dependencyIds());
        log(saved, "UPDATED", null);
        if (saved.getStatus() == TaskStatus.PENDING && !saved.getScheduledAt().isAfter(Instant.now())) {
            taskExecutionQueue.enqueue(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        TaskEntity task = getTask(id);
        dependencyRepository.deleteByTaskTaskId(id);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse pause(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ApiException(HttpStatus.CONFLICT, "Running task will finish or timeout before it can pause");
        }
        task.setStatus(TaskStatus.PAUSED);
        log(task, "PAUSED", null);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse resume(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() != TaskStatus.PAUSED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only paused tasks can be resumed");
        }
        task.setStatus(TaskStatus.PENDING);
        task.setScheduledAt(Instant.now());
        TaskEntity saved = taskRepository.save(task);
        log(saved, "RESUMED", null);
        taskExecutionQueue.enqueue(saved);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse cancel(Long id) {
        TaskEntity task = getTask(id);
        task.setStatus(TaskStatus.CANCELLED);
        task.setFailureReason("Cancelled by user");
        task.setCompletedAt(Instant.now());
        log(task, "CANCELLED", "Cancelled by user");
        return toResponse(taskRepository.save(task));
    }

    public List<TaskLogResponse> recentLogs() {
        return taskLogRepository.findTop25ByOrderByCreatedAtDesc().stream()
                .map(this::toLogResponse)
                .toList();
    }

    private void applyRequest(TaskEntity task, TaskRequest request) {
        task.setTaskName(request.taskName());
        task.setTaskDescription(request.taskDescription());
        task.setPriority(request.priority());
        task.setExecutionTime(request.executionTime());
        task.setTimeoutMillis(request.timeoutMillis());
        task.setScheduledAt(request.scheduledAt() == null ? Instant.now() : request.scheduledAt());
    }

    private void saveDependencies(TaskEntity task, List<Long> dependencyIds) {
        if (dependencyIds == null) {
            return;
        }
        for (Long dependencyId : dependencyIds) {
            if (dependencyId.equals(task.getTaskId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Task cannot depend on itself");
            }
            TaskEntity dependsOn = getTask(dependencyId);
            TaskDependency dependency = new TaskDependency();
            dependency.setTask(task);
            dependency.setDependsOnTask(dependsOn);
            dependencyRepository.save(dependency);
        }
    }

    private TaskEntity getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private TaskResponse toResponse(TaskEntity task) {
        List<Long> dependencies = dependencyRepository.findByTaskTaskId(task.getTaskId()).stream()
                .map(dependency -> dependency.getDependsOnTask().getTaskId())
                .toList();
        return new TaskResponse(
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getExecutionTime(),
                task.getTimeoutMillis(),
                task.getRetryCount(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getScheduledAt(),
                task.getActualExecutionTime(),
                task.getFailureReason(),
                dependencies
        );
    }

    private TaskLogResponse toLogResponse(TaskLog log) {
        return new TaskLogResponse(
                log.getId(),
                log.getTask().getTaskId(),
                log.getEvent(),
                log.getStartTime(),
                log.getCompletionTime(),
                log.getFailureReason(),
                log.getRetryAttempt(),
                log.getCreatedAt()
        );
    }

    private void log(TaskEntity task, String event, String reason) {
        TaskLog log = new TaskLog();
        log.setTask(task);
        log.setEvent(event);
        log.setFailureReason(reason);
        log.setRetryAttempt(task.getRetryCount());
        taskLogRepository.save(log);
    }
}
