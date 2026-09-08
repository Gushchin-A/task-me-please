package dev.gushchin.taskmanager.model;

public enum EmailVerificationResendResult {
    SENT,
    DELIVERY_FAILED,
    COOLDOWN,
    LIMIT_REACHED,
    NOT_APPLICABLE
}
