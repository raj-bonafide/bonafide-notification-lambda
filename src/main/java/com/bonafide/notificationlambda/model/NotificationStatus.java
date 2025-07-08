package com.bonafide.notificationlambda.model;

/**
 * Status of notification delivery.
 * Used in Lambda/serverless notification responses.
 */
public enum NotificationStatus {
    /** Notification sent to all recipients */
    SENT,
    /** Notification failed for all recipients */
    FAILED,
    /** Notification sent to some, but not all recipients */
    PARTIAL
} 