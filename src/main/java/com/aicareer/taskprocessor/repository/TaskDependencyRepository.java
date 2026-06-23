package com.aicareer.taskprocessor.repository;

import com.aicareer.taskprocessor.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findByTaskTaskId(Long taskId);
    void deleteByTaskTaskId(Long taskId);
}
