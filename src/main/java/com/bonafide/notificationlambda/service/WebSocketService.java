package com.bonafide.notificationlambda.service;

import com.bonafide.notificationlambda.model.ConnectionRecord;
import com.bonafide.notificationlambda.model.NotificationRequest;
import com.bonafide.notificationlambda.model.NotificationResult;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class WebSocketService {
    private final String apiEndpoint;
    private final String region;

    public WebSocketService(String apiEndpoint, String region) {
        this.apiEndpoint = apiEndpoint;
        this.region = region;
    }

    public NotificationResult sendToConnections(List<ConnectionRecord> connections, NotificationRequest notification) {
        if (connections.isEmpty()) {
            return NotificationResult.builder()
                .status("NO_RECIPIENTS")
                .sent(0)
                .failed(0)
                .totalRecipients(0)
                .message("No eligible recipients found")
                .build();
        }
        try {
            ApiGatewayManagementApiClient client = ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(apiEndpoint))
                .region(software.amazon.awssdk.regions.Region.of(region))
                .build();
            int successful = 0;
            int failed = 0;
            String payload = createNotificationPayload(notification);
            for (ConnectionRecord connection : connections) {
                try {
                    software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest request =
                        software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest.builder()
                            .connectionId(connection.getConnectionId())
                            .data(software.amazon.awssdk.core.SdkBytes.fromUtf8String(payload))
                            .build();
                    client.postToConnection(request);
                    successful++;
                } catch (software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException e) {
                    log.warn("Stale connection removed: {}", connection.getConnectionId());
                    failed++;
                } catch (Exception e) {
                    log.error("Error sending to connection: {}", connection.getConnectionId(), e);
                    failed++;
                }
            }
            return NotificationResult.builder()
                .status(successful > 0 ? "SENT" : "FAILED")
                .sent(successful)
                .failed(failed)
                .totalRecipients(connections.size())
                .message(String.format("Sent to %d/%d connections", successful, connections.size()))
                .build();
        } catch (Exception e) {
            log.error("Error sending notifications", e);
            return NotificationResult.builder()
                .status("FAILED")
                .sent(0)
                .failed(connections.size())
                .totalRecipients(connections.size())
                .message("Failed to send notifications: " + e.getMessage())
                .build();
        }
    }

    private String createNotificationPayload(NotificationRequest notification) {
        try {
            Map<String, Object> payload = Map.of(
                "type", "NOTIFICATION",
                "payload", notification,
                "metadata", Map.of(
                    "sentAt", Instant.now().toString(),
                    "notificationId", UUID.randomUUID().toString()
                )
            );
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create notification payload", e);
        }
    }
} 