package com.aicareer.taskprocessor.worker;

import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.entity.TaskLog;
import com.aicareer.taskprocessor.entity.TaskStatus;
import com.aicareer.taskprocessor.queue.TaskExecutionQueue;
import com.aicareer.taskprocessor.repository.TaskDependencyRepository;
import com.aicareer.taskprocessor.repository.TaskLogRepository;
import com.aicareer.taskprocessor.repository.TaskRepository;
import com.aicareer.taskprocessor.service.TaskUpdateBroadcaster;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TaskWorker {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskLogRepository taskLogRepository;
    private final TaskUpdateBroadcaster broadcaster;
    private final TaskExecutionQueue taskExecutionQueue;
    private final ExecutorService callableExecutor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Long, ReentrantLock> taskLocks = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public TaskWorker(TaskRepository taskRepository,
                      TaskDependencyRepository dependencyRepository,
                      TaskLogRepository taskLogRepository,
                      TaskUpdateBroadcaster broadcaster,
                      @Lazy TaskExecutionQueue taskExecutionQueue) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.taskLogRepository = taskLogRepository;
        this.broadcaster = broadcaster;
        this.taskExecutionQueue = taskExecutionQueue;
    }

    public void process(Long taskId) {
        ReentrantLock lock = taskLocks.computeIfAbsent(taskId, ignored -> new ReentrantLock(true));
        if (!lock.tryLock()) {
            return;
        }

        Instant startedAt = Instant.now();
        try {
            TaskEntity task = taskRepository.findById(taskId).orElse(null);
            if (task == null || task.getStatus() == TaskStatus.CANCELLED || task.getStatus() == TaskStatus.PAUSED) {
                return;
            }

            if (!dependenciesCompleted(taskId)) {
                task.setStatus(TaskStatus.PENDING);
                task.setScheduledAt(Instant.now().plusSeconds(2));
                taskRepository.save(task);
                taskExecutionQueue.enqueue(task);
                publish();
                return;
            }

            markRunning(task, startedAt);
            Future<String> future = callableExecutor.submit(createCallable(task));
            try {
                future.get(task.getTimeoutMillis(), TimeUnit.MILLISECONDS);
                markCompleted(task.getTaskId(), startedAt);
            } catch (TimeoutException ex) {
                future.cancel(true);
                failOrRetry(task.getTaskId(), "Task exceeded timeout of " + task.getTimeoutMillis() + " ms");
            } catch (Exception ex) {
                failOrRetry(task.getTaskId(), ex.getMessage());
            }
        } finally {
            lock.unlock();
            publish();
        }
    }

    private Callable<String> createCallable(TaskEntity task) {
        return () -> {
            Thread.sleep(task.getExecutionTime());
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Task interrupted");
            }
            if (random.nextDouble() < 0.12) {
                throw new IllegalStateException("Simulated worker failure");
            }
            return "OK";
        };
    }

    private boolean dependenciesCompleted(Long taskId) {
        return dependencyRepository.findByTaskTaskId(taskId).stream()
                .allMatch(dependency -> dependency.getDependsOnTask().getStatus() == TaskStatus.COMPLETED);
    }

    private void markRunning(TaskEntity task, Instant startedAt) {
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(startedAt);
        task.setFailureReason(null);
        taskRepository.save(task);
        log(task, "STARTED", startedAt, null, null);
    }

    private void markCompleted(Long taskId, Instant startedAt) {
        TaskEntity task = taskRepository.findById(taskId).orElseThrow();
        Instant completedAt = Instant.now();
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(completedAt);
        task.setActualExecutionTime(Duration.between(startedAt, completedAt).toMillis());
        taskRepository.save(task);
        log(task, "COMPLETED", startedAt, completedAt, null);
    }

    private void failOrRetry(Long taskId, String reason) {
        TaskEntity task = taskRepository.findById(taskId).orElseThrow();
        task.setRetryCount(task.getRetryCount() + 1);
        if (task.getRetryCount() <= task.getMaxRetries()) {
            task.setStatus(TaskStatus.RETRYING);
            task.setFailureReason(reason);
            task.setScheduledAt(Instant.now().plusSeconds(2));
            taskRepository.save(task);
            log(task, "RETRYING", task.getStartedAt(), null, reason);
            taskExecutionQueue.enqueue(task);
            return;
        }

        task.setStatus(TaskStatus.FAILED);
        task.setCompletedAt(Instant.now());
        task.setFailureReason(reason);
        taskRepository.save(task);
        log(task, "FAILED", task.getStartedAt(), task.getCompletedAt(), reason);
    }

    private void log(TaskEntity task, String event, Instant start, Instant completion, String reason) {
        TaskLog log = new TaskLog();
        log.setTask(task);
        log.setEvent(event);
        log.setStartTime(start);
        log.setCompletionTime(completion);
        log.setFailureReason(reason);
        log.setRetryAttempt(task.getRetryCount());
        taskLogRepository.save(log);
    }

    private void publish() {
        broadcaster.broadcast(java.util.Map.of("type", "TASK_UPDATE"));
    }

    @PreDestroy
    public void shutdown() {
        callableExecutor.shutdownNow();
    }
}
