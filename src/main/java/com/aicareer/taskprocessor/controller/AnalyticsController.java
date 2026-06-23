package com.aicareer.taskprocessor.controller;

import com.aicareer.taskprocessor.dto.AnalyticsReport;
import com.aicareer.taskprocessor.service.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final MonitoringService monitoringService;

    public AnalyticsController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/report")
    public AnalyticsReport report() {
        return monitoringService.analyticsReport();
    }
}
