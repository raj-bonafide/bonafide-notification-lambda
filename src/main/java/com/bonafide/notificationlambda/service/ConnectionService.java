package com.bonafide.notificationlambda.service;

import com.bonafide.notificationlambda.model.ConnectionRecord;
import com.bonafide.notificationlambda.model.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.time.Instant;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Slf4j
public class ConnectionService {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public ConnectionService(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void storeConnection(ConnectionRecord connection) {
        try {
            software.amazon.awssdk.services.dynamodb.model.PutItemRequest request =
                software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                        "connectionId", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connection.getConnectionId()).build(),
                        "userId", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connection.getUserId()).build(),
                        "roles", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(connection.getRoles()).build(),
                        "teams", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(connection.getTeams()).build(),
                        "department", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connection.getDepartment()).build(),
                        "connectedAt", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(String.valueOf(connection.getConnectedAt())).build(),
                        "lastSeen", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(String.valueOf(connection.getLastSeen())).build(),
                        "subscribedTopics", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(connection.getSubscribedTopics()).build()
                    ))
                    .build();
            dynamoDbClient.putItem(request);
            log.info("Stored connection: {}", connection.getConnectionId());
        } catch (Exception e) {
            log.error("Error storing connection", e);
        }
    }

    public void removeConnection(String connectionId) {
        try {
            software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest request =
                software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("connectionId",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connectionId).build()))
                    .build();
            dynamoDbClient.deleteItem(request);
            log.info("Removed connection: {}", connectionId);
        } catch (Exception e) {
            log.error("Error removing connection", e);
        }
    }

    public void updateHeartbeat(String connectionId) {
        try {
            software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest request =
                software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("connectionId",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connectionId).build()))
                    .updateExpression("SET lastSeen = :timestamp")
                    .expressionAttributeValues(Map.of(":timestamp",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build()))
                    .build();
            dynamoDbClient.updateItem(request);
        } catch (Exception e) {
            log.error("Error updating heartbeat", e);
        }
    }

    public void updateSubscriptions(String connectionId, List<String> topics) {
        try {
            software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest request =
                software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("connectionId",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(connectionId).build()))
                    .updateExpression("SET subscribedTopics = :topics")
                    .expressionAttributeValues(Map.of(":topics",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(topics).build()))
                    .build();
            dynamoDbClient.updateItem(request);
        } catch (Exception e) {
            log.error("Error updating subscriptions", e);
        }
    }

    public List<ConnectionRecord> getEligibleConnections(NotificationRequest notification) {
        try {
            software.amazon.awssdk.services.dynamodb.model.ScanRequest request =
                software.amazon.awssdk.services.dynamodb.model.ScanRequest.builder()
                    .tableName(tableName)
                    .build();
            software.amazon.awssdk.services.dynamodb.model.ScanResponse response = dynamoDbClient.scan(request);
            List<ConnectionRecord> eligible = new ArrayList<>();
            for (Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item : response.items()) {
                ConnectionRecord connection = mapToConnectionRecord(item);
                if (isEligibleForNotification(connection, notification)) {
                    eligible.add(connection);
                }
            }
            return eligible;
        } catch (Exception e) {
            log.error("Error getting eligible connections", e);
            return new ArrayList<>();
        }
    }

    public List<ConnectionRecord> getAllConnections() {
        try {
            software.amazon.awssdk.services.dynamodb.model.ScanRequest request =
                software.amazon.awssdk.services.dynamodb.model.ScanRequest.builder()
                    .tableName(tableName)
                    .build();
            software.amazon.awssdk.services.dynamodb.model.ScanResponse response = dynamoDbClient.scan(request);
            return response.items().stream()
                .map((Map<String, AttributeValue> item) -> mapToConnectionRecord(item))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting all connections", e);
            return new ArrayList<>();
        }
    }

    private ConnectionRecord mapToConnectionRecord(Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item) {
        return ConnectionRecord.builder()
            .connectionId(item.get("connectionId").s())
            .userId(item.get("userId").s())
            .roles(item.get("roles").ss())
            .teams(item.get("teams").ss())
            .department(item.get("department").s())
            .connectedAt(Long.parseLong(item.get("connectedAt").n()))
            .lastSeen(Long.parseLong(item.get("lastSeen").n()))
            .subscribedTopics(item.get("subscribedTopics").ss())
            .build();
    }

    private boolean isEligibleForNotification(ConnectionRecord connection, NotificationRequest notification) {
        if (notification.getTargetUsers() != null && notification.getTargetUsers().contains(connection.getUserId())) {
            return true;
        }
        if (notification.getProcessOwnerId() != null && notification.getProcessOwnerId().equals(connection.getUserId())) {
            return true;
        }
        if (notification.getRequiredRoles() != null) {
            for (String role : notification.getRequiredRoles()) {
                if (connection.getRoles().contains(role)) {
                    return true;
                }
            }
        }
        if (notification.getTargetTeams() != null) {
            for (String team : notification.getTargetTeams()) {
                if (connection.getTeams().contains(team)) {
                    return true;
                }
            }
        }
        if (connection.getSubscribedTopics().contains(notification.getType()) ||
            connection.getSubscribedTopics().contains("ALL")) {
            return true;
        }
        return false;
    }
} 