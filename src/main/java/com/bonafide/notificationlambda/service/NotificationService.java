package com.bonafide.notificationlambda.service;

import com.bonafide.notificationlambda.model.ConnectionRecord;
import com.bonafide.notificationlambda.model.NotificationRequest;
import com.bonafide.notificationlambda.model.NotificationResult;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class NotificationService {
    private final ConnectionService connectionService;
    private final WebSocketService webSocketService;

    public NotificationService(ConnectionService connectionService, WebSocketService webSocketService) {
        this.connectionService = connectionService;
        this.webSocketService = webSocketService;
    }

    public NotificationResult sendNotification(NotificationRequest request) {
        try {
            List<ConnectionRecord> eligibleConnections = connectionService.getEligibleConnections(request);
            if (eligibleConnections.isEmpty()) {
                log.warn("No eligible connections found for notification: {}", request.getType());
                return NotificationResult.builder()
                    .status("NO_RECIPIENTS")
                    .sent(0)
                    .failed(0)
                    .totalRecipients(0)
                    .message("No eligible recipients found")
                    .build();
            }
            NotificationResult result = webSocketService.sendToConnections(eligibleConnections, request);
            log.info("Notification sent - Type: {}, Module: {}, Recipients: {}, Successful: {}", 
                request.getType(), request.getModuleName(), result.getTotalRecipients(), result.getSent());
            return result;
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return NotificationResult.builder()
                .status("FAILED")
                .sent(0)
                .failed(0)
                .totalRecipients(0)
                .message("Failed to send notification: " + e.getMessage())
                .build();
        }
    }
} 