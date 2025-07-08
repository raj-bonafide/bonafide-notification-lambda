package com.bonafide.notificationlambda.model;

/**
 * Priority levels for notifications.
 * Used to indicate urgency in Lambda/serverless notifications.
 */
public enum NotificationPriority {
    /** Low priority */
    LOW,
    /** Medium priority */
    MEDIUM,
    /** High priority */
    HIGH,
    /** Critical priority */
    CRITICAL
} 