package com.bonafide.notificationlambda.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRecord {
    private String connectionId;
    private String userId;
    private List<String> roles;
    private List<String> teams;
    private String department;
    private long connectedAt;
    private long lastSeen;
    private List<String> subscribedTopics;
} 