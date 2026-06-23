package com.aicareer.taskprocessor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ApiDocsController {

    @GetMapping("/api-docs")
    public Map<String, Object> docs() {
        return Map.of(
                "auth", List.of("POST /auth/register", "POST /auth/login"),
                "tasks", List.of(
                        "POST /tasks/create",
                        "GET /tasks",
                        "GET /tasks/{id}",
                        "PUT /tasks/{id}",
                        "DELETE /tasks/{id}",
                        "PUT /tasks/{id}/pause",
                        "PUT /tasks/{id}/resume",
                        "DELETE /tasks/{id}/cancel"
                ),
                "monitoring", List.of("GET /dashboard/stats", "GET /analytics/report", "GET /dashboard/logs")
        );
    }
}
