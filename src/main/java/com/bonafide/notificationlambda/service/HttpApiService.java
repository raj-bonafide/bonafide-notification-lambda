package com.bonafide.notificationlambda.service;

import com.bonafide.notificationlambda.model.ConnectionRecord;
import com.bonafide.notificationlambda.model.NotificationRequest;
import com.bonafide.notificationlambda.model.NotificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.*;

@Slf4j
public class HttpApiService {
    private final NotificationService notificationService;
    private final ConnectionService connectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpApiService(NotificationService notificationService, ConnectionService connectionService) {
        this.notificationService = notificationService;
        this.connectionService = connectionService;
    }

    public Map<String, Object> handleRequest(String method, String path, Map<String, Object> event) {
        try {
            if ("POST".equals(method) && "/api/notifications/send".equals(path)) {
                return handleSendNotification(event);
            } else if ("GET".equals(method) && "/api/notifications/metrics".equals(path)) {
                return handleGetMetrics(event);
            } else if ("GET".equals(method) && "/api/notifications/health".equals(path)) {
                return handleHealthCheck(event);
            } else if ("GET".equals(method) && "/api/notifications/connections".equals(path)) {
                return handleGetConnections(event);
            } else {
                return createJsonResponse(404, Map.of("error", "Not found: " + method + " " + path));
            }
        } catch (Exception e) {
            log.error("Error handling HTTP API request", e);
            return createJsonResponse(500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private Map<String, Object> handleSendNotification(Map<String, Object> event) throws Exception {
        String body = (String) event.get("body");
        NotificationRequest request = objectMapper.readValue(body, NotificationRequest.class);
        NotificationResult result = notificationService.sendNotification(request);
        return createJsonResponse(200, Map.of(
            "status", result.getStatus(),
            "sent", result.getSent(),
            "failed", result.getFailed(),
            "totalRecipients", result.getTotalRecipients(),
            "message", result.getMessage()
        ));
    }

    private Map<String, Object> createJsonResponse(int statusCode, Object body) {
        try {
            return Map.of(
                "statusCode", statusCode,
                "headers", Map.of("Content-Type", "application/json"),
                "body", objectMapper.writeValueAsString(body)
            );
        } catch (Exception e) {
            return Map.of(
                "statusCode", 500,
                "body", "{\"error\": \"Error creating response\"}"
            );
        }
    }

    private Map<String, Object> handleGetMetrics(Map<String, Object> event) {
        List<ConnectionRecord> connections = connectionService.getAllConnections();
        Map<String, Object> metrics = Map.of(
            "activeConnections", connections.size(),
            "connectionsByRole", countByAttribute(connections, "roles"),
            "connectionsByTeam", countByAttribute(connections, "teams"),
            "connectionsByDepartment", countByAttribute(connections, "department"),
            "timestamp", Instant.now().getEpochSecond()
        );
        return createJsonResponse(200, metrics);
    }

    private Map<String, Object> handleHealthCheck(Map<String, Object> event) {
        return createJsonResponse(200, Map.of(
            "status", "UP",
            "service", "notification-service",
            "version", "1.0.0",
            "timestamp", Instant.now().toString()
        ));
    }

    private Map<String, Object> handleGetConnections(Map<String, Object> event) {
        List<ConnectionRecord> connections = connectionService.getAllConnections();
        return createJsonResponse(200, Map.of(
            "count", connections.size(),
            "connections", connections
        ));
    }

    private Map<String, Integer> countByAttribute(List<ConnectionRecord> connections, String attribute) {
        Map<String, Integer> counts = new HashMap<>();
        for (ConnectionRecord conn : connections) {
            List<String> values = new ArrayList<>();
            switch (attribute) {
                case "roles":
                    values = conn.getRoles();
                    break;
                case "teams":
                    values = conn.getTeams();
                    break;
                case "department":
                    values.add(conn.getDepartment());
                    break;
            }
            for (String value : values) {
                counts.put(value, counts.getOrDefault(value, 0) + 1);
            }
        }
        return counts;
    }
} 