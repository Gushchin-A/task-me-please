package dev.gushchin.taskmanager.exception;

public class InvalidTeamNameException extends RuntimeException {
    private final Reason reason;

    public InvalidTeamNameException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        BLANK,
        TOO_LONG,
        UNCHANGED
    }
}
