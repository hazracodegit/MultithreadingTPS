package com.aicareer.taskprocessor.controller;

import com.aicareer.taskprocessor.dto.TaskRequest;
import com.aicareer.taskprocessor.dto.TaskResponse;
import com.aicareer.taskprocessor.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/create")
    public TaskResponse create(@Valid @RequestBody TaskRequest request, Authentication authentication) {
        return taskService.create(request, authentication.getName());
    }

    @GetMapping
    public List<TaskResponse> getAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public TaskResponse getOne(@PathVariable Long id) {
        return taskService.findOne(id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    @PutMapping("/{id}/pause")
    public TaskResponse pause(@PathVariable Long id) {
        return taskService.pause(id);
    }

    @PutMapping("/{id}/resume")
    public TaskResponse resume(@PathVariable Long id) {
        return taskService.resume(id);
    }

    @DeleteMapping("/{id}/cancel")
    public TaskResponse cancel(@PathVariable Long id) {
        return taskService.cancel(id);
    }
}
