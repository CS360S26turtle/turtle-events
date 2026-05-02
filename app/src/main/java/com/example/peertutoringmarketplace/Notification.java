/*
 * File: Notification.java
 * Purpose: Stores system messages intended for specific users.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Needs a 'userId' field to specify the recipient.
 */
package com.example.peertutoringmarketplace;

/**
 * Represents a system-generated notification message.
 */
public class Notification {
    private String notificationId;
    private String message;

    /**
     * @return The unique ID of the notification.
     */
    public String getNotificationId() { return notificationId; }

    /**
     * @param notificationId The unique ID to set.
     */
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    /**
     * @return The content of the notification message.
     */
    public String getMessage() { return message; }

    /**
     * @param message The message text to set.
     */
    public void setMessage(String message) { this.message = message; }
}