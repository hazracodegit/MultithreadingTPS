package com.aicareer.taskprocessor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_dependencies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "depends_on_task_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private TaskEntity dependsOnTask;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public TaskEntity getDependsOnTask() {
        return dependsOnTask;
    }

    public void setDependsOnTask(TaskEntity dependsOnTask) {
        this.dependsOnTask = dependsOnTask;
    }
}
