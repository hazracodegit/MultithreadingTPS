package com.aicareer.taskprocessor.controller;

import com.aicareer.taskprocessor.dto.DashboardStats;
import com.aicareer.taskprocessor.dto.TaskLogResponse;
import com.aicareer.taskprocessor.service.MonitoringService;
import com.aicareer.taskprocessor.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardStatsController {

    private final MonitoringService monitoringService;
    private final TaskService taskService;

    public DashboardStatsController(MonitoringService monitoringService, TaskService taskService) {
        this.monitoringService = monitoringService;
        this.taskService = taskService;
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return monitoringService.dashboardStats();
    }

    @GetMapping("/logs")
    public List<TaskLogResponse> logs() {
        return taskService.recentLogs();
    }
}
