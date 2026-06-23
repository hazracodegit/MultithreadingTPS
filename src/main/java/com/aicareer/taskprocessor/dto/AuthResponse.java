package com.aicareer.taskprocessor.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}
