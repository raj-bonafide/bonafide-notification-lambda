package com.bonafide.notificationlambda.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.bonafide.notificationlambda.model.RequestType;
import com.bonafide.notificationlambda.service.ConnectionService;
import com.bonafide.notificationlambda.service.NotificationService;
import com.bonafide.notificationlambda.service.WebSocketService;
import com.bonafide.notificationlambda.service.HttpApiService;
import com.bonafide.notificationlambda.model.NotificationRequest;
import com.bonafide.notificationlambda.model.NotificationResult;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.*;
import java.net.URI;
import java.time.Instant;

@Slf4j
public class NotificationLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONNECTIONS_TABLE = System.getenv("CONNECTIONS_TABLE");
    private static final String WEBSOCKET_API_ENDPOINT = System.getenv("WEBSOCKET_API_ENDPOINT");
    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private final DynamoDbClient dynamoDbClient;
    private final ConnectionService connectionService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final HttpApiService httpApiService;

    public NotificationLambdaHandler() {
        this.dynamoDbClient = DynamoDbClient.builder()
            .region(software.amazon.awssdk.regions.Region.of(AWS_REGION))
            .build();
        this.connectionService = new ConnectionService(dynamoDbClient, CONNECTIONS_TABLE);
        this.webSocketService = new WebSocketService(WEBSOCKET_API_ENDPOINT, AWS_REGION);
        this.notificationService = new NotificationService(connectionService, webSocketService);
        this.httpApiService = new HttpApiService(notificationService, connectionService);
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            log.info("Received event: {}", objectMapper.writeValueAsString(event));
            RequestType requestType = determineRequestType(event);
            switch (requestType) {
                case WEBSOCKET_CONNECT:
                    return handleWebSocketConnect(event);
                case WEBSOCKET_DISCONNECT:
                    return handleWebSocketDisconnect(event);
                case WEBSOCKET_DEFAULT:
                    return handleWebSocketDefault(event);
                case HTTP_API:
                    return handleHttpApi(event);
                case DIRECT_INVOKE:
                    return handleDirectInvoke(event);
                default:
                    return createErrorResponse(400, "Unknown request type");
            }
        } catch (Exception e) {
            log.error("Error processing request", e);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private RequestType determineRequestType(Map<String, Object> event) {
        if (event.containsKey("requestContext")) {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            log.info("raw path :: {}, contains http :: {}", requestContext.getOrDefault("rawPath","").toString(), requestContext.containsKey("http"));
            if (event.getOrDefault("rawPath","").toString().contains("api") && requestContext.containsKey("http")) {
                return RequestType.HTTP_API;
            }

            if (requestContext.containsKey("routeKey")) {
                String routeKey = (String) requestContext.get("routeKey");
                switch (routeKey) {
                    case "$connect": return RequestType.WEBSOCKET_CONNECT;
                    case "$disconnect": return RequestType.WEBSOCKET_DISCONNECT;
                    case "$default": return RequestType.WEBSOCKET_DEFAULT;
                }
            }
        }
        if (event.containsKey("requestSource") || event.containsKey("action")) {
            return RequestType.DIRECT_INVOKE;
        }
        return RequestType.UNKNOWN;
    }

    private Map<String, Object> handleWebSocketConnect(Map<String, Object> event) {
        try {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            String connectionId = (String) requestContext.get("connectionId");
            Map<String, Object> queryParams = (Map<String, Object>) event.get("queryStringParameters");
            if (queryParams == null) queryParams = new HashMap<>();
            String userId = (String) queryParams.getOrDefault("userId", "anonymous");
            String roles = (String) queryParams.getOrDefault("roles", "USER");
            String teams = (String) queryParams.getOrDefault("teams", "DEFAULT");
            String department = (String) queryParams.getOrDefault("department", "GENERAL");
            com.bonafide.notificationlambda.model.ConnectionRecord connection = com.bonafide.notificationlambda.model.ConnectionRecord.builder()
                .connectionId(connectionId)
                .userId(userId)
                .roles(Arrays.asList(roles.split(",")))
                .teams(Arrays.asList(teams.split(",")))
                .department(department)
                .connectedAt(Instant.now().getEpochSecond())
                .lastSeen(Instant.now().getEpochSecond())
                .subscribedTopics(Arrays.asList("PROCESS_COMPLETE", "SYSTEM_ALERTS", "ERROR_ALERTS"))
                .build();
            connectionService.storeConnection(connection);
            log.info("WebSocket connected: {} for user {}", connectionId, userId);
            return createSuccessResponse();
        } catch (Exception e) {
            log.error("Error handling WebSocket connect", e);
            return createSuccessResponse();
        }
    }

    private Map<String, Object> handleWebSocketDisconnect(Map<String, Object> event) {
        try {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            String connectionId = (String) requestContext.get("connectionId");
            connectionService.removeConnection(connectionId);
            log.info("WebSocket disconnected: {}", connectionId);
            return createSuccessResponse();
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnect", e);
            return createSuccessResponse();
        }
    }

    private Map<String, Object> handleWebSocketDefault(Map<String, Object> event) {
        try {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            String connectionId = (String) requestContext.get("connectionId");
            String body = (String) event.get("body");
            if (body != null && !body.isEmpty()) {
                JsonNode messageNode = objectMapper.readTree(body);
                String action = messageNode.path("action").asText("unknown");
                switch (action) {
                    case "heartbeat":
                        connectionService.updateHeartbeat(connectionId);
                        break;
                    case "subscribe":
                        JsonNode topicsNode = messageNode.path("topics");
                        List<String> topics = new ArrayList<>();
                        if (topicsNode.isArray()) {
                            topicsNode.forEach(topic -> topics.add(topic.asText()));
                        }
                        connectionService.updateSubscriptions(connectionId, topics);
                        break;
                    default:
                        log.info("Unknown WebSocket action: {}", action);
                }
            }
            return createSuccessResponse();
        } catch (Exception e) {
            log.error("Error handling WebSocket default", e);
            return createSuccessResponse();
        }
    }

    private Map<String, Object> handleHttpApi(Map<String, Object> event) {
        try {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            Map<String, Object> http = (Map<String, Object>) requestContext.get("http");
            String method = (String) http.get("method");
            String path = (String) http.get("path");
            log.info("HTTP API request: {} {}", method, path);

            if ("OPTIONS".equals(method)) {
                return handleCorsOptions(event);
            }

            return httpApiService.handleRequest(method, path, event);
        } catch (Exception e) {
            log.error("Error handling HTTP API request", e);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private Map<String, Object> handleDirectInvoke(Map<String, Object> event) {
        try {
            String action = (String) event.get("action");
            if ("send_notification".equals(action)) {
                Map<String, Object> notification = (Map<String, Object>) event.get("notification");
                NotificationRequest request = objectMapper.convertValue(notification, NotificationRequest.class);
                NotificationResult result = notificationService.sendNotification(request);
                return Map.of(
                    "status", result.getStatus(),
                    "sent", result.getSent(),
                    "failed", result.getFailed(),
                    "total_recipients", result.getTotalRecipients(),
                    "message", result.getMessage()
                );
            }
            return createErrorResponse(400, "Unknown action: " + action);
        } catch (Exception e) {
            log.error("Error handling direct invoke", e);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private Map<String, Object> createSuccessResponse() {
        return Map.of("statusCode", 200);
    }

    private Map<String, Object> createErrorResponse(int statusCode, String message) {
        return Map.of(
            "statusCode", statusCode,
            "body", objectMapper.valueToTree(Map.of("error", message)).toString()
        );
    }

    private Map<String, Object> handleCorsOptions(Map<String, Object> event) {
        return Map.of(
                "statusCode", 200,
                "headers", Map.of(
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                        "Access-Control-Allow-Headers", "Content-Type, Authorization, X-Amz-Date, X-Api-Key, X-Amz-Security-Token, Cookie",
                        "Access-Control-Allow-Credentials", "true",
                        "Access-Control-Max-Age", "86400"
                ),
                "body", ""
        );
    }
} 