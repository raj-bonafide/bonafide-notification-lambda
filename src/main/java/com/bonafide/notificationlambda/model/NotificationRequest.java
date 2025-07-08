package com.bonafide.notificationlambda.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String type;
    private String title;
    private String message;
    private String moduleName;
    private String priority;
    private List<String> targetUsers;
    private List<String> requiredRoles;
    private List<String> targetTeams;
    private String processOwnerId;
    private Map<String, Object> data;
    private List<Map<String, Object>> actions;
    private long timestamp;
} 