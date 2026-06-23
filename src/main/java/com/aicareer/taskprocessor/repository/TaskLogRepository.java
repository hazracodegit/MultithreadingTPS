package com.aicareer.taskprocessor.repository;

import com.aicareer.taskprocessor.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    List<TaskLog> findTop25ByOrderByCreatedAtDesc();
}
