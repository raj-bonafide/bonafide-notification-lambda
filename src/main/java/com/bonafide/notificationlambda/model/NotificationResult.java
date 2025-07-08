package com.bonafide.notificationlambda.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {
    private String status;
    private int sent;
    private int failed;
    private int totalRecipients;
    private String message;
} 