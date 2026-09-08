package dev.gushchin.taskmanager.model;

public enum PasswordResetRequestResult {
    SENT,
    DELIVERY_FAILED,
    INVALID_ACCOUNT,
    LIMIT_REACHED
}
