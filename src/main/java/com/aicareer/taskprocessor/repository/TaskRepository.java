package com.aicareer.taskprocessor.repository;

import com.aicareer.taskprocessor.entity.TaskEntity;
import com.aicareer.taskprocessor.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    long countByStatus(TaskStatus status);
    List<TaskEntity> findByStatusAndScheduledAtLessThanEqual(TaskStatus status, Instant scheduledAt);
}
