package com.aicareer.taskprocessor.queue;

import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.worker.TaskWorker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskExecutionQueue {

    private final PriorityBlockingQueue<QueuedTask> queue = new PriorityBlockingQueue<>();
    private final java.util.Set<Long> queuedTaskIds = ConcurrentHashMap.newKeySet();
    private final ExecutorService workerPool;
    private final TaskWorker taskWorker;
    private final int workerCount;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public TaskExecutionQueue(TaskWorker taskWorker,
                              @Value("${app.task.worker-count}") int workerCount) {
        this.taskWorker = taskWorker;
        this.workerCount = workerCount;
        this.workerPool = Executors.newFixedThreadPool(workerCount);
    }

    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < workerCount; i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    public void enqueue(TaskEntity task) {
        if (queuedTaskIds.add(task.getTaskId())) {
            queue.offer(QueuedTask.of(task.getTaskId(), task.getPriority(), task.getScheduledAt()));
        }
    }

    public int size() {
        return queue.size();
    }

    public int activeThreadCount() {
        if (workerPool instanceof ThreadPoolExecutor executor) {
            return executor.getActiveCount();
        }
        return workerCount;
    }

    private void workerLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                QueuedTask queuedTask = queue.take();
                long wait = queuedTask.scheduledAt().toEpochMilli() - Instant.now().toEpochMilli();
                if (wait > 0) {
                    queue.offer(queuedTask);
                    TimeUnit.MILLISECONDS.sleep(Math.min(wait, 1000));
                    continue;
                }
                queuedTaskIds.remove(queuedTask.taskId());
                taskWorker.process(queuedTask.taskId());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @PreDestroy
    public void stopWorkers() {
        running.set(false);
        workerPool.shutdownNow();
    }
}
