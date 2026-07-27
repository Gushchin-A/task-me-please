package dev.gushchin.taskmanager.model;

public record EmailVerificationResendState(int cooldownSeconds, int remainingAttempts) {
    public boolean available() {
        return cooldownSeconds == 0 && remainingAttempts > 0;
    }
}
